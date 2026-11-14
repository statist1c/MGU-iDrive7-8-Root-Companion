package dev.mgu.standalone;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/** Original native UI inspired by the reference installer's card/tab organization. */
public final class MainActivity extends Activity {
    private static final String APP_NAME="MGU iDrive7-8 Root Companion";
    private int BG=0xFFFAF8FF, CARD=0xFFEFEDF5, INK=0xFF1D1C22,
        MUTED=0xFF5A5D6B, BLUE=0xFF30477F, PALE_BLUE=0xFFDFE5F6,
        AMBER=0xFF8B5700, PALE_AMBER=0xFFFFEED2, RED=0xFF9C3437, LINE=0xFFD6D5DF;
    private String settingsSignature,profile; private boolean dark;
    private int ON_BLUE=Color.WHITE;
    private LinearLayout body;
    private EditText source,gateway,vin;
    private Spinner arch;
    private CheckBox legacy,follow;
    private TextView status,log,identity,connectionLabel,adapterInfo,stepLabel,stepTitle,sshAddress,backupSummary,monitorSummary,filterAll,filterIssues;
    private final ArrayList<Button> actions=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService background=Executors.newSingleThreadExecutor();
    private final ScrollView[] tabs=new ScrollView[4];
    private final LinearLayout[] stepPages=new LinearLayout[4],navItems=new LinearLayout[4];
    private final TextView[] stepButtons=new TextView[4],navLabels=new TextView[4];
    private final View[] progressSegments=new View[4],navPills=new View[4];
    private final Icon[] navIcons=new Icon[4];
    private boolean configuring,onlyIssues,clearedView;
    private Boolean busyShown;
    private String logs="",lastResult="",lastSnapshot="";
    private int selectedTab=0,selectedStep=0;
    private long nextConnectionPoll;
    private final Runnable refresh=new Runnable() { public void run() { update(); handler.postDelayed(this,1000); } };
    private static final String[] VARIANTS={"Choose hardware variant…","id7","id8-bdc3","id8-bcp"};

    @Override public void onCreate(Bundle state) {
        dark=AppSettings.dark(this); profile=AppSettings.mode(this); settingsSignature=AppSettings.signature(this);
        setTheme(dark?R.style.Theme_RootCompanion_Dark:R.style.Theme_RootCompanion);
        if(dark){BG=0xFF11141C;CARD=0xFF1E2330;INK=0xFFEEF1FA;MUTED=0xFFB1B9CA;BLUE=0xFFAFC3FF;PALE_BLUE=0xFF28385B;AMBER=0xFFFFD68C;PALE_AMBER=0xFF3B2C18;RED=0xFFFFADB1;LINE=0xFF3C4558;ON_BLUE=0xFF10254B;}
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(CARD);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(Build.VERSION.SDK_INT>=30) getWindow().setDecorFitsSystemWindows(false);
        LinearLayout shell=new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL); shell.setBackgroundColor(BG); shell.setFocusableInTouchMode(true);
        setContentView(shell);
        shell.setOnApplyWindowInsetsListener((view,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()); shell.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else shell.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(16),dp(10),dp(12),dp(8));
        TextView name=text(APP_NAME,21,INK); name.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); name.setMaxLines(2); name.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(name,new LinearLayout.LayoutParams(0,dp(62),1));
        FrameLayout monitorShortcut=new FrameLayout(this); monitorShortcut.setContentDescription("Open activity monitor");
        Icon shortcut=new Icon(1); shortcut.color=BLUE; FrameLayout.LayoutParams shortcutParams=new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER); monitorShortcut.addView(shortcut,shortcutParams);
        monitorShortcut.setOnClickListener(v->selectTab(1)); header.addView(monitorShortcut,new LinearLayout.LayoutParams(dp(48),dp(48))); TextView settings=text("⚙",28,BLUE);settings.setGravity(Gravity.CENTER);settings.setContentDescription("App settings");settings.setOnClickListener(v->{if(!configuring)startActivity(new Intent(this,SettingsActivity.class));});header.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));shell.addView(header);
        status=text(profile.equals("wifi")?"Ready · Wi-Fi ENET experimental":"Ready · connect USB Ethernet",12,MUTED); status.setPadding(dp(14),dp(8),dp(14),dp(8)); status.setMaxLines(2); status.setEllipsize(TextUtils.TruncateAt.END); status.setBackground(shape(CARD,14,0));
        LinearLayout.LayoutParams statusParams=new LinearLayout.LayoutParams(-1,-2); statusParams.setMargins(dp(16),0,dp(16),dp(8)); shell.addView(status,statusParams);
        status.setContentDescription("Current operation status; tap for full details");
        status.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Current status").setMessage(status.getText()).setPositiveButton("Close",null).setNeutralButton("Monitor",(d,w)->selectTab(1)).show());
        FrameLayout host=new FrameLayout(this); shell.addView(host,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<4;i++) { tabs[i]=new ScrollView(this); tabs[i].setFillViewport(true); tabs[i].setClipToPadding(false); host.addView(tabs[i],new FrameLayout.LayoutParams(-1,-1)); }
        buildSetup(); buildMonitor(); buildBackups(); buildSsh();
        LinearLayout navigation=new LinearLayout(this); navigation.setPadding(dp(8),dp(8),dp(8),dp(7)); navigation.setBackgroundColor(CARD);
        String[] names={"Setup","Monitor","Backups","SSH"};
        for(int i=0;i<4;i++) {
            final int index=i; LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.setMinimumHeight(dp(64));
            item.setTag("tab-"+names[i].toLowerCase(Locale.ROOT)); item.setContentDescription(names[i]+" tab"); item.setClickable(true); item.setFocusable(true);
            FrameLayout pill=new FrameLayout(this); Icon icon=new Icon(i); pill.addView(icon,new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
            item.addView(pill,new LinearLayout.LayoutParams(dp(64),dp(32))); TextView caption=text(names[i],12,MUTED); caption.setGravity(Gravity.CENTER); caption.setPadding(0,dp(4),0,0); item.addView(caption);
            item.setOnClickListener(v->selectTab(index)); navigation.addView(item,new LinearLayout.LayoutParams(0,-2,1)); navItems[i]=item; navLabels[i]=caption; navPills[i]=pill; navIcons[i]=icon;
        }
        shell.addView(navigation); selectTab(0); selectStep(0); shell.requestApplyInsets(); shell.requestFocus();
        if(state!=null) { if(profile.equals(state.getString("profile",profile))) { source.setText(state.getString("source","")); gateway.setText(state.getString("gateway","169.254.254.36")); } vin.setText(state.getString("vin","")); arch.setSelection(state.getInt("arch",0)); legacy.setChecked(state.getBoolean("legacy",false)); selectStep(state.getInt("step",0)); selectTab(state.getInt("tab",0)); }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
    }
    private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int fill,int radius,int stroke) { GradientDrawable d=new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(radius)); if(stroke!=0) d.setStroke(dp(1),stroke); return d; }
    private TextView text(String value,int size,int color) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setLineSpacing(dp(2),1); return t; }
    private LinearLayout page(int tab) { LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(16),dp(12),dp(16),dp(24)); tabs[tab].addView(content); return content; }
    private LinearLayout card(LinearLayout parent,String title) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(16),dp(16),dp(12)); box.setBackground(shape(CARD,22,0));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2); params.bottomMargin=dp(14); parent.addView(box,params);
        if(!title.isEmpty()) { TextView heading=text(title,18,INK); heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); heading.setPadding(0,0,0,dp(8)); box.addView(heading); }
        body=box; return box;
    }
    private TextView label(String value,int size,int color) { TextView t=text(value,size,color); t.setPadding(0,0,0,dp(10)); body.addView(t); return t; }
    private EditText input(String hint) {
        EditText e=new EditText(this); e.setHint(hint); e.setTextSize(15); e.setTextColor(INK); e.setHintTextColor(MUTED); e.setSingleLine(true); e.setPadding(dp(14),dp(13),dp(14),dp(13)); e.setMinHeight(dp(50)); e.setBackground(shape(BG,13,dark?LINE:0xFF898B99)); e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2); params.bottomMargin=dp(10); body.addView(e,params); return e;
    }
    private Button button(String title,Runnable run) { return styledButton(title,run,BLUE,true); }
    private Button secondary(String title,Runnable run) { return styledButton(title,run,BLUE,false); }
    private Button danger(String title,Runnable run) { return styledButton(title,run,RED,false); }
    private Button styledButton(String title,Runnable run,int color,boolean solid) {
        Button b=new Button(this); b.setText(title); b.setAllCaps(false); b.setTextSize(14); b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); b.setMinHeight(dp(48)); b.setMinimumHeight(dp(48)); b.setPadding(dp(14),dp(10),dp(14),dp(10)); b.setStateListAnimator(null); b.setElevation(0);
        StateListDrawable backgrounds=new StateListDrawable(); backgrounds.addState(new int[]{-android.R.attr.state_enabled},shape(dark?LINE:0xFFE1DFE8,24,0)); backgrounds.addState(new int[]{android.R.attr.state_pressed},shape(solid?color:PALE_BLUE,24,color)); backgrounds.addState(new int[]{},shape(solid?color:Color.TRANSPARENT,24,solid?0:color)); b.setBackground(backgrounds);
        b.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{MUTED,solid?ON_BLUE:color}));
        b.setOnClickListener(v->run.run()); LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2); params.setMargins(0,dp(4),0,dp(6)); body.addView(b,params); actions.add(b); return b;
    }
    private void op(String title,String action) { button(title,()->prepare(action)); }
    private void sectionHeading(LinearLayout parent,String title,String subtitle) { TextView heading=text(title,25,INK); heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); parent.addView(heading); TextView sub=text(subtitle,14,MUTED); sub.setPadding(0,dp(5),0,dp(18)); parent.addView(sub); }
    private void buildSetup() {
        LinearLayout content=page(0),summary=new LinearLayout(this); summary.setGravity(Gravity.CENTER_VERTICAL);
        stepLabel=text("STEP 1 OF 4",12,BLUE); stepLabel.setTypeface(Typeface.DEFAULT_BOLD); summary.addView(stepLabel,new LinearLayout.LayoutParams(0,-2,1));
        connectionLabel=text("●  No adapter",12,AMBER); connectionLabel.setPadding(dp(12),dp(8),dp(12),dp(8)); connectionLabel.setBackground(shape(PALE_AMBER,22,LINE)); summary.addView(connectionLabel); content.addView(summary);
        stepTitle=text("Connect to the car",25,INK); stepTitle.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); stepTitle.setPadding(0,dp(10),0,dp(14)); content.addView(stepTitle);
        LinearLayout progress=new LinearLayout(this); for(int i=0;i<4;i++) { View segment=new View(this); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(5),1); p.setMargins(0,0,i==3?0:dp(4),0); progress.addView(segment,p); progressSegments[i]=segment; } content.addView(progress);
        LinearLayout steps=new LinearLayout(this); steps.setPadding(0,dp(18),0,dp(14)); String[] titles={"Connect","Vehicle","Root","Verify"};
        for(int i=0;i<4;i++) { final int step=i; TextView chip=text(titles[i],13,MUTED); chip.setGravity(Gravity.CENTER); chip.setPadding(dp(4),dp(12),dp(4),dp(12)); chip.setMinimumHeight(dp(46)); chip.setTag("step-"+titles[i].toLowerCase(Locale.ROOT)); chip.setContentDescription(titles[i]+" setup step"); chip.setClickable(true); chip.setFocusable(true); chip.setOnClickListener(v->selectStep(step)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.setMargins(0,0,i==3?0:dp(6),0); steps.addView(chip,p); stepButtons[i]=chip; } content.addView(steps);
        for(int i=0;i<4;i++) { stepPages[i]=new LinearLayout(this); stepPages[i].setOrientation(LinearLayout.VERTICAL); content.addView(stepPages[i]); }
        buildConnectStep(); buildVehicleStep(); buildRootStep(); buildVerifyStep();
    }
    private void buildConnectStep() {
        LinearLayout parent=stepPages[0]; card(parent,"Get connected");
        label(profile.equals("wifi")?"VXSCAN ENET Wi-Fi/USB profile · experimental. User adapter SSID: MHD ENET_4472. Join adapter Wi-Fi, then run a read-only check. Wired Ethernet is recommended for flashing.":"Use a wired connection with your parked vehicle awake. Start with a read-only connection check.",15,MUTED);
        label(profile.equals("wifi")?"1   Connect the Wi-Fi ENET adapter to OBD.\n2   Join its Wi-Fi in Android settings.\n3   Enter the diagnostic gateway supplied by its maker.":"1   Connect USB-C Ethernet to your phone.\n2   Connect ENET to the car's OBD port.\n3   Check the phone's Ethernet address.",15,INK);
        adapterInfo=label("No assigned connection address detected yet.",14,MUTED); button("Detect connection address",()->detect());
        card(parent,"Connection settings"); label("Enter an assigned phone IP, or leave it blank for detection. For Wi-Fi, enter the diagnostic gateway supplied by the adapter maker.",14,MUTED);
        label("Phone connection IP",12,MUTED); source=input("Detect automatically"); source.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        label("ENET gateway",12,MUTED); gateway=input(profile.equals("wifi")?"Adapter diagnostic gateway IPv4":"169.254.254.36"); gateway.setText(AppSettings.prefs(this).getString(profile+"_gateway",profile.equals("wifi")?"":"169.254.254.36"));source.setText(AppSettings.prefs(this).getString(profile+"_source","")); gateway.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        secondary("Open Android connection settings",()->startActivity(new Intent(profile.equals("wifi")?Settings.ACTION_WIFI_SETTINGS:Settings.ACTION_SETTINGS))); button("Continue to vehicle check",()->selectStep(1));
        card(parent,"Phone root is optional"); label("The app runs without phone root when Android supplies a working Ethernet address and route. Some phones need their wired IP configured in Settings; rooted phones can use the helper under SSH → Phone Ethernet.",14,MUTED);
        secondary("Ethernet helper & advanced settings",()->selectTab(3));
        safetyCard(parent);
    }
    private void buildVehicleStep() {
        LinearLayout parent=stepPages[1]; card(parent,"Identify your head unit"); label("Select the actual hardware variant. Check the read VIN against your physical vehicle before authorizing any change.",15,MUTED);
        label("Vehicle VIN",12,MUTED); vin=input("Exact 17-character VIN"); vin.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        label("Head-unit variant",12,MUTED); arch=new Spinner(this); ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,VARIANTS); arch.setAdapter(adapter); arch.setMinimumHeight(dp(52)); arch.setBackgroundTintList(ColorStateList.valueOf(BLUE)); body.addView(arch,new LinearLayout.LayoutParams(-1,-2));
        op("Check compatibility & read VIN","inspect");
        card(parent,"Vehicle check"); identity=label("No compatibility check completed. The supported-MGU gate is a catalog check, not a guarantee of safe flashing.",14,MUTED); secondary("Read current status",()->prepare("status")); button("Continue to root setup",()->selectStep(2));
        safetyCard(parent);
    }
    private void buildRootStep() {
        LinearLayout parent=stepPages[2]; card(parent,"Before you flash"); body.setBackground(shape(PALE_AMBER,22,LINE)); label("Flashing can permanently damage the module, erase coding / CAFD and delete Digital Key pairings.",16,AMBER);
        label("Have a verified restorable coding backup, a restore plan and stable phone/vehicle power. Keep the ENET cable connected. Android can stop apps even with a wake lock.",14,INK); secondary("Read all safety information",()->showSafety());
        card(parent,"One-time root setup"); label("Run a fresh, matching vehicle check first. The app flashes the debug package, enrolls SSH and requests persistence. Never repeat a flash merely to fix reset settings.",15,MUTED); op("Flash + Enable SSH","flash"); secondary("Go to vehicle check",()->selectStep(1));
        card(parent,"Already flashed?"); label("Repair SSH only if it stopped answering. This does not flash firmware, but it changes debug / SSH state and may reboot the unit.",14,MUTED); secondary("Repair SSH & request persistence",()->prepare("enable")); secondary("Continue to verification",()->selectStep(3));
    }
    private void buildVerifyStep() {
        LinearLayout parent=stepPages[3]; card(parent,"Verify the result"); label("Read the current state after the operation. Port 22 being open alone does not prove a successful root login. Follow the operation log for the backend's reported result.",15,MUTED); op("Read current status","status"); secondary("Open live monitor",()->selectTab(1));
        card(parent,"Keep your access information"); label("Generated SSH identities and connection details are saved privately on this phone. Export them before clearing app data or uninstalling.",14,MUTED); secondary("Open backups",()->selectTab(2));
        safetyCard(parent);
    }
    private void buildMonitor() {
        LinearLayout parent=page(1); sectionHeading(parent,"Activity monitor","Connection status and operation output from this phone."); card(parent,"Current operation"); monitorSummary=label("No operation has been started.",15,MUTED);
        secondary("Export full session log",()->saveIntent(42,"text/plain","mgu-session-log.txt"));
        LinearLayout filter=new LinearLayout(this); TextView all=text("All output",13,BLUE); filterAll=all; all.setPadding(dp(12),dp(10),dp(12),dp(10)); all.setBackground(shape(PALE_BLUE,20,0)); all.setOnClickListener(v->{ onlyIssues=false; renderLog(); }); filter.addView(all);
        TextView issues=text("Issues only",13,MUTED); filterIssues=issues; issues.setPadding(dp(12),dp(10),dp(12),dp(10)); issues.setOnClickListener(v->{ onlyIssues=true; renderLog(); }); filter.addView(issues); body.addView(filter);
        follow=new CheckBox(this); follow.setText("Follow new output"); follow.setTextSize(13); follow.setTextColor(MUTED); follow.setButtonTintList(ColorStateList.valueOf(BLUE)); follow.setChecked(true); body.addView(follow);
        card(parent,"Session output"); log=label("Nothing logged yet. Connect the adapter and run a read-only vehicle check to begin.",13,MUTED); log.setTypeface(Typeface.MONOSPACE); log.setTextIsSelectable(true);
        label("Logs can include VIN and vehicle addresses. Review them before sharing. Exports contain the full log, even when a display filter is selected.",12,MUTED); secondary("Clear display only",()->{ clearedView=true; renderLog(); });
    }
    private void buildBackups() {
        LinearLayout parent=page(2); sectionHeading(parent,"Backups & keys","Keep a private copy before changing or removing the app."); card(parent,"Saved on this device"); backupSummary=label("No saved SSH identities or coding dumps yet.",15,MUTED); secondary("Refresh saved items",()->refreshBackupSummary());
        card(parent,"Export your private bundle"); label("Save SSH private keys, VIN, logs and any coding dumps through Android's document picker. Protect this ZIP like a password.",15,MUTED); button("Export private backup",()->requestPrivateExport());
        card(parent,"Read-only coding dump"); label("Reads coding data without reflashing. This reference dump is not a guaranteed coding-tool backup or an automatic restore. Make and verify a restorable backup with your coding tool before flashing.",14,MUTED); secondary("Create coding dump",()->prepare("preflight"));
        safetyCard(parent);
    }
    private void buildSsh() {
        LinearLayout parent=page(3); sectionHeading(parent,"SSH & advanced tools","Maintenance for an authorized MGU head unit."); card(parent,"SSH connection"); sshAddress=label("Head-unit address: not detected\nUser: root   ·   Port: 22",15,INK); label("SSH is bundled in the app. This screen provides maintenance controls; it is not an interactive terminal. First-use host identity is pinned locally per VIN.",14,MUTED);
        card(parent,"Repair & persistence"); secondary("Repair SSH on an already-flashed unit",()->prepare("enable")); secondary("Make existing SSH persistent",()->prepare("persist")); secondary("Stop debug / SSH",()->prepare("close")); danger("Remove persistence & disable SSH",()->prepare("disable"));
        card(parent,"Connection security"); legacy=new CheckBox(this); legacy.setText("Allow legacy RSA/SHA1 for this operation"); legacy.setTextSize(14); legacy.setTextColor(INK); legacy.setButtonTintList(ColorStateList.valueOf(BLUE)); body.addView(legacy); label("Advanced opt-in only. No automatic algorithm downgrade is used. Investigate an unexpected changed host key; do not reflash just to repair trust.",13,MUTED); secondary("Review / reset local SSH host key",()->resetHostPin());
        card(parent,"Phone Ethernet · root optional"); label("Only needed if Android settings cannot provide a working wired address and route. Requires a phone that is already rooted and your explicit approval.",14,MUTED); secondary("Configure wired IP using root",()->rootDialog(false)); secondary("Remove this app's rooted network setup",()->rootDialog(true)); secondary("Open battery settings",()->startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        card(parent,"Advanced key repair"); label("Fix refused key re-flashes the head unit and carries the same coding, Digital Key and damage risks as a normal flash. Use it only for the documented key-refusal case.",14,RED); danger("Fix refusing key — re-flash",()->prepare("fix-key"));
        card(parent,"About this app"); label(APP_NAME+"\nv0.8.0 pre-release · runs entirely on this phone",14,MUTED); secondary("Safety & limitations",()->showSafety());
    }
    private void safetyCard(LinearLayout parent) { card(parent,"Experimental · read before changes"); label("Not tested on a physical phone or BMW head unit. Flashing can erase coding and Digital Keys or damage the module. The upstream root login key is shared.",13,MUTED); secondary("Safety information",()->showSafety()); }
    private void showSafety() {
        TextView info=text("Use only on a vehicle you own or are authorized to service, parked and awake with suitable stable power.\n\nFlash and fix-key can permanently damage the head unit, erase coding / CAFD and delete Digital Key pairings. Verify a restorable coding backup and restore plan first; a diagnostic dump is not a guaranteed restore. Do not repeat flashing to fix reset settings.\n\nKeep the phone, power and ENET cable connected until completion. Android may terminate an app despite its foreground service, battery exemptions and wake lock. There is no safe cancel-flash button or automatic retry.\n\nPersistent root SSH increases exposure. The default upstream root key is shared; isolate the vehicle from untrusted networks and protect exported keys.\n\nPhone root is optional and only used by the explicitly confirmed Ethernet helper. The app cannot add missing USB Ethernet drivers or guarantee OEM routing.\n\nThis app has been built and checked in an Android emulator. It has not been tested on your physical phones or a real BMW head unit. Disable does not restore coding, firmware or Digital Key pairings.",15,INK); info.setPadding(dp(20),dp(12),dp(20),dp(16)); ScrollView scroll=new ScrollView(this); scroll.addView(info); new AlertDialog.Builder(this).setTitle("Safety & limitations").setView(scroll).setPositiveButton("Understood",null).show();
    }
    private void requestPrivateExport() { if(BackendService.running||configuring) return; new AlertDialog.Builder(this).setTitle("Private vehicle data").setMessage("The ZIP contains SSH private keys, VIN, logs and any coding dumps. Save it only to storage you trust. Anyone with a trusted root key may access the head unit. Uninstalling the app clears its private data.").setNegativeButton("Cancel",null).setPositiveButton("Choose destination",(d,w)->saveIntent(44,"application/zip","mgu-private-backup.zip")).show(); }
    private void refreshBackupSummary() { int identities=countDirectories(new File(getFilesDir(),"backend/data/enrollments")),dumps=countDirectories(new File(getFilesDir(),"backend/data/preflight")); File[] files=new File(getFilesDir(),"job-logs").listFiles(); backupSummary.setText(identities+" SSH key bundle"+(identities==1?"":"s")+"\n"+dumps+" coding dump"+(dumps==1?"":"s")+"\n"+(files==null?0:files.length)+" session logs\n\nStored privately inside this app."); }
    private int countDirectories(File dir) { File[] files=dir.listFiles(File::isDirectory); return files==null?0:files.length; }
    void selectTab(int index) { selectedTab=Math.max(0,Math.min(3,index)); for(int i=0;i<4;i++) { tabs[i].setVisibility(i==selectedTab?View.VISIBLE:View.GONE); navPills[i].setBackground(shape(i==selectedTab?PALE_BLUE:Color.TRANSPARENT,18,0)); navLabels[i].setTextColor(i==selectedTab?BLUE:MUTED); navLabels[i].setTypeface(i==selectedTab?Typeface.DEFAULT_BOLD:Typeface.DEFAULT); navIcons[i].color=i==selectedTab?BLUE:MUTED; navIcons[i].invalidate(); navItems[i].setSelected(i==selectedTab); } if(selectedTab==2) refreshBackupSummary(); }
    void selectStep(int step) { selectedStep=Math.max(0,Math.min(3,step)); String[] titles={"Connect to the car","Identify your head unit","Enable root SSH","Verify & keep a backup"}; stepLabel.setText("STEP "+(selectedStep+1)+" OF 4"); stepTitle.setText(titles[selectedStep]); for(int i=0;i<4;i++) { stepPages[i].setVisibility(i==selectedStep?View.VISIBLE:View.GONE); stepButtons[i].setBackground(shape(i==selectedStep?BLUE:Color.TRANSPARENT,14,i==selectedStep?0:LINE)); stepButtons[i].setTextColor(i==selectedStep?ON_BLUE:MUTED); stepButtons[i].setSelected(i==selectedStep); progressSegments[i].setBackground(shape(i==selectedStep?BLUE:LINE,4,0)); } tabs[0].post(()->tabs[0].smoothScrollTo(0,0)); }
    private void renderLog() { filterAll.setBackground(shape(onlyIssues?Color.TRANSPARENT:PALE_BLUE,20,0)); filterIssues.setBackground(shape(onlyIssues?PALE_BLUE:Color.TRANSPARENT,20,0)); filterAll.setTextColor(onlyIssues?MUTED:BLUE); filterIssues.setTextColor(onlyIssues?BLUE:MUTED); String display=logs; if(onlyIssues) { StringBuilder found=new StringBuilder(); for(String line:logs.split("\\n")) if(line.toLowerCase(Locale.ROOT).matches(".*(fail|error|warn|reject|refus|unavailable|inconclusive|permission|unsupported).*")) found.append(line).append('\n'); display=found.toString(); } if(clearedView) display="Display cleared. Saved logs have not been deleted."; else if(display.isEmpty()) display=onlyIssues?"No issues in the current log.":"Nothing logged yet. Run a read-only vehicle check to begin."; log.setText(display); }
    private void pollConnection() { if(BackendService.running||configuring) return; String preferred=source.getText().toString().trim(); background.execute(()->{ String badge,detail; boolean ready=false; try { EthernetRoute route=EthernetRoute.find(this,preferred,profile); badge="●  Address ready"; detail=route.iface+"  ·  "+route.source+"\nAssigned phone address; run the vehicle check to test reachability."; ready=true; } catch(Exception e) { java.util.List<String> candidates=EthernetRoute.interfaceCandidates(this); badge=candidates.isEmpty()?"●  No adapter":"●  IP setup needed"; detail=candidates.isEmpty()?"No usable selected connection is visible. Join adapter Wi-Fi or connect USB Ethernet.":"Adapter: "+candidates+"\nConfigure an address for the selected connection profile."; } String b=badge,d=detail; boolean ok=ready; handler.post(()->{ connectionLabel.setText(b); connectionLabel.setTextColor(ok?BLUE:AMBER); connectionLabel.setBackground(shape(ok?PALE_BLUE:PALE_AMBER,22,ok?0:LINE)); adapterInfo.setText(d); }); }); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("profile",profile); state.putString("source",source.getText().toString()); state.putString("gateway",gateway.getText().toString()); state.putString("vin",vin.getText().toString()); state.putInt("arch",arch.getSelectedItemPosition()); state.putBoolean("legacy",legacy.isChecked()); state.putInt("tab",selectedTab); state.putInt("step",selectedStep); super.onSaveInstanceState(state); }
    private final class Icon extends View {
        final int kind; int color=BLUE; final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Icon(int type) { super(MainActivity.this); kind=type; setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas original) { super.onDraw(original); Canvas c=original; c.save(); c.scale(getWidth()/24f,getHeight()/24f); paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.9f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); Path p=new Path();
            if(kind==0) { c.drawRoundRect(3,8,21,18,3,3,paint); p.moveTo(5,8); p.lineTo(8,3); p.lineTo(16,3); p.lineTo(19,8); c.drawPath(p,paint); c.drawLine(6,18,6,21,paint); c.drawLine(18,18,18,21,paint); c.drawCircle(7,13,1,paint); c.drawCircle(17,13,1,paint); }
            else if(kind==1) { for(int y=6;y<=18;y+=6) { c.drawCircle(3,y,0.7f,paint); c.drawLine(8,y,21,y,paint); } }
            else if(kind==2) { p.moveTo(3,6); p.lineTo(10,6); p.lineTo(12,9); p.lineTo(21,9); p.lineTo(21,20); p.lineTo(3,20); p.close(); c.drawPath(p,paint); }
            else { c.drawRoundRect(2,4,22,20,1,1,paint); p.moveTo(6,9); p.lineTo(9,12); p.lineTo(6,15); c.drawPath(p,paint); c.drawLine(12,15,18,15,paint); } c.restore();
        }
    }

    private void saveIntent(int code,String type,String name) { Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(type); i.addCategory(Intent.CATEGORY_OPENABLE); i.putExtra(Intent.EXTRA_TITLE,name); startActivityForResult(i,code); }
    private void detect() {
        if(BackendService.running || configuring) return;
        try { EthernetRoute route=EthernetRoute.find(this,source.getText().toString().trim(),profile); source.setText(route.source); status.setText("Assigned connection address: "+route.iface+" / "+route.source+". Run a read-only check next."); }
        catch(Exception e) { status.setText(e.getMessage()); }
    }
    private void resetHostPin() {
        if(BackendService.running || configuring) return;
        String vehicle=vin.getText().toString().trim().toUpperCase(Locale.ROOT);
        if(!vehicle.matches("[A-HJ-NPR-Z0-9]{17}")) { status.setText("Enter the exact VIN to review its SSH host pins"); return; }
        android.content.SharedPreferences prefs=getSharedPreferences("host-pins",MODE_PRIVATE);
        StringBuilder pins=new StringBuilder();
        for(Map.Entry<String,?> entry:prefs.getAll().entrySet()) if(entry.getKey().startsWith(vehicle+"@")) pins.append(entry.getKey()).append(" · SHA256:").append(entry.getValue()).append('\n');
        if(pins.length()==0) { status.setText("No host key is remembered for this VIN"); return; }
        new AlertDialog.Builder(this).setTitle("Review local SSH trust")
            .setMessage(pins+"\nA changed key can indicate another device or interception. Compare old/new fingerprints and verify the isolated vehicle connection first. Resetting removes this app's remembered host identity for this VIN; the next SSH attempt will trust and pin its first observed key. This does not alter the car or erase its SSH private key. Do not reflash merely to fix a changed host key.")
            .setNegativeButton("Keep pins",null).setPositiveButton("Reset local host pins",(d,w)->{
                android.content.SharedPreferences.Editor edit=prefs.edit();
                for(String key:prefs.getAll().keySet()) if(key.startsWith(vehicle+"@")) edit.remove(key);
                status.setText(edit.commit()?"Local host pins reset. Recheck the physical vehicle and fingerprints before connecting.":"Host pin reset could not be saved");
            }).show();
    }
    private void prepare(String action) {
        if(BackendService.running || configuring) return;
        try {
            if(arch.getSelectedItemPosition()==0) { selectTab(0); selectStep(1); throw new IOException("Choose the actual head-unit variant in Vehicle check"); }
            JSONObject req=new JSONObject().put("action",action).put("arch",VARIANTS[arch.getSelectedItemPosition()])
                .put("source",source.getText().toString().trim()).put("gateway",gateway.getText().toString().trim())
                .put("transport",profile).put("ssh_override",profile.equals("wifi")?AppSettings.prefs(this).getString("ssh_override",""):"").put("vin",vin.getText().toString().trim().toUpperCase(Locale.ROOT)).put("legacy",legacy.isChecked());
            if(!(profile.equals("wifi")?EthernetRoute.local(req.getString("gateway")):EthernetRoute.linkLocal(req.getString("gateway")))) throw new IOException("Enter a usable gateway IPv4 address for the selected profile");
            if(!action.equals("inspect") && !req.getString("vin").matches("[A-HJ-NPR-Z0-9]{17}")) { selectTab(0); selectStep(1); throw new IOException("Enter the exact physical VIN in Vehicle check"); }
            if(action.equals("inspect") || action.equals("status") || action.equals("preflight")) { start(req); return; }
            boolean flash=action.equals("flash") || action.equals("fix-key");
            LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(24,8,24,8);
            TextView warning=new TextView(this); warning.setText((flash?"This reprograms the head unit. It can brick the module, erase coding/CAFD and delete Digital Key pairings. ":"This changes root SSH / debug behavior and may reboot the head unit. ")+"Use only on your authorized parked vehicle. Keep stable power and the ENET cable connected. No hardware validation is claimed.\n\nType exactly:\nI AUTHORIZE "+req.getString("vin")); panel.addView(warning);
            CheckBox backup=new CheckBox(this); backup.setText("I have a verified restorable coding backup, a restore plan, stable power and unrestricted battery settings for this app."); if(flash) panel.addView(backup);
            CheckBox wifiAck=new CheckBox(this);wifiAck.setText("I understand Wi-Fi interruptions can brick the module, and my adapter must forward diagnostic TCP 6801 and SSH TCP 22.");if(flash&&profile.equals("wifi"))panel.addView(wifiAck);
            EditText confirm=new EditText(this); confirm.setSingleLine(true); panel.addView(confirm);
            ScrollView scroll=new ScrollView(this); scroll.addView(panel);
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Authorize "+action).setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Start",null).create();
            dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
                try {
                    if(!confirm.getText().toString().equals("I AUTHORIZE "+req.getString("vin")) || (flash && !backup.isChecked()) || (flash && profile.equals("wifi") && !wifiAck.isChecked())) { confirm.setError("Enter the exact phrase and acknowledge the backup"); return; }
                    req.put("wifi_ack",wifiAck.isChecked());req.put("confirm",confirm.getText().toString()).put("backup",backup.isChecked()); dialog.dismiss(); start(req);
                } catch(Exception e) { status.setText(e.getMessage()); }
            })); dialog.show();
        } catch(Exception e) { status.setText(e.getMessage()); }
    }
    private void start(JSONObject req) {
        try { clearedView=false; lastResult=""; selectTab(1); startForegroundService(new Intent(this,BackendService.class).putExtra("request",req.toString())); status.setText("Starting embedded backend…"); }
        catch(Exception e) { status.setText("Unable to start foreground operation: "+e.getMessage()); }
    }
    private void update() {
        boolean active=BackendService.running || configuring;
        if(busyShown==null || busyShown!=active) {
            busyShown=active;
            for(Button b:actions) b.setEnabled(!active);
            source.setEnabled(!active); gateway.setEnabled(!active); vin.setEnabled(!active); arch.setEnabled(!active); legacy.setEnabled(!active);
            if(active) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if(configuring) return;
        if(SystemClock.uptimeMillis()>=nextConnectionPoll) { nextConnectionPoll=SystemClock.uptimeMillis()+5000; pollConnection(); }
        JSONObject state=BackendService.snapshot(this);
        if(!state.optString("state").equals("Ready") && !state.toString().equals(lastSnapshot)) {
            lastSnapshot=state.toString();
            String phase=state.optString("state"), action=state.optString("action");
            String friendly=action.equals("inspect")?"Vehicle check":action.equals("flash")?"Flash + Enable SSH":action.equals("preflight")?"Coding dump":action.equals("fix-key")?"Refused-key repair":action.equals("enable")?"SSH repair":action.equals("persist")?"SSH persistence":action.equals("status")?"Read status":action;
            status.setText(friendly+" · "+phase); status.setTextColor(phase.equals("failed")?RED:BLUE);
            monitorSummary.setText(friendly+" · "+phase+(phase.equals("running")?"\nKeep phone power and ENET connected.":phase.equals("failed")?"\nRead the log before retrying. Never retry a flash automatically.":"\nThis is the backend's reported outcome; review the log."));
            logs=state.optString("log"); renderLog();
            if(selectedTab==1 && follow.isChecked() && BackendService.running) tabs[1].post(()->tabs[1].fullScroll(View.FOCUS_DOWN));
        }
        String raw=state.optString("result");
        if(!raw.isEmpty() && !raw.equals(lastResult)) {
            lastResult=raw;
            try {
                JSONObject result=new JSONObject(raw).optJSONObject("result");
                if(result!=null) {
                    vin.setText(result.optString("vin")); source.setText(result.optString("source"));
                    identity.setText("Last verified VIN: "+result.optString("vin")+"\n\n"+result.optString("svk")+"\n\nMGU IP: "+result.optString("mgu_ip")+"\nSSH port open: "+result.optBoolean("ssh_port_open")+"\n\nThe check expires after five minutes and is consumed by a vehicle change. Port reachability does not prove root login.");
                    String host=result.optString("mgu_ip"); if(host.equals("null") || host.isEmpty()) host="not reported";
                    sshAddress.setText("Head unit: "+host+"\nUser: root   ·   Port: 22");
                }
            } catch(Exception ignored) { }
        }
    }
    private void rootDialog(boolean remove) {
        if(BackendService.running || configuring) return;
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(24,8,24,8);
        EditText iface=new EditText(this); iface.setHint("Actual wired interface, e.g. eth0"); iface.setSingleLine(true); panel.addView(iface);
        EditText ip=new EditText(this); ip.setHint("Unused phone Ethernet IP, e.g. 169.254.250.10"); ip.setSingleLine(true); ip.setText(source.getText()); panel.addView(ip);
        java.util.List<String> candidates=EthernetRoute.interfaceCandidates(this); if(candidates.size()==1) iface.setText(candidates.get(0));
        TextView note=new TextView(this); note.setText("Detected wired candidates: "+candidates+"\nUse only on your rooted phone. Verify the actual wired interface name and choose an unused phone address. Rebooted/partially configured states require review. Changes are temporary. Your existing root manager must approve su."); panel.addView(note);
        new AlertDialog.Builder(this).setTitle(remove?"Remove recorded network setup":"Preview rooted Ethernet setup").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Preview",(d,w)->{
            try {
                String name=iface.getText().toString().trim(), address=ip.getText().toString().trim();
                String preview=RootEthernet.preview(name,address,remove);
                new AlertDialog.Builder(this).setTitle("Confirm phone networking change").setMessage(preview).setNegativeButton("Cancel",null).setPositiveButton("Apply using root",(d2,w2)->{
                    configuring=true; status.setText("Waiting for root network setup…");
                    background.execute(()->{ String message;
                        try { message=RootEthernet.apply(this,name,address,remove); }
                        catch(Exception e) { message="Network setup stopped: "+e.getMessage(); }
                        String finalMessage=message; handler.post(()->{ configuring=false; if(!remove) source.setText(address); status.setText(finalMessage); });
                    });
                }).show();
            } catch(Exception e) { status.setText(e.getMessage()); }
        }).show();
    }
    private void zipTree(ZipOutputStream zip,File root,File dir) throws IOException {
        File[] files=dir.listFiles(); if(files==null) return;
        for(File file:files) {
            if(file.isDirectory()) zipTree(zip,root,file);
            else { zip.putNextEntry(new ZipEntry(root.toURI().relativize(file.toURI()).getPath())); try(InputStream in=new FileInputStream(file)) { byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1) zip.write(b,0,n); } zip.closeEntry(); }
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data); if(result!=RESULT_OK || data==null) return;
        if(request==42) {
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())) { if(out==null) throw new IOException("Destination unavailable"); out.write(logs.getBytes(StandardCharsets.UTF_8)); }
            catch(Exception e) { status.setText(e.getMessage()); }
        }
        if(request==44 && !BackendService.running && !configuring) {
            configuring=true;
            background.execute(()->{
                String message="Private backup saved; protect its keys.";
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())) {
                    if(out==null) throw new IOException("Destination unavailable");
                    try(ZipOutputStream zip=new ZipOutputStream(out)) { zipTree(zip,getFilesDir(),getFilesDir()); }
                } catch(Exception e) { message="Backup failed: "+e.getMessage(); }
                String text=message; handler.post(()->{ configuring=false; status.setText(text); });
            });
        }
    }
    @Override protected void onResume() { super.onResume(); if(!settingsSignature.equals(AppSettings.signature(this))){recreate();return;}handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh);AppSettings.prefs(this).edit().putString(profile+"_source",source.getText().toString()).putString(profile+"_gateway",gateway.getText().toString()).apply(); super.onPause(); }
    @Override protected void onDestroy() { handler.removeCallbacks(refresh); background.shutdown(); super.onDestroy(); }
    @Override public void onBackPressed() {
        if(BackendService.running) new AlertDialog.Builder(this).setTitle("Vehicle operation is running").setMessage("Leaving this screen does not cancel the service. Keep the app alive, phone power stable and ENET connected. Never force-stop to cancel flashing.").setNegativeButton("Stay",null).setPositiveButton("Leave screen",(d,w)->finish()).show();
        else if(selectedTab!=0) selectTab(0);
        else if(selectedStep>0) selectStep(selectedStep-1);
        else super.onBackPressed();
    }
}

