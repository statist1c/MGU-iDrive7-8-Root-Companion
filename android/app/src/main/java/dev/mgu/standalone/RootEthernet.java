// SPDX-License-Identifier: GPL-3.0-only
package dev.mgu.standalone;

import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Optional explicit root action, affecting only a wired link-local source route. */
public final class RootEthernet {
    private static final String IP="/system/bin/ip", TABLE="20065", PRIORITY="9050";
    private static void validate(String iface,String source) {
        if(!iface.matches("eth[0-9]+|usb[0-9]+|enx[0-9a-fA-F]+") || !EthernetRoute.linkLocal(source) || source.equals("169.254.254.36"))
            throw new IllegalArgumentException("Choose a wired ethN/usbN/enx... interface and a usable unused 169.254.x.x address");
    }
    public static String preview(String iface,String source,boolean remove) {
        validate(iface,source);
        return remove?"Remove the recorded source-specific rule, dedicated route and address added by this app. The interface remains up.":
            IP+" link set dev "+iface+" up\n"+IP+" address add "+source+"/16 dev "+iface+" (if absent)\n"+
            IP+" route add 169.254.0.0/16 dev "+iface+" src "+source+" table "+TABLE+"\n"+
            IP+" rule add priority "+PRIORITY+" from "+source+"/32 table "+TABLE+"\n\nNo default/Wi-Fi/mobile route or SELinux setting is changed.";
    }
    private static String run(String command,boolean optional) throws Exception {
        Process p=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        Thread drain=new Thread(()->{
            try(InputStream in=p.getInputStream()) { byte[] b=new byte[1024]; int n; while((n=in.read(b))!=-1) synchronized(out) { if(out.size()<65536) out.write(b,0,Math.min(n,65536-out.size())); } }
            catch(IOException ignored) { }
        }); drain.start();
        if(!p.waitFor(30,TimeUnit.SECONDS)) { p.destroy(); throw new IOException("Root request timed out; inspect networking before retrying"); }
        drain.join(1000); String text;
        synchronized(out) { text=new String(out.toByteArray(),StandardCharsets.UTF_8); }
        if(p.exitValue()!=0 && !optional) throw new IOException(text.isEmpty()?"Root command denied/failed":text);
        return text;
    }
    public static String apply(Context context,String iface,String source,boolean remove) throws Exception {
        validate(iface,source);
        File file=new File(context.getFilesDir(),"root-network.json");
        if(remove) {
            JSONObject saved=new JSONObject(read(file));
            if(!saved.getString("interface").equals(iface) || !saved.getString("source").equals(source)) throw new IOException("Use the interface/address recorded by this app");
            run(IP+" rule del priority "+PRIORITY+" from "+source+"/32 table "+TABLE,false);
            run(IP+" route del 169.254.0.0/16 dev "+iface+" table "+TABLE,false);
            if(saved.getBoolean("added")) run(IP+" address del "+source+"/16 dev "+iface,false);
            if(!file.delete()) throw new IOException("Configuration removed but its record could not be deleted");
            return "Recorded configuration removed. Interface remains up.";
        }
        if(file.exists()) throw new IOException("A network configuration is already recorded. Remove/review it first; a reboot may already have removed temporary rules.");
        run(IP+" link show dev "+iface,false);
        if(run(IP+" rule show",false).matches("(?s).*(?m)^"+PRIORITY+":.*")) throw new IOException("Rule priority already occupied; refusing to overwrite");
        String routes=run(IP+" route show table "+TABLE,true).trim();
        // Some ip versions report an absent table to stderr; that is an empty table.
        if(!routes.isEmpty() && !routes.contains("FIB table does not exist")) throw new IOException("Dedicated route table is occupied or unreadable; refusing to overwrite");
        boolean exists=run(IP+" -o -4 address show dev "+iface,false).contains(" "+source+"/16 ");
        // Persist intent before changes, allowing manual recovery after an interrupted su.
        try(OutputStream out=new FileOutputStream(file)) { out.write(new JSONObject().put("interface",iface).put("source",source).put("added",!exists).toString().getBytes(StandardCharsets.UTF_8)); }
        boolean addressAdded=false, routeAdded=false;
        try {
            run(IP+" link set dev "+iface+" up",false);
            if(!exists) { run(IP+" address add "+source+"/16 dev "+iface,false); addressAdded=true; }
            run(IP+" route add 169.254.0.0/16 dev "+iface+" src "+source+" table "+TABLE,false); routeAdded=true;
            run(IP+" rule add priority "+PRIORITY+" from "+source+"/32 table "+TABLE,false);
        } catch(Exception e) {
            if(routeAdded) run(IP+" route del 169.254.0.0/16 dev "+iface+" table "+TABLE,true);
            if(addressAdded) run(IP+" address del "+source+"/16 dev "+iface,true);
            // Keep the record: su timeout can leave the actual outcome uncertain.
            throw e;
        }
        return "Ethernet source route configured. Run the read-only compatibility check next.";
    }
    private static String read(File f) throws IOException {
        try(InputStream in=new FileInputStream(f); ByteArrayOutputStream out=new ByteArrayOutputStream()) { byte[] b=new byte[1024]; int n; while((n=in.read(b))!=-1) out.write(b,0,n); return out.toString("UTF-8"); }
    }
}
