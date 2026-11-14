package dev.mgu.standalone;
import android.app.*;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
final class ThemeChecks {
 static void run(Instrumentation t) throws Exception {
  if(!EthernetRoute.local("169.254.255.106")||!EthernetRoute.local("169.254.255.1")||EthernetRoute.linkLocal("169.254.255.106")||!EthernetRoute.local("192.168.4.1")||!EthernetRoute.local("10.1.2.3")||EthernetRoute.local("8.8.8.8")||EthernetRoute.local("127.0.0.1"))throw new AssertionError("Java IPv4 policy");
  EthernetRoute wifi=EthernetRoute.find(t.getTargetContext(),"","wifi");wifi.assertAvailable();wifi.bindProcess();wifi.release();if(wifi.network==null)throw new AssertionError("Wi-Fi network missing");
  File dir=new File(t.getTargetContext().getExternalFilesDir(null),"theme-previews");dir.mkdirs();
  Bitmap[] icon=new Bitmap[1];t.runOnMainSync(()->{icon[0]=Bitmap.createBitmap(432,432,Bitmap.Config.ARGB_8888);android.graphics.drawable.Drawable d=t.getTargetContext().getDrawable(R.mipmap.ic_launcher);d.setBounds(0,0,432,432);d.draw(new android.graphics.Canvas(icon[0]));});try(OutputStream o=new FileOutputStream(new File(dir,"icon.png"))){icon[0].compress(Bitmap.CompressFormat.PNG,100,o);}icon[0].recycle();
  for(String theme:new String[]{"light","dark","auto"}){
   AppSettings.prefs(t.getTargetContext()).edit().putString("theme",theme).putString("transport","wifi").putString("wifi_gateway","192.168.4.1").commit();
   MainActivity a=(MainActivity)t.startActivitySync(new Intent(t.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));t.waitForIdleSync();
   Field color=MainActivity.class.getDeclaredField("BG");color.setAccessible(true);boolean expected=theme.equals("dark")||(theme.equals("auto")&&(a.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
   if(color.getInt(a)!=(expected?0xff11141c:0xfffaf8ff))throw new AssertionError("Theme mismatch "+theme);
   Field gateway=MainActivity.class.getDeclaredField("gateway");gateway.setAccessible(true);if(!((EditText)gateway.get(a)).getText().toString().equals("192.168.4.1"))throw new AssertionError("Wi-Fi profile not loaded");
   t.runOnMainSync(()->a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE));t.waitForIdleSync();Thread.sleep(200);Bitmap b=t.getUiAutomation().takeScreenshot();try(OutputStream o=new FileOutputStream(new File(dir,theme+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,o);}b.recycle();t.runOnMainSync(()->{a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);a.finish();});t.waitForIdleSync();
  }
  SettingsActivity settings=(SettingsActivity)t.startActivitySync(new Intent(t.getTargetContext(),SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));t.waitForIdleSync();
  java.util.ArrayList<View> matches=new java.util.ArrayList<>();settings.getWindow().getDecorView().findViewsWithText(matches,"Wi-Fi ENET",View.FIND_VIEWS_WITH_TEXT);if(matches.isEmpty())throw new AssertionError("Wi-Fi settings missing");
  t.runOnMainSync(()->settings.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE));t.waitForIdleSync();Thread.sleep(350);Bitmap[] shot=new Bitmap[1];t.runOnMainSync(()->{View v=settings.getWindow().getDecorView();shot[0]=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(shot[0]));});Bitmap b=shot[0];try(OutputStream o=new FileOutputStream(new File(dir,"settings.png"))){b.compress(Bitmap.CompressFormat.PNG,100,o);}b.recycle();t.runOnMainSync(()->{settings.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);settings.finish();});
  AppSettings.prefs(t.getTargetContext()).edit().clear().commit();
 }
}
