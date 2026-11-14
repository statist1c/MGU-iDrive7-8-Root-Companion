package dev.mgu.standalone;

import android.content.Context;
import android.net.*;
import java.io.IOException;
import java.net.*;
import java.util.*;

/** Verifies assigned wired source; never assumes mobile/Wi-Fi can carry ENET. */
public final class EthernetRoute {
    public final String source, iface;
    public final Network network;
    private final ConnectivityManager manager;
    private EthernetRoute(ConnectivityManager cm,String ip,String name,Network net) { manager=cm; source=ip; iface=name; network=net; }
    public static boolean linkLocal(String ip) {
        if(ip==null || !ip.matches("169\\.254\\.[0-9]{1,3}\\.[0-9]{1,3}")) return false;
        String[] p=ip.split("\\."); return Integer.parseInt(p[2])>0 && Integer.parseInt(p[2])<255 && Integer.parseInt(p[3])<=255;
    }
    public static java.util.List<String> interfaceCandidates(Context context) {
        LinkedHashSet<String> names=new LinkedHashSet<>();
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        for(Network net:cm.getAllNetworks()) {
            NetworkCapabilities caps=cm.getNetworkCapabilities(net); LinkProperties links=cm.getLinkProperties(net);
            if(caps!=null && links!=null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) && links.getInterfaceName()!=null) names.add(links.getInterfaceName());
        }
        try {
            Enumeration<NetworkInterface> all=NetworkInterface.getNetworkInterfaces();
            while(all!=null && all.hasMoreElements()) { String name=all.nextElement().getName(); if(name.matches("eth[0-9]+|usb[0-9]+|enx[0-9a-fA-F]+")) names.add(name); }
        } catch(Exception ignored) { }
        return new ArrayList<>(names);
    }
    public static EthernetRoute find(Context context,String preferred) throws Exception {
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ArrayList<EthernetRoute> choices=new ArrayList<>();
        for(Network net:cm.getAllNetworks()) {
            NetworkCapabilities caps=cm.getNetworkCapabilities(net); LinkProperties links=cm.getLinkProperties(net);
            if(caps==null || links==null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) continue;
            for(LinkAddress address:links.getLinkAddresses()) {
                String ip=address.getAddress().getHostAddress();
                if(address.getAddress() instanceof Inet4Address && linkLocal(ip) && (preferred.isEmpty() || preferred.equals(ip)))
                    choices.add(new EthernetRoute(cm,ip,links.getInterfaceName(),net));
            }
        }
        // A rooted static setup may exist before Android registers an Ethernet Network.
        if(choices.isEmpty() && !preferred.isEmpty()) {
            Enumeration<NetworkInterface> all=NetworkInterface.getNetworkInterfaces();
            while(all!=null && all.hasMoreElements()) {
                NetworkInterface ni=all.nextElement();
                if(!ni.isUp() || !ni.getName().matches("eth[0-9]+|usb[0-9]+|enx[0-9a-fA-F]+")) continue;
                for(Enumeration<InetAddress> addresses=ni.getInetAddresses();addresses.hasMoreElements();) {
                    InetAddress address=addresses.nextElement();
                    if(address instanceof Inet4Address && linkLocal(preferred) && preferred.equals(address.getHostAddress()))
                        choices.add(new EthernetRoute(cm,preferred,ni.getName(),null));
                }
            }
        }
        if(choices.size()!=1) throw new IOException(choices.isEmpty()?"No assigned wired 169.254.x.x address found. Connect USB Ethernet and configure its IP in Settings, or use the rooted setup.":"Multiple Ethernet addresses found. Enter the intended source IP.");
        return choices.get(0);
    }
    public void bindProcess() throws IOException {
        if(network!=null && !manager.bindProcessToNetwork(network)) throw new IOException("Android refused the Ethernet network binding");
    }
    public void release() { manager.bindProcessToNetwork(null); }
    public Socket socket(String host,int port,int timeout) throws IOException {
        if(!linkLocal(host)) throw new IOException("Only link-local vehicle endpoints are permitted");
        Socket socket=new Socket();
        try {
            if(network!=null) network.bindSocket(socket);
            socket.bind(new InetSocketAddress(source,0));
            socket.connect(new InetSocketAddress(host,port),timeout);
            return socket;
        } catch(IOException e) { socket.close(); throw e; }
    }
}
