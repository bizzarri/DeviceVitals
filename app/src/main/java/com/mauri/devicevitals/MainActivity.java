package com.mauri.devicevitals;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.provider.Settings;
import android.telephony.*;
import android.view.*;
import android.widget.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

/** Local-only dashboard. Polling stops when the activity is paused. */
public final class MainActivity extends Activity {
 private static final int BG=Color.rgb(10,18,30), PANEL=Color.rgb(20,33,49), TEXT=Color.rgb(239,246,255), MUTED=Color.rgb(164,182,202), MINT=Color.rgb(109,228,196), BLUE=Color.rgb(130,182,255), GOLD=Color.rgb(255,205,126);
 private final Handler handler=new Handler(Looper.getMainLooper());
 private Metric ram,storage,battery;
 private TextView batteryDetails,uptime,updated,memoryNote,cellularDetails,simPermission;
 private final Runnable refresh=new Runnable(){public void run(){updateStats();handler.postDelayed(this,2000);}};
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  LinearLayout root=column();root.setBackgroundColor(BG);
  root.setOnApplyWindowInsetsListener((v,insets)->{
   if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}
   else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
   return insets;
  });
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  LinearLayout page=column();page.setPadding(dp(24),dp(24),dp(24),dp(32));scroll.addView(page);root.addView(scroll,new LinearLayout.LayoutParams(-1,-1));setContentView(root);root.requestApplyInsets();
  TextView eyebrow=text("YOUR DEVICE, AT A GLANCE",11,MINT);eyebrow.setLetterSpacing(.16f);page.addView(eyebrow);
  TextView title=text("Device Vitals",34,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);add(page,title,8);
  add(page,text("A little clarity for your everyday device.",15,MUTED),6);
  updated=text("Live · refreshing every 2 seconds",12,MINT);add(page,updated,16);
  GridLayout grid=new GridLayout(this);int columns=getResources().getConfiguration().screenWidthDp>=700?3:1;grid.setColumnCount(columns);add(page,grid,24);
  ram=new Metric("01  /  MEMORY","RAM in use",MINT);storage=new Metric("02  /  STORAGE","Internal storage used",BLUE);battery=new Metric("03  /  BATTERY","Battery remaining",GOLD);
  addLink(ram.box,"View memory use by app  ›",MINT,v->openMemoryReport());
  addLink(storage.box,"View top 5 storage users  ›",BLUE,v->startActivity(new Intent(this,StorageActivity.class)));
  Metric[] metrics={ram,storage,battery};
  for(int i=0;i<metrics.length;i++){GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(i/columns),GridLayout.spec(i%columns,1f));p.width=0;p.height=-2;p.setMargins(0,0,i%columns<columns-1?dp(12):0,dp(12));grid.addView(metrics[i].box,p);}
  LinearLayout details=panel();add(page,details,4);details.addView(text("BATTERY & SYSTEM",11,MUTED));batteryDetails=text("Reading battery…",16,TEXT);add(details,batteryDetails,14);uptime=text("",14,MUTED);add(details,uptime,10);
  LinearLayout cellular = panel();
  add(page, cellular, 16);
  cellular.addView(text("CELLULAR CONNECTIVITY", 11, MINT));
  cellularDetails = text("Reading cellular connection…", 15, TEXT);
  add(cellular, cellularDetails, 12);
  simPermission = text("Allow access to show all active SIMs", 15, MINT);
  simPermission.setPadding(0, dp(14), 0, dp(14));
  simPermission.setMinHeight(dp(48));
  simPermission.setFocusable(true);
  simPermission.setOnClickListener(v -> requestPermissions(
    new String[]{android.Manifest.permission.READ_PHONE_STATE}, 1));
  add(cellular, simPermission, 8);
  addLink(cellular, "Open mobile network settings  ›", MINT, v -> {
   try {
    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
   } catch (ActivityNotFoundException | SecurityException e) {
    Toast.makeText(this, "Open Network & Internet in Android Settings.", Toast.LENGTH_LONG).show();
   }
  });
  LinearLayout device=panel();add(page,device,16);device.addView(text("ABOUT THIS DEVICE",11,MUTED));TextView model=text(Build.MANUFACTURER+" "+Build.MODEL,22,TEXT);model.setTypeface(Typeface.DEFAULT,Typeface.BOLD);add(device,model,12);
  add(device,text("Android "+Build.VERSION.RELEASE+"  ·  API "+Build.VERSION.SDK_INT,15,MUTED),10);
  add(device,text("Security patch: "+(Build.VERSION.SECURITY_PATCH.isEmpty()?"Not reported":Build.VERSION.SECURITY_PATCH),14,MUTED),8);
  String abi=Build.SUPPORTED_ABIS.length>0?Build.SUPPORTED_ABIS[0]:"Unknown";add(device,text(Runtime.getRuntime().availableProcessors()+" processor cores  ·  "+abi,14,MUTED),8);
  android.util.DisplayMetrics dm=getResources().getDisplayMetrics();add(device,text(dm.widthPixels+" × "+dm.heightPixels+" display area  ·  "+dm.densityDpi+" dpi",14,MUTED),8);
  memoryNote=text("",12,MUTED);add(page,memoryNote,20);
  add(page,text("Storage covers the internal data volume, not SD cards or the full advertised device capacity. Battery readings depend on the hardware.",12,MUTED),10);
  add(page,text("Private by design. All readings stay on this device.",12,MINT),16);
  TextView button=text("Refresh now",15,BG);button.setTypeface(Typeface.DEFAULT,Typeface.BOLD);button.setGravity(Gravity.CENTER);button.setPadding(dp(16),dp(16),dp(16),dp(16));button.setBackground(shape(MINT,14));button.setOnClickListener(v->{updateStats();button.announceForAccessibility("Device readings refreshed");});button.setFocusable(true);add(page,button,20);
 }
 @Override protected void onResume(){super.onResume();handler.removeCallbacks(refresh);handler.post(refresh);}
 @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
 private void updateStats(){
  ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();((ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(info);long used=Math.max(0,info.totalMem-info.availMem);
  ram.set(percent(used,info.totalMem),bytes(used)+" / "+bytes(info.totalMem),bytes(info.availMem)+" available");
  memoryNote.setText("RAM in use = system RAM minus available RAM. Android reclaims cached memory when needed. "+(info.lowMemory?"Android currently reports low memory.":"Android is not reporting low memory."));
  try{StatFs fs=new StatFs(Environment.getDataDirectory().getAbsolutePath());long total=fs.getTotalBytes(),available=fs.getAvailableBytes();storage.set(percent(total-available,total),bytes(total-available)+" / "+bytes(total),bytes(available)+" available to apps");}catch(RuntimeException e){storage.unavailable("Storage reading unavailable");}
  Intent power=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
  if(power==null){battery.unavailable("No battery reading");batteryDetails.setText("Battery details unavailable");}else{
   int level=power.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=power.getIntExtra(BatteryManager.EXTRA_SCALE,-1),status=power.getIntExtra(BatteryManager.EXTRA_STATUS,BatteryManager.BATTERY_STATUS_UNKNOWN);
   String state=status==BatteryManager.BATTERY_STATUS_FULL?"Fully charged":status==BatteryManager.BATTERY_STATUS_CHARGING?"Charging":status==BatteryManager.BATTERY_STATUS_DISCHARGING?"On battery":status==BatteryManager.BATTERY_STATUS_NOT_CHARGING?"Not charging":"Status not reported";
   int plug=power.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);String source=plug==BatteryManager.BATTERY_PLUGGED_AC?"AC power":plug==BatteryManager.BATTERY_PLUGGED_USB?"USB power":plug==BatteryManager.BATTERY_PLUGGED_WIRELESS?"Wireless power":plug!=0?"External power":"Unplugged";
   if(level>=0&&scale>0)battery.set(percent(level,scale),state,source);else battery.unavailable("Battery level not reported");
   int temp=power.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE),voltage=power.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1);
   String temperature=temp==Integer.MIN_VALUE?"Temperature not reported":String.format(Locale.getDefault(),"%.1f °C / %.1f °F",temp/10.0,temp/10.0*1.8+32);
   String volts=voltage>0?String.format(Locale.getDefault(),"%.2f V",voltage/1000.0):"Voltage not reported";batteryDetails.setText(temperature+"  ·  "+volts+"\n"+health(power.getIntExtra(BatteryManager.EXTRA_HEALTH,BatteryManager.BATTERY_HEALTH_UNKNOWN)));
  }
  updateCellular();
  long minutes=SystemClock.elapsedRealtime()/60000;uptime.setText("Since restart: "+minutes/1440+"d "+minutes%1440/60+"h "+minutes%60+"m");updated.setText("LIVE  ·  Updated "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date()));
 }
 private void updateCellular() {
  simPermission.setVisibility(View.GONE);
  TelephonyManager phone = getSystemService(TelephonyManager.class);
  if (phone == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
   cellularDetails.setText("This device has no cellular radio.");
   return;
  }
  String internet = "Active connection: " + internetTransport();
  if (Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0) {
   cellularDetails.setText("Airplane mode is on.\n" + internet);
   return;
  }
  try {
   int defaultSim = SubscriptionManager.getDefaultDataSubscriptionId();
   boolean canListSims = checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
     == PackageManager.PERMISSION_GRANTED;
   if (!canListSims) {
    simPermission.setVisibility(View.VISIBLE);
    String summary = defaultSim < 0 ? "No default mobile data SIM."
      : describeSim(phone.createForSubscriptionId(defaultSim), "", "Default data SIM");
    cellularDetails.setText(summary + "\n\n" + internet);
    return;
   }
   SubscriptionManager subscriptions = getSystemService(SubscriptionManager.class);
   List<SubscriptionInfo> active = subscriptions == null ? null
     : subscriptions.getActiveSubscriptionInfoList();
   if (active == null || active.isEmpty()) {
    cellularDetails.setText("No active SIMs.\n" + internet);
    return;
   }
   StringBuilder details = new StringBuilder();
   for (SubscriptionInfo sim : active) {
    int id = sim.getSubscriptionId();
    String type = Build.VERSION.SDK_INT >= 28 && sim.isEmbedded() ? "eSIM" : "SIM";
    if (id == defaultSim) type += " · Default data";
    if (details.length() > 0) details.append("\n\n");
    details.append(describeSim(phone.createForSubscriptionId(id),
      sim.getDisplayName() == null ? "" : sim.getDisplayName().toString(), type));
   }
   cellularDetails.setText(details + "\n\n" + internet);
  } catch (SecurityException | UnsupportedOperationException | IllegalStateException e) {
   cellularDetails.setText("Cellular details unavailable.\n" + internet);
  }
 }

 private String describeSim(TelephonyManager phone, String name, String type) {
  String carrier = phone.getSimOperatorName();
  if (name.isEmpty()) name = carrier.isEmpty() ? "Carrier unavailable" : carrier;
  StringBuilder details = new StringBuilder(name).append(" · ").append(type);
  try {
   if (!carrier.isEmpty() && !name.equalsIgnoreCase(carrier)) {
    details.append("\nCarrier: ").append(carrier);
   }
   String network = phone.getNetworkOperatorName();
   if (!network.isEmpty()) {
    details.append("\nServing network: ").append(network);
    if (phone.isNetworkRoaming()) details.append(" (roaming)");
   }
   details.append("\nMobile data: ").append(phone.isDataEnabled() ? "On" : "Off");
   // getDataState is tied to the active data subscription on some Android versions.
   int activeData = Build.VERSION.SDK_INT >= 30 ? SubscriptionManager.getActiveDataSubscriptionId()
     : SubscriptionManager.getDefaultDataSubscriptionId();
   if (Build.VERSION.SDK_INT >= 30 && phone.getSubscriptionId() == activeData) {
    details.append(" · ").append(dataConnection(phone.getDataState()));
   }
   if (Build.VERSION.SDK_INT >= 33 ||
       checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
    details.append("\nRadio technology: ").append(radioTechnology(phone.getDataNetworkType()));
   }
   details.append("\n").append(signalReading(phone, !network.isEmpty()));
  } catch (SecurityException | UnsupportedOperationException | IllegalStateException e) {
   details.append("\nSome cellular readings are unavailable.");
  }
  return details.toString();
 }

 private String internetTransport() {
  ConnectivityManager connectivity = getSystemService(ConnectivityManager.class);
  NetworkCapabilities network = connectivity == null ? null
    : connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
  if (network == null) return "No active network";
  if (network.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return "VPN";
  if (network.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
  if (network.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Cellular";
  if (network.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
  return "Other network";
 }

 private String signalReading(TelephonyManager phone, boolean registered) {
  SignalStrength signal = Build.VERSION.SDK_INT >= 28 && registered ? phone.getSignalStrength() : null;
  if (signal == null) return "Signal: Unavailable";
  StringBuilder result = new StringBuilder("Signal: ").append(signal.getLevel()).append(" / 4");
  if (Build.VERSION.SDK_INT >= 29) {
   for (CellSignalStrength strength : signal.getCellSignalStrengths()) {
    int dbm = strength.getDbm();
    if (dbm >= 0) continue;
    String radio = strength instanceof CellSignalStrengthNr ? "5G NR"
      : strength instanceof CellSignalStrengthLte ? "LTE" : "Radio";
    result.append("\n").append(radio).append(" signal: ").append(dbm).append(" dBm");
   }
  }
  return result.toString();
 }

 private String dataConnection(int state) {
  switch (state) {
   case TelephonyManager.DATA_CONNECTED: return "Connected";
   case TelephonyManager.DATA_CONNECTING: return "Connecting";
   case TelephonyManager.DATA_DISCONNECTED: return "Disconnected";
   case TelephonyManager.DATA_SUSPENDED: return "Suspended";
   default: return "Status unavailable";
  }
 }

 private String radioTechnology(int type) {
  switch (type) {
   case TelephonyManager.NETWORK_TYPE_NR: return "5G NR";
   case TelephonyManager.NETWORK_TYPE_LTE: return "LTE";
   case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPA+";
   case TelephonyManager.NETWORK_TYPE_HSPA:
   case TelephonyManager.NETWORK_TYPE_HSDPA:
   case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSPA";
   case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS";
   case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE";
   case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS";
   default: return "Unavailable";
  }
 }

 private void addLink(LinearLayout parent,String label,int color,View.OnClickListener action){
  TextView link=text(label,15,color);link.setPadding(0,dp(14),0,dp(14));link.setMinHeight(dp(48));link.setFocusable(true);link.setOnClickListener(action);add(parent,link,8);
 }
 private void openMemoryReport(){
  try {startActivity(new Intent("android.settings.APP_MEMORY_USAGE"));}
  catch(ActivityNotFoundException|SecurityException e){
   new AlertDialog.Builder(this).setTitle("Android memory report").setMessage("This device does not expose a direct link to its memory report. In Developer options, open Memory, then Memory used by apps. Android reports usage over a selected time period, rather than an instant snapshot.").setPositiveButton("Developer options",(d,w)->{
    try{startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));}
    catch(ActivityNotFoundException|SecurityException missing){Toast.makeText(this,"Open Settings → System → Developer options → Memory",Toast.LENGTH_LONG).show();}
   }).setNegativeButton("Cancel",null).show();
  }
 }
 private String health(int v){switch(v){case BatteryManager.BATTERY_HEALTH_GOOD:return "Battery health: Good";case BatteryManager.BATTERY_HEALTH_OVERHEAT:return "Battery health: Overheating";case BatteryManager.BATTERY_HEALTH_DEAD:return "Battery health: Dead";case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:return "Battery health: Over voltage";case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:return "Battery health: Failure reported";case BatteryManager.BATTERY_HEALTH_COLD:return "Battery health: Cold";default:return "Battery health: Not reported";}}
 private int percent(long n,long total){return total<=0?0:(int)Math.max(0,Math.min(100,Math.round(n*100.0/total)));}
 private String bytes(long n){return String.format(Locale.getDefault(),"%.1f GiB",n/1073741824.0);}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("tnum");return t;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
 private LinearLayout panel(){LinearLayout l=column();l.setPadding(dp(22),dp(22),dp(22),dp(22));l.setBackground(shape(PANEL,20));return l;}
 private void add(LinearLayout parent,View child,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(child,p);}
 private final class Metric{
  final LinearLayout box;final TextView value,detail,caption;final ProgressBar progress;
  Metric(String label,String name,int accent){box=panel();box.addView(text(label,11,accent));add(box,text(name,16,TEXT),18);value=text("—",48,TEXT);value.setTypeface(Typeface.DEFAULT,Typeface.BOLD);add(box,value,8);detail=text("Reading…",15,TEXT);add(box,detail,4);progress=new ProgressBar(MainActivity.this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(41,58,77)));add(box,progress,16);caption=text("",13,MUTED);add(box,caption,10);}
  void set(int p,String d,String n){value.setText(p+"%");detail.setText(d);caption.setText(n);progress.setProgress(p);progress.setVisibility(View.VISIBLE);}
  void unavailable(String reason){value.setText("—");detail.setText(reason);caption.setText("");progress.setVisibility(View.INVISIBLE);}
 }
}

