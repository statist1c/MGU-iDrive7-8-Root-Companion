package dev.mgu.standalone;

import android.content.*;
import android.os.SystemClock;
import android.util.Base64;
import com.jcraft.jsch.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;

/** Embedded SSH adapter. Actual MGU negotiation is unverified. */
public final class SshAdapter {
    private final Context context;
    private final EthernetRoute route;
    private final BackendService callback;
    private final boolean legacy;
    private String vin="";
    private boolean mayRotate;
    public SshAdapter(Context c,EthernetRoute r,BackendService log,boolean allowLegacy) {
        context=c; route=r; callback=log; legacy=allowLegacy;
        // Android's built-in provider named BC is a reduced fork. Use the bundled provider.
        Security.removeProvider("BC"); Security.addProvider(new BouncyCastleProvider());
    }
    public void setVehicle(String vehicle,boolean afterFlash) {
        if(!vehicle.matches("[A-HJ-NPR-Z0-9]{17}")) throw new IllegalArgumentException("Invalid SSH VIN context");
        vin=vehicle; mayRotate=afterFlash;
    }
    private final class Pins implements HostKeyRepository {
        public int check(String host,byte[] key) {
            try {
                String digest=Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(key),Base64.NO_WRAP|Base64.NO_PADDING);
                SharedPreferences prefs=context.getSharedPreferences("host-pins",Context.MODE_PRIVATE);
                String name=vin+"@"+host, previous=prefs.getString(name,null);
                if(previous!=null && !previous.equals(digest) && !mayRotate) {
                    callback.onLine("SSH host key changed: refusing connection. Expected SHA256:"+previous+"; received SHA256:"+digest);
                    return CHANGED;
                }
                if(previous==null || !previous.equals(digest)) {
                    if(!prefs.edit().putString(name,digest).commit()) return CHANGED;
                    callback.onLine((previous==null?"First-use SSH host key pinned: ":"Post-flash SSH host key replaced: ")+"SHA256:"+digest);
                }
                mayRotate=false;
                return OK;
            } catch(Exception e) { return CHANGED; }
        }
        public void add(HostKey key,UserInfo ui) { }
        public void remove(String host,String type) { }
        public void remove(String host,String type,byte[] key) { }
        public String getKnownHostsRepositoryID() { return "Private per-VIN pins"; }
        public HostKey[] getHostKey() { return new HostKey[0]; }
        public HostKey[] getHostKey(String host,String type) { return new HostKey[0]; }
    }
    private static final class BoundedOutput extends OutputStream {
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        boolean truncated;
        @Override public synchronized void write(int value) { if(bytes.size()<1024*1024) bytes.write(value); else truncated=true; }
        @Override public synchronized void write(byte[] b,int offset,int length) {
            int allowed=Math.min(length,1024*1024-bytes.size()); if(allowed>0) bytes.write(b,offset,allowed); if(allowed<length) truncated=true;
        }
        synchronized String text() { return new String(bytes.toByteArray(),StandardCharsets.UTF_8); }
    }
    public String run(String host,String keyPath,String command,String user,int port,int timeout,String inputBase64) {
        Session session=null; ChannelExec channel=null;
        BoundedOutput out=new BoundedOutput(), err=new BoundedOutput();
        try {
            if(vin.isEmpty() || !user.equals("root") || port!=22 || timeout<1 || timeout>180) throw new IOException("Invalid SSH operation context");
            File key=new File(keyPath).getCanonicalFile();
            if(!key.getPath().startsWith(context.getFilesDir().getCanonicalPath()+File.separator)) throw new IOException("SSH identity must stay inside private app storage");
            byte[] input=Base64.decode(inputBase64,Base64.DEFAULT);
            if(input.length>1024*1024) throw new IOException("SSH input exceeds limit");
            JSch jsch=new JSch(); jsch.setHostKeyRepository(new Pins()); jsch.addIdentity(key.getPath());
            session=jsch.getSession(user,host,port);
            session.setConfig("StrictHostKeyChecking","yes"); session.setConfig("PreferredAuthentications","publickey");
            if(legacy) {
                callback.onLine("Legacy RSA/SHA1 enabled for this explicitly selected operation");
                session.setConfig("server_host_key",JSch.getConfig("server_host_key")+",ssh-rsa");
                session.setConfig("PubkeyAcceptedAlgorithms",JSch.getConfig("PubkeyAcceptedAlgorithms")+",ssh-rsa");
            }
            session.setSocketFactory(new com.jcraft.jsch.SocketFactory() {
                public Socket createSocket(String h,int p) throws IOException { return route.socket(h,p,10000); }
                public InputStream getInputStream(Socket s) throws IOException { return s.getInputStream(); }
                public OutputStream getOutputStream(Socket s) throws IOException { return s.getOutputStream(); }
            });
            long deadline=SystemClock.elapsedRealtime()+timeout*1000L;
            session.setServerAliveInterval(10000); session.setServerAliveCountMax(3); session.connect(Math.min(timeout*1000,10000));
            channel=(ChannelExec)session.openChannel("exec"); channel.setCommand(command);
            channel.setInputStream(new ByteArrayInputStream(input)); channel.setOutputStream(out); channel.setErrStream(err);
            channel.connect(Math.min(timeout*1000,10000));
            while(!channel.isClosed()) {
                if(SystemClock.elapsedRealtime()>deadline) throw new IOException("Embedded SSH command timed out; do not retry a flash automatically");
                Thread.sleep(30);
            }
            if(out.truncated || err.truncated) throw new IOException("SSH output exceeded the diagnostic limit");
            return new JSONObject().put("rc",channel.getExitStatus()).put("out",out.text()).put("err",err.text()).toString();
        } catch(Exception e) {
            try { return new JSONObject().put("rc",255).put("out",out.text()).put("err",e.toString()).toString(); }
            catch(Exception impossible) { return "{\"rc\":255,\"out\":\"\",\"err\":\"SSH adapter failure\"}"; }
        } finally { if(channel!=null) channel.disconnect(); if(session!=null) session.disconnect(); }
    }
}
