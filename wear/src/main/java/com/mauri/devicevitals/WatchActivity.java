package com.mauri.devicevitals;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.Gravity;
import android.widget.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;

public final class WatchActivity extends Activity {
 private static final UUID SERVICE = UUID.fromString("62f19ab8-a2af-486a-9bc2-9d1610154c6c");
 private static final int PICK_PHONE = 1;
 private final Handler handler = new Handler(Looper.getMainLooper());
 private BluetoothSocket connection;
 private BluetoothDevice phone;
 private TextView status, readings;
 private Button refresh, choose;

 @Override protected void onCreate(Bundle state) {
  super.onCreate(state);
  ScrollView scroll = new ScrollView(this);
  scroll.setFillViewport(true);
  LinearLayout page = new LinearLayout(this);
  page.setOrientation(LinearLayout.VERTICAL);
  page.setGravity(Gravity.CENTER_HORIZONTAL);
  int side = Math.round(getResources().getDisplayMetrics().widthPixels * 0.16f);
  page.setPadding(side, dp(36), side, dp(38));
  page.setBackgroundColor(0xff0a121e);
  scroll.addView(page);
  setContentView(scroll);
  TextView title = new TextView(this);
  title.setText("Phone vitals");
  title.setTextColor(0xff6de4c4);
  title.setTextSize(20);
  title.setGravity(Gravity.CENTER);
  page.addView(title);
  status = new TextView(this);
  status.setText("On your phone, open Device Vitals and choose Connect Pixel Watch.");
  status.setGravity(Gravity.CENTER);
  page.addView(status);
  readings = new TextView(this);
  readings.setTextSize(15);
  readings.setGravity(Gravity.CENTER);
  page.addView(readings);
  refresh = new Button(this);
  refresh.setText("Refresh");
  refresh.setOnClickListener(v -> {
   if (phone == null) choosePhone();
   else fetch(phone);
  });
  page.addView(refresh);
  choose = new Button(this);
  choose.setText("Choose phone");
  choose.setOnClickListener(v -> choosePhone());
  page.addView(choose);
 }

 private int dp(int value) {
  return Math.round(value * getResources().getDisplayMetrics().density);
 }

 private void choosePhone() {
  if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
      != PackageManager.PERMISSION_GRANTED) {
   requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PICK_PHONE);
   return;
  }
  BluetoothManager manager = getSystemService(BluetoothManager.class);
  BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
  if (adapter == null || !adapter.isEnabled()) {
   status.setText("Turn on Bluetooth to reach your phone.");
   return;
  }
  List<BluetoothDevice> paired = new ArrayList<>(adapter.getBondedDevices());
  if (paired.isEmpty()) {
   status.setText("Pair your watch with your phone first.");
   return;
  }
  String[] names = new String[paired.size()];
  for (int i = 0; i < names.length; i++) {
   BluetoothDevice device = paired.get(i);
   names[i] = device.getName() == null ? device.getAddress() : device.getName();
  }
  new AlertDialog.Builder(this).setTitle("Choose phone")
    .setItems(names, (dialog, index) -> {
     phone = paired.get(index);
     fetch(phone);
    }).setNegativeButton("Cancel", null).show();
 }

 @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
  super.onRequestPermissionsResult(code, permissions, results);
  if (code == PICK_PHONE && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
   choosePhone();
  }
 }

 private void fetch(BluetoothDevice device) {
  final BluetoothSocket socket;
  try {
   socket = device.createRfcommSocketToServiceRecord(SERVICE);
  } catch (IOException | SecurityException e) {
   status.setText("Bluetooth connection unavailable.");
   return;
  }
  connection = socket;
  refresh.setEnabled(false);
  choose.setEnabled(false);
  readings.setText("");
  status.setText("Connecting…");
  handler.postDelayed(() -> finish(socket, null), 12000);
  new Thread(() -> {
   try {
    socket.connect();
    String snapshot = new DataInputStream(socket.getInputStream()).readUTF();
    socket.getOutputStream().write(1);
    socket.getOutputStream().flush();
    handler.post(() -> finish(socket, snapshot));
   } catch (IOException | SecurityException e) {
    handler.post(() -> finish(socket, null));
   }
  }, "Vitals Bluetooth").start();
 }

 private void finish(BluetoothSocket socket, String snapshot) {
  if (connection != socket) return;
  closeConnection();
  refresh.setEnabled(true);
  choose.setEnabled(true);
  if (snapshot == null || snapshot.isEmpty()) {
   status.setText("Phone unavailable. Open Device Vitals and start watch sharing.");
  } else {
   status.setText("Updated " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()));
   readings.setText(snapshot);
  }
 }

 private void closeConnection() {
  BluetoothSocket socket = connection;
  connection = null;
  handler.removeCallbacksAndMessages(null);
  if (socket != null) {
   try { socket.close(); } catch (IOException ignored) {}
  }
 }

 @Override protected void onDestroy() {
  closeConnection();
  super.onDestroy();
 }
}
