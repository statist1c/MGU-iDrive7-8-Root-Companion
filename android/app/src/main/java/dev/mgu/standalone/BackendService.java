// SPDX-License-Identifier: GPL-3.0-only
package dev.mgu.standalone;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import com.chaquo.python.*;
import com.chaquo.python.android.AndroidPlatform;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.zip.*;

/** One in-process Python worker protected by a connected-device foreground service. */
public final class BackendService extends Service {
    public static volatile boolean running;
    private static String phase="Ready", action="", text="", result="";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wake;
    private BufferedWriter logFile;
    public static synchronized JSONObject snapshot(Context c) {
        try {
            if(!running && phase.equals("Ready")) {
                File last=new File(c.getFilesDir(),"last-job.json");
                if(last.exists()) {
                    JSONObject saved=new JSONObject(read(last));
                    if(saved.optString("state").equals("running")) saved.put("state","interrupted — outcome unknown; inspect before any retry");
                    return saved;
                }
            }
            return new JSONObject().put("state",phase).put("action",action).put("log",text).put("result",result);
        } catch(Exception e) { return new JSONObject(); }
    }
    private static String read(File f) throws IOException {
        try(InputStream in=new FileInputStream(f); ByteArrayOutputStream out=new ByteArrayOutputStream()) { byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1) out.write(b,0,n); return out.toString("UTF-8"); }
    }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onCreate() {
        super.onCreate();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("mgu","Vehicle connection",NotificationManager.IMPORTANCE_LOW));
    }
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,"mgu").setSmallIcon(android.R.drawable.stat_sys_upload).setContentTitle("MGU operation running")
            .setContentText("Keep phone power and ENET connected. Do not force-stop.").setOngoing(true).setContentIntent(open).build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(Build.VERSION.SDK_INT>=34) startForeground(7,notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        else startForeground(7,notification());
        synchronized(BackendService.class) {
            if(running) return START_NOT_STICKY;
            if(intent==null || !intent.hasExtra("request")) { stopSelf(); return START_NOT_STICKY; }
            running=true; phase="running"; text=""; result="";
        }
        String request=intent.getStringExtra("request");
        worker.execute(()->execute(request,id));
        return START_NOT_STICKY; // Never automatically resume a flash after process death.
    }
    public synchronized void onLine(String line) {
        synchronized(BackendService.class) { text+=line+"\n"; if(text.length()>160000) text=text.substring(text.length()-160000); }
        try { if(logFile!=null) { logFile.write(line); logFile.newLine(); logFile.flush(); } } catch(IOException ignored) { }
    }
    private boolean save() {
        try {
            JSONObject data;
            synchronized(BackendService.class) { data=new JSONObject().put("state",phase).put("action",action).put("log",text).put("result",result); }
            android.util.AtomicFile file=new android.util.AtomicFile(new File(getFilesDir(),"last-job.json"));
            FileOutputStream out=file.startWrite();
            try { out.write(data.toString().getBytes(StandardCharsets.UTF_8)); file.finishWrite(out); }
            catch(Exception e) { file.failWrite(out); throw e; }
            return true;
        } catch(Exception e) { onLine("Could not persist job state: "+e.getMessage()); return false; }
    }
    static File extractBackend(Context context) throws IOException {
        File base=new File(context.getFilesDir(),"backend"); if(!base.exists() && !base.mkdirs()) throw new IOException("Cannot create private backend storage");
        // Merge bundled source/assets only. The ZIP never contains generated identities/logs.
        try(ZipInputStream in=new ZipInputStream(context.getAssets().open("backend.zip"))) {
            ZipEntry entry; byte[] buffer=new byte[8192];
            while((entry=in.getNextEntry())!=null) {
                File target=new File(base,entry.getName()).getCanonicalFile();
                if(!target.getPath().startsWith(base.getCanonicalPath()+File.separator)) throw new IOException("Invalid backend asset path");
                if(entry.isDirectory()) continue;
                File parent=target.getParentFile(); if(!parent.exists() && !parent.mkdirs()) throw new IOException("Cannot extract backend directory");
                try(OutputStream out=new FileOutputStream(target)) { int n; while((n=in.read(buffer))!=-1) out.write(buffer,0,n); }
            }
        }
        return base;
    }
    private void execute(String json,int startId) {
        EthernetRoute route=null;
        try {
            JSONObject req=new JSONObject(json);
            synchronized(BackendService.class) { action=req.getString("action"); }
            File logs=new File(getFilesDir(),"job-logs"); if(!logs.exists()) logs.mkdirs();
            logFile=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(logs,System.currentTimeMillis()+".log")),StandardCharsets.UTF_8));
            if(!save()) throw new IOException("Cannot persist an in-progress job marker; refusing vehicle traffic");
            wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MGU:operation"); wake.acquire();
            route=EthernetRoute.find(this,req.optString("source",""),req.optString("transport","ethernet")); route.bindProcess();
            req.put("network_id",route.network==null?"wired:"+route.iface:String.valueOf(route.network.getNetworkHandle()));req.put("source",route.source).put("interface",route.iface);
            onLine("Wired interface "+route.iface+" · source "+route.source);
            File backend=extractBackend(this);
            if(!Python.isStarted()) Python.start(new AndroidPlatform(this));
            SshAdapter ssh=new SshAdapter(this,route,this,req.optBoolean("legacy",false));
            String response=Python.getInstance().getModule("standalone_backend").callAttr("run",backend.getPath(),req.toString(),this,ssh,route).toString();
            JSONObject outcome=new JSONObject(response);
            synchronized(BackendService.class) { result=response; phase=outcome.optInt("rc",1)==0?"complete":"failed"; }
        } catch(Throwable e) {
            onLine("FAILED: "+e.toString()); synchronized(BackendService.class) { phase="failed"; }
        } finally {
            if(route!=null) route.release();
            if(wake!=null && wake.isHeld()) wake.release();
            save();
            try { if(logFile!=null) logFile.close(); } catch(IOException ignored) { }
            new Handler(Looper.getMainLooper()).post(()->{
                synchronized(BackendService.class) { running=false; }
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelfResult(startId);
            });
        }
    }
    @Override public void onDestroy() { worker.shutdown(); super.onDestroy(); }
}
