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
    private boolean wifi;
    public static boolean local(String ip){if(ip!=null && ip.matches("169\\.254\\.[0-9]{1,3}\\.[0-9]{1,3}")){String[] a=ip.split("\\.");if(Integer.parseInt(a[2])<=255 && Integer.parseInt(a[3])<=255)return true;}if(ip==null||!ip.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))return false;String[] p=ip.split("\\.");int[] n=new int[4];for(int i=0;i<4;i++){n[i]=Integer.parseInt(p[i]);if(n[i]>255)return false;}return n[0]==10 || (n[0]==172&&n[1]>=16&&n[1]<=31) || (n[0]==192&&n[1]==168);}

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
        return find(context,preferred,"ethernet");
    }
    public static EthernetRoute find(Context context,String preferred,String transport) throws Exception {
        boolean wifi=transport.equals("wifi");
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ArrayList<EthernetRoute> choices=new ArrayList<>();
        for(Network net:cm.getAllNetworks()) {
            NetworkCapabilities caps=cm.getNetworkCapabilities(net); LinkProperties links=cm.getLinkProperties(net);
            if(caps==null || links==null || !caps.hasTransport(wifi?NetworkCapabilities.TRANSPORT_WIFI:NetworkCapabilities.TRANSPORT_ETHERNET) || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue;
            for(LinkAddress address:links.getLinkAddresses()) {
                String ip=address.getAddress().getHostAddress();
                if(address.getAddress() instanceof Inet4Address && (wifi?local(ip):linkLocal(ip)) && (preferred.isEmpty() || preferred.equals(ip)))
                    choices.add(new EthernetRoute(cm,ip,links.getInterfaceName(),net));
            }
        }
        // A rooted static setup may exist before Android registers an Ethernet Network.
        if(!wifi && choices.isEmpty() && !preferred.isEmpty()) {
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
        if(choices.size()!=1) throw new IOException(choices.isEmpty()?wifi?"No assigned Wi-Fi private IPv4 address found. Join the ENET adapter Wi-Fi and check its IP settings.":"No assigned wired 169.254.x.x address found. Configure USB Ethernet in Settings.":"Multiple Ethernet addresses found. Enter the intended source IP.");
        EthernetRoute result=choices.get(0);result.wifi=wifi;return result;
    }
    public void assertAvailable() throws IOException {
        try { if(network!=null){NetworkCapabilities c=manager.getNetworkCapabilities(network);LinkProperties l=manager.getLinkProperties(network);if(c==null||l==null||!c.hasTransport(wifi?NetworkCapabilities.TRANSPORT_WIFI:NetworkCapabilities.TRANSPORT_ETHERNET)||c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)||!iface.equals(l.getInterfaceName()))throw new IOException("Selected connection was lost");boolean found=false;for(LinkAddress a:l.getLinkAddresses())if(source.equals(a.getAddress().getHostAddress()))found=true;if(!found)throw new IOException("Assigned source address changed");}else{NetworkInterface n=NetworkInterface.getByName(iface);if(n==null||!n.isUp()||!Collections.list(n.getInetAddresses()).contains(InetAddress.getByName(source)))throw new IOException("Wired source disappeared");}}catch(SocketException e){throw new IOException(e);}
    }
    public void bindProcess() throws IOException { assertAvailable();
        if(network!=null && !manager.bindProcessToNetwork(network)) throw new IOException("Android refused the Ethernet network binding");
    }
    public void release() { manager.bindProcessToNetwork(null); }
    public Socket socket(String host,int port,int timeout) throws IOException {
        assertAvailable();
        if(!(wifi?local(host):linkLocal(host))) throw new IOException("Only link-local vehicle endpoints are permitted");
        Socket socket=new Socket();
        try {
            if(network!=null) network.bindSocket(socket);
            socket.bind(new InetSocketAddress(source,0));
            socket.connect(new InetSocketAddress(host,port),timeout);
            return socket;
        } catch(IOException e) { socket.close(); throw e; }
    }
}
