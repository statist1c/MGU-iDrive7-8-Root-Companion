package dev.mgu.standalone;
import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
public final class SettingsActivity extends Activity {
 boolean updateBusy; TextView updateStatus; private LinearLayout updateBody;
 final android.os.Handler updateHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 final java.util.concurrent.ExecutorService updateWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
 private final Runnable updateRefresh=new Runnable(){public void run(){if(updateStatus!=null)updateStatus.setText(AppUpdater.prefs(SettingsActivity.this).getString("status","Check GitHub releases for updates."));updateHandler.postDelayed(this,1000);}};

 @Override public void onCreate(Bundle state){
  boolean dark=AppSettings.dark(this);setTheme(dark?R.style.Theme_RootCompanion_Dark:R.style.Theme_RootCompanion);super.onCreate(state);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  int bg=dark?0xff11141c:0xfffaf8ff,ink=dark?0xffeef1fa:0xff1d1c22;
  getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);int pad=(int)(20*getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);box.setBackgroundColor(bg);scroll.addView(box);setContentView(scroll);
  if(android.os.Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
  scroll.setOnApplyWindowInsetsListener((v,insets)->{if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());scroll.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else scroll.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});scroll.requestApplyInsets();
  TextView title=new TextView(this);title.setText("App settings");title.setTextSize(26);title.setTextColor(ink);box.addView(title);
  Button back=new Button(this);back.setText("Done");styleButton(back,dark,true);box.addView(back);back.setOnClickListener(v->finish());
  AppUpdateUi.attach(this,box);
  TextView label=new TextView(this);label.setText("Appearance");label.setTextSize(20);label.setTextColor(ink);box.addView(label);
  RadioGroup themes=new RadioGroup(this);box.addView(themes);styleGroup(themes,dark,pad);String[] keys={"auto","light","dark"},names={"Auto · follow phone theme","Light","Dark"};
  for(int i=0;i<3;i++){RadioButton r=new RadioButton(this);r.setId(View.generateViewId());r.setText(names[i]);themes.addView(r);r.setChecked(AppSettings.prefs(this).getString("theme","auto").equals(keys[i]));final String key=keys[i];r.setOnClickListener(v->{AppSettings.prefs(this).edit().putString("theme",key).apply();recreate();});}
  TextView connection=new TextView(this);connection.setText("Connection profile");connection.setTextSize(20);connection.setTextColor(ink);box.addView(connection);
  String[] modes={"ethernet","wifi"},labels={"USB-C Ethernet adapter","Wi-Fi ENET · VXSCAN / MHD ENET"};RadioGroup routes=new RadioGroup(this);box.addView(routes);styleGroup(routes,dark,pad);
  for(int i=0;i<2;i++){RadioButton r=new RadioButton(this);r.setText(labels[i]);routes.addView(r);r.setChecked(AppSettings.mode(this).equals(modes[i]));r.setEnabled(!BackendService.running);final String mode=modes[i];r.setOnClickListener(v->AppSettings.prefs(this).edit().putString("transport",mode).apply());}
  TextView help=new TextView(this);help.setText("VXSCAN ENET Wi-Fi/USB adapter profile (product-page identification); user-reported Wi-Fi name MHD ENET_4472. Exact hardware identity and SSH compatibility remain unverified. Join adapter Wi-Fi in Android settings first. The observed router 169.254.255.1 is not necessarily the vehicle diagnostic gateway. The phone source is detected automatically; do not copy another device’s IP. Enter its diagnostic gateway on the Connect page. The adapter must forward raw ENET TCP 6801 and head-unit SSH TCP 22. Internet access is not required. Cellular and VPN connections are not selected. Wired Ethernet is recommended for flashing: wireless interruptions can leave the module unusable. No physical adapter or BMW validation is claimed.");help.setTextColor(ink);help.setPadding(0,pad,0,pad);box.addView(help);
  EditText ssh=new EditText(this);ssh.setSingleLine(true);ssh.setHint("Optional Wi-Fi SSH IPv4 override");ssh.setText(AppSettings.prefs(this).getString("ssh_override",""));ssh.setEnabled(!BackendService.running);box.addView(ssh);
  TextView error=new TextView(this);error.setTextColor(ink);box.addView(error);
  back.setOnClickListener(v->{String ip=ssh.getText().toString().trim();if(!ip.isEmpty()&&!EthernetRoute.local(ip)){error.setText("Use a private or usable link-local IPv4 address.");return;}if(!BackendService.running)AppSettings.prefs(this).edit().putString("ssh_override",ip).apply();finish();});
  Button wifi=new Button(this);wifi.setText("Open Android Wi-Fi settings");styleButton(wifi,dark,false);box.addView(wifi);wifi.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
 }
 void updateHeading(LinearLayout parent,String title){updateBody=new LinearLayout(this);updateBody.setOrientation(LinearLayout.VERTICAL);int pad=dp(16);updateBody.setPadding(pad,pad,pad,pad);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(AppSettings.dark(this)?0xff1e2330:0xffefedf5);bg.setCornerRadius(dp(22));updateBody.setBackground(bg);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(12),0,dp(20));parent.addView(updateBody,params);TextView label=updateLabel(title);label.setTextSize(20);}
 TextView updateLabel(String text){TextView label=new TextView(this);label.setText(text);label.setTextSize(14);label.setTextColor(AppSettings.dark(this)?0xffeef1fa:0xff1d1c22);label.setPadding(0,0,0,dp(10));updateBody.addView(label);return label;}
 CheckBox updateCheck(String text,boolean checked){CheckBox box=new CheckBox(this);box.setText(text);box.setChecked(checked);updateBody.addView(box);return box;}
 void updateButton(String title,boolean primary,Runnable action){Button button=new Button(this);button.setText(title);styleButton(button,AppSettings.dark(this),primary);updateBody.addView(button);button.setOnClickListener(v->action.run());}
 void message(String text){if(!isDestroyed())new AlertDialog.Builder(this).setTitle("App updates").setMessage(text).setPositiveButton("OK",null).show();}
 @Override protected void onResume(){super.onResume();updateHandler.post(updateRefresh);AppUpdater.schedule(this);if(AppUpdater.prefs(this).getLong("download_id",-1)>=0)AppUpdater.scheduleValidation(this);}
 @Override protected void onPause(){updateHandler.removeCallbacks(updateRefresh);super.onPause();}
 @Override protected void onDestroy(){updateHandler.removeCallbacksAndMessages(null);updateWorker.shutdown();super.onDestroy();}
 private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private android.graphics.drawable.GradientDrawable buttonShape(int fill,int stroke){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(24));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
 private void styleButton(Button b,boolean dark,boolean solid){
  int blue=dark?0xFFAFC3FF:0xFF30477F,pale=dark?0xFF28385B:0xFFDFE5F6,muted=dark?0xFFB1B9CA:0xFF5A5D6B;
  b.setAllCaps(false);b.setTextSize(14);b.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(14),dp(10),dp(14),dp(10));b.setStateListAnimator(null);b.setElevation(0);
  android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();states.addState(new int[]{-android.R.attr.state_enabled},buttonShape(dark?0xFF3C4558:0xFFE1DFE8,0));states.addState(new int[]{android.R.attr.state_pressed},buttonShape(solid?blue:pale,blue));states.addState(new int[]{},buttonShape(solid?blue:android.graphics.Color.TRANSPARENT,solid?0:blue));b.setBackground(states);
  b.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{muted,solid?(dark?0xFF10254B:android.graphics.Color.WHITE):blue}));
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(4),0,dp(6));b.setLayoutParams(params);
 }
 private void styleGroup(RadioGroup group,boolean dark,int pad){android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(dark?0xff1e2330:0xffefedf5);bg.setCornerRadius(pad);group.setBackground(bg);group.setPadding(pad/2,pad/2,pad/2,pad/2);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,pad/2,0,pad);group.setLayoutParams(lp);}
}
