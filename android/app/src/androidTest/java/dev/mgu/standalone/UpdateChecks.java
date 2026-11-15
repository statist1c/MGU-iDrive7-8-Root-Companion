package dev.mgu.standalone;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.*;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

/** Emulator checks only: GitHub metadata GET, scheduling and negative install guards. */
final class UpdateChecks {
 static void run(Instrumentation t) throws Exception {
  Context c=t.getTargetContext();
  android.content.SharedPreferences prefs=AppUpdater.prefs(c);
  Map<String,?> saved=new HashMap<>(prefs.getAll());
  boolean wasRunning=BackendService.running;
  JobScheduler jobs=c.getSystemService(JobScheduler.class);
  try {
   prefs.edit().clear().putBoolean("automatic",true).commit();
   AppUpdater.schedule(c);
   JobInfo job=jobs.getPendingJob(AppUpdater.CHECK_JOB);
   if(job==null||!job.isPersisted()||job.getIntervalMillis()!=86400000L)throw new AssertionError("Daily update job not registered");
   prefs.edit().putBoolean("automatic",false).commit();AppUpdater.schedule(c);
   if(jobs.getPendingJob(AppUpdater.CHECK_JOB)!=null)throw new AssertionError("Disabled update job still registered");
   BackendService.running=true;
   if(AppUpdater.check(c,false)!=null)throw new AssertionError("Vehicle work did not pause checks");
   try {AppUpdater.download(c,new JSONObject());throw new AssertionError("Download allowed during vehicle work");}catch(IOException expected){}
   BackendService.running=wasRunning;
   if(AppUpdater.validateDownload(c))throw new AssertionError("Untracked update accepted");
   try {c.getContentResolver().openFileDescriptor(Uri.parse("content://"+c.getPackageName()+".updates/update.apk"),"r");throw new AssertionError("Unverified APK provider access allowed");}catch(FileNotFoundException expected){}
   long before=prefs.getLong("download_id",-1);
   JSONObject release=AppUpdater.check(c,false);
   if(prefs.getLong("last_check",0)==0)throw new AssertionError("GitHub check did not complete");
   if(prefs.getLong("download_id",-1)!=before)throw new AssertionError("Metadata check started a download");
   if(release!=null&&!UpdatePolicy.assetUrl(release.getString("url")))throw new AssertionError("Wrong repository selected");
   List<String> permissions=Arrays.asList(c.getPackageManager().getPackageInfo(c.getPackageName(),PackageManager.GET_PERMISSIONS).requestedPermissions);
   if(!permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")||!permissions.contains("android.permission.RECEIVE_BOOT_COMPLETED"))throw new AssertionError("Update permissions missing");
   SettingsActivity settings=(SettingsActivity)t.startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));t.waitForIdleSync();
   for(String text:new String[]{"Check now","Review available update","Install downloaded app update","Include preview releases"}){ArrayList<View> found=new ArrayList<>();settings.getWindow().getDecorView().findViewsWithText(found,text,View.FIND_VIEWS_WITH_TEXT);if(found.isEmpty())throw new AssertionError("Missing updater control: "+text);}
   t.runOnMainSync(settings::finish);t.waitForIdleSync();
  } finally {
   BackendService.running=wasRunning;
   android.content.SharedPreferences.Editor e=prefs.edit().clear();
   for(Map.Entry<String,?> item:saved.entrySet()){Object v=item.getValue();String k=item.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Float)e.putFloat(k,(Float)v);}
   e.commit();AppUpdater.schedule(c);
  }
 }
}
