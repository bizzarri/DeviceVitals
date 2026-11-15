package com.mauri.devicevitals;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

final class WatchShare {
 static final int PERMISSION_REQUEST = 42;
 private static final UUID SERVICE = UUID.fromString("62f19ab8-a2af-486a-9bc2-9d1610154c6c");
 private final MainActivity activity;
 private final Supplier<String> readDashboard;
 private volatile BluetoothServerSocket server;
 private final Handler handler = new Handler(Looper.getMainLooper());
 private volatile String snapshot = "";

 WatchShare(MainActivity activity, Supplier<String> readDashboard) {
  this.activity = activity;
  this.readDashboard = readDashboard;
 }

 void refreshSnapshot() {
  snapshot = readDashboard.get();
 }

 void chooseWatch() {
  if (server != null) {
   stop();
   return;
  }
  if (Build.VERSION.SDK_INT >= 31 && activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
      != PackageManager.PERMISSION_GRANTED) {
   activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST);
   return;
  }
  BluetoothManager manager = activity.getSystemService(BluetoothManager.class);
  BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
  if (adapter == null || !adapter.isEnabled()) {
   showMessage("Turn on Bluetooth to connect your watch.");
   return;
  }
  List<BluetoothDevice> devices = new ArrayList<>(adapter.getBondedDevices());
  if (devices.isEmpty()) {
   showMessage("Pair your watch with this phone first.");
   return;
  }
  String[] names = new String[devices.size()];
  for (int i = 0; i < names.length; i++) {
   BluetoothDevice device = devices.get(i);
   names[i] = device.getName() == null ? device.getAddress() : device.getName();
  }
  new AlertDialog.Builder(activity).setTitle("Choose your paired watch")
    .setItems(names, (dialog, index) -> start(adapter, devices.get(index)))
    .setNegativeButton("Cancel", null).show();
 }

 private void start(BluetoothAdapter adapter, BluetoothDevice watch) {
  try {
   BluetoothServerSocket listener = adapter.listenUsingRfcommWithServiceRecord("Device Vitals", SERVICE);
   server = listener;
   activity.updateWatchLink(true);
   new Thread(() -> serve(listener, watch), "Vitals Bluetooth").start();
  } catch (IOException | SecurityException e) {
   showMessage("Could not start watch sharing. Check Bluetooth and try again.");
  }
 }

 private void serve(BluetoothServerSocket listener, BluetoothDevice watch) {
  try {
   while (server == listener) {
    try (BluetoothSocket socket = listener.accept()) {
     if (server != listener) continue;
     if (!watch.equals(socket.getRemoteDevice())) {
      Log.w("VitalsWatch", "Connection came from a different paired device.");
      continue;
     }
     Runnable timeout = () -> close(socket);
     handler.postDelayed(timeout, 8000);
     try {
      DataOutputStream output = new DataOutputStream(socket.getOutputStream());
      output.writeUTF(snapshot);
      output.flush();
      // Keep the RFCOMM channel open until the watch has read the complete frame.
      if (socket.getInputStream().read() == 1) {
       Log.i("VitalsWatch", "Watch acknowledged the snapshot.");
      }
     } catch (IOException e) {
      Log.w("VitalsWatch", "Snapshot transfer interrupted.");
     } finally {
      handler.removeCallbacks(timeout);
     }
    }
   }
  } catch (IOException | SecurityException e) {
   activity.runOnUiThread(() -> {
    if (server == listener) {
     stop();
     showMessage("Watch sharing stopped. Tap Connect Pixel Watch to try again.");
    }
   });
  }
 }

 private static void close(BluetoothSocket socket) {
  try { socket.close(); } catch (IOException ignored) {}
 }

 void stop() {
  BluetoothServerSocket listener = server;
  server = null;
  if (listener != null) {
   try { listener.close(); } catch (IOException ignored) {}
  }
  activity.updateWatchLink(false);
 }

 private void showMessage(String text) {
  Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
 }
}
