package com.mauri.devicevitals;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Queries app storage only after the user explicitly grants Usage access. */
public final class StorageActivity extends Activity {
 private static final int BG=0xff0a121e,PANEL=0xff142131,TEXT=0xffeff6ff,MUTED=0xffa4b6ca,BLUE=0xff82b6ff;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private LinearLayout results;
 private volatile int generation;
 private Future<?> pending;
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  LinearLayout root=column();root.setBackgroundColor(BG);
  root.setOnApplyWindowInsetsListener((v,i)->{
   if(Build.VERSION.SDK_INT>=30){android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);}
   else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());
   return i;
  });
  ScrollView scroll=new ScrollView(this);LinearLayout page=column();page.setPadding(dp(24),dp(16),dp(24),dp(32));scroll.addView(page);root.addView(scroll);setContentView(root);root.requestApplyInsets();
  button(page,"‹  Back to Device Vitals",v->finish());
  TextView title=text("Top 5 storage users",28,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);add(page,title,16);
  add(page,text("Installed apps · this Android profile · internal storage",13,BLUE),8);
  add(page,text("Ranked by app + data size. Cache is included in data, not added twice. Photos, downloads, system files, and other profiles are not ranked here.",14,MUTED),12);
  results=column();add(page,results,20);
 }
 @Override protected void onResume(){super.onResume();load();}
 @Override protected void onPause(){generation++;if(pending!=null)pending.cancel(true);super.onPause();}
 @Override protected void onDestroy(){worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
 private boolean hasAccess(){
  AppOpsManager ops=(AppOpsManager)getSystemService(APP_OPS_SERVICE);
  return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName())==AppOpsManager.MODE_ALLOWED;
 }
 private void load(){
  final int token=++generation;if(pending!=null)pending.cancel(true);results.removeAllViews();
  if(!hasAccess()){
   add(results,text("Allow access to app storage sizes",20,TEXT),0);
   add(results,text("To find your largest apps, Device Vitals needs Android’s Usage access permission and reads the installed-app list. Everything stays on your device. On the next screen, select Device Vitals and enable Allow usage access, then return here. You can revoke this access at any time.",15,MUTED),12);
   button(results,"Open Usage access settings",v->openAccess());return;
  }
  TextView status=text("Measuring installed apps…",16,TEXT);add(results,status,0);
  pending=worker.submit(()->{
   try{
    PackageManager pm=getPackageManager();
    StorageStatsManager manager=(StorageStatsManager)getSystemService(STORAGE_STATS_SERVICE);
    List<ApplicationInfo> installed=pm.getInstalledApplications(0);
    Map<Integer,List<ApplicationInfo>> groups=new HashMap<>();
    for(ApplicationInfo app:installed){
     if(!StorageManager.UUID_DEFAULT.equals(app.storageUuid))continue;
     List<ApplicationInfo> group=groups.get(app.uid);if(group==null){group=new ArrayList<>();groups.put(app.uid,group);}group.add(app);
    }
    List<Entry> entries=new ArrayList<>();int skipped=0,checked=0;
    for(Map.Entry<Integer,List<ApplicationInfo>> group:groups.entrySet()){
     if(Thread.currentThread().isInterrupted()||token!=generation)return;
     try{
      StorageStats stats=manager.queryStatsForUid(StorageManager.UUID_DEFAULT,group.getKey());
      List<ApplicationInfo> apps=group.getValue();Collections.sort(apps,(a,b)->a.packageName.compareTo(b.packageName));
      StringBuilder names=new StringBuilder();StringBuilder packages=new StringBuilder();
      for(ApplicationInfo app:apps){if(names.length()>0){names.append(" + ");packages.append(", ");}names.append(pm.getApplicationLabel(app));packages.append(app.packageName);}
      entries.add(new Entry(names.toString(),packages.toString(),stats.getAppBytes(),stats.getDataBytes(),stats.getCacheBytes(),apps.size()));
     }catch(SecurityException denied){if(!hasAccess()){main.post(()->{if(token==generation&&!isDestroyed())load();});return;}skipped++;}
     catch(Exception unavailable){skipped++;}
     checked++;final int progress=checked,total=groups.size();
     if(checked%10==0)main.post(()->{if(token==generation&&!isDestroyed())status.setText("Measured "+progress+" of "+total+" app groups…");});
    }
    Collections.sort(entries,(a,b)->Long.compare(b.total(),a.total()));
    final int misses=skipped,count=groups.size();
    main.post(()->{if(token==generation&&!isFinishing()&&!isDestroyed())show(entries,misses,count);});
   }catch(Exception error){main.post(()->{if(token==generation&&!isDestroyed()){results.removeAllViews();add(results,text("Couldn’t read app storage sizes. Check Usage access and try again.",16,TEXT),0);button(results,"Try again",v->load());button(results,"Usage access settings",v->openAccess());}});}
  });
 }
 private void show(List<Entry> entries,int skipped,int total){
  results.removeAllViews();
  add(results,text("Measured now · "+entries.size()+" of "+total+" app groups",12,BLUE),0);
  if(skipped>0)add(results,text(skipped+" app groups could not be read. This ranking includes only readable groups, so the complete top five may differ.",14,MUTED),10);
  if(entries.isEmpty())add(results,text("No readable internal-storage app sizes were returned.",16,TEXT),16);
  for(int i=0;i<Math.min(5,entries.size());i++){
   Entry e=entries.get(i);LinearLayout card=column();card.setPadding(dp(18),dp(18),dp(18),dp(18));GradientDrawable bg=new GradientDrawable();bg.setColor(PANEL);bg.setCornerRadius(dp(18));card.setBackground(bg);add(results,card,14);
   TextView label=text((i+1)+". "+e.label,18,TEXT);label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);add(card,label,0);
   TextView size=text(bytes(e.total()),28,BLUE);size.setTypeface(Typeface.DEFAULT,Typeface.BOLD);add(card,size,8);
   add(card,text("App: "+bytes(e.code)+"   Data: "+bytes(e.data),14,MUTED),8);
   add(card,text("Includes "+bytes(e.cache)+" cache",12,MUTED),6);
   if(e.members>1)add(card,text("Shared Android user ID: combined app storage",12,MUTED),6);
   add(card,text(e.packages,11,MUTED),8);
  }
  add(results,text("Sizes are a snapshot. Android may account for shared or system app code differently from Settings.",12,MUTED),18);
  button(results,"Refresh list",v->load());
 }
 private void openAccess(){
  try{startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,Uri.parse("package:"+getPackageName())));}
  catch(ActivityNotFoundException|SecurityException e){try{startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));}catch(ActivityNotFoundException|SecurityException unavailable){Toast.makeText(this,"Open Settings → Apps → Special app access → Usage access",Toast.LENGTH_LONG).show();}}
 }
 private static final class Entry{
  final String label,packages;final long code,data,cache;final int members;
  Entry(String n,String p,long a,long d,long c,int m){label=n;packages=p;code=Math.max(0,a);data=Math.max(0,d);cache=Math.max(0,c);members=m;}
  long total(){return code+data;}
 }
 private String bytes(long n){return n>=1073741824L?String.format(Locale.getDefault(),"%.2f GiB",n/1073741824.0):String.format(Locale.getDefault(),"%.1f MiB",n/1048576.0);}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private TextView text(String s,int size,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(c);return t;}
 private void add(LinearLayout p,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);p.addView(v,lp);}
 private void button(LinearLayout p,String title,View.OnClickListener action){TextView b=text(title,16,BLUE);b.setPadding(0,dp(14),0,dp(14));b.setMinHeight(dp(48));b.setFocusable(true);b.setOnClickListener(action);add(p,b,12);}
}
