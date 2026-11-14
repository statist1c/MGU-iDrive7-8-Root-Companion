package dev.mgu.standalone;
import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
public final class SettingsActivity extends Activity {
 @Override public void onCreate(Bundle state){
  boolean dark=AppSettings.dark(this);setTheme(dark?R.style.Theme_RootCompanion_Dark:R.style.Theme_RootCompanion);super.onCreate(state);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  int bg=dark?0xff11141c:0xfffaf8ff,ink=dark?0xffeef1fa:0xff1d1c22;
  getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);int pad=(int)(20*getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);box.setBackgroundColor(bg);scroll.addView(box);setContentView(scroll);
  if(android.os.Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
  scroll.setOnApplyWindowInsetsListener((v,insets)->{if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());scroll.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else scroll.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});scroll.requestApplyInsets();
  TextView title=new TextView(this);title.setText("App settings");title.setTextSize(26);title.setTextColor(ink);box.addView(title);
  Button back=new Button(this);back.setText("Done");box.addView(back);back.setOnClickListener(v->finish());
  TextView label=new TextView(this);label.setText("Appearance");label.setTextSize(20);label.setTextColor(ink);box.addView(label);
  RadioGroup themes=new RadioGroup(this);box.addView(themes);styleGroup(themes,dark,pad);String[] keys={"auto","light","dark"},names={"Auto · follow phone theme","Light","Dark"};
  for(int i=0;i<3;i++){RadioButton r=new RadioButton(this);r.setId(View.generateViewId());r.setText(names[i]);themes.addView(r);r.setChecked(AppSettings.prefs(this).getString("theme","auto").equals(keys[i]));final String key=keys[i];r.setOnClickListener(v->{AppSettings.prefs(this).edit().putString("theme",key).apply();recreate();});}
  TextView connection=new TextView(this);connection.setText("Connection profile");connection.setTextSize(20);connection.setTextColor(ink);box.addView(connection);
  String[] modes={"ethernet","wifi"},labels={"USB Ethernet","Wi-Fi ENET · experimental"};RadioGroup routes=new RadioGroup(this);box.addView(routes);styleGroup(routes,dark,pad);
  for(int i=0;i<2;i++){RadioButton r=new RadioButton(this);r.setText(labels[i]);routes.addView(r);r.setChecked(AppSettings.mode(this).equals(modes[i]));r.setEnabled(!BackendService.running);final String mode=modes[i];r.setOnClickListener(v->AppSettings.prefs(this).edit().putString("transport",mode).apply());}
  TextView help=new TextView(this);help.setText("Join your adapter’s Wi-Fi in Android settings first. Enter its diagnostic gateway on the Connect page. The adapter must forward raw ENET TCP 6801 and head-unit SSH TCP 22. Internet access is not required. Cellular and VPN connections are not selected. Wired Ethernet is recommended for flashing: wireless interruptions can leave the module unusable. No physical adapter or BMW validation is claimed.");help.setTextColor(ink);help.setPadding(0,pad,0,pad);box.addView(help);
  EditText ssh=new EditText(this);ssh.setSingleLine(true);ssh.setHint("Optional Wi-Fi SSH IPv4 override");ssh.setText(AppSettings.prefs(this).getString("ssh_override",""));ssh.setEnabled(!BackendService.running);box.addView(ssh);
  TextView error=new TextView(this);error.setTextColor(ink);box.addView(error);
  back.setOnClickListener(v->{String ip=ssh.getText().toString().trim();if(!ip.isEmpty()&&!EthernetRoute.local(ip)){error.setText("Use a private or usable link-local IPv4 address.");return;}if(!BackendService.running)AppSettings.prefs(this).edit().putString("ssh_override",ip).apply();finish();});
  Button wifi=new Button(this);wifi.setText("Open Android Wi-Fi settings");box.addView(wifi);wifi.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
 }
 private void styleGroup(RadioGroup group,boolean dark,int pad){android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(dark?0xff1e2330:0xffefedf5);bg.setCornerRadius(pad);group.setBackground(bg);group.setPadding(pad/2,pad/2,pad/2,pad/2);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,pad/2,0,pad);group.setLayoutParams(lp);}
}
