package dev.mgu.standalone;

import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;

/** Emulator UI regression checks only. No root helper or vehicle action is authorized. */
final class DesignChecks {
    private static EditText field(MainActivity activity,String name) throws Exception { Field f=MainActivity.class.getDeclaredField(name); f.setAccessible(true); return (EditText)f.get(activity); }
    static void run(Instrumentation test) throws Exception {
        MainActivity activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        File previews=new File(test.getTargetContext().getExternalFilesDir(null),"design-previews"); if(!previews.exists()) previews.mkdirs();
        try {
            // Temporary test-only screenshot access. Production onCreate always sets FLAG_SECURE.
            test.runOnMainSync(()->activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE));
            String[] names={"setup","monitor","backups","ssh"};
            for(int i=0;i<4;i++) {
                final int tab=i; test.runOnMainSync(()->{ activity.selectTab(tab); if(tab==0) activity.selectStep(0); }); test.waitForIdleSync(); Thread.sleep(250);
                Bitmap image=test.getUiAutomation().takeScreenshot(); if(image==null) throw new AssertionError("Could not capture tab "+tab);
                try(OutputStream out=new FileOutputStream(new File(previews,names[i]+".png"))) { image.compress(Bitmap.CompressFormat.PNG,100,out); } image.recycle();
            }
            EditText source=field(activity,"source"),vin=field(activity,"vin");
            Field af=MainActivity.class.getDeclaredField("arch"); af.setAccessible(true); Spinner variant=(Spinner)af.get(activity);
            test.runOnMainSync(()->{ source.setText("169.254.250.10"); vin.setText("WBA00000000000000"); variant.setSelection(1); activity.selectTab(2); activity.selectTab(3); activity.selectTab(0); activity.selectStep(1); });
            test.waitForIdleSync();
            if(!source.getText().toString().equals("169.254.250.10") || !vin.getText().toString().equals("WBA00000000000000") || variant.getSelectedItemPosition()!=1) throw new AssertionError("Form data did not survive tab changes");
            test.runOnMainSync(()->{
                activity.selectStep(2); ArrayList<View> matches=new ArrayList<>(); activity.getWindow().getDecorView().findViewsWithText(matches,"Flash + Enable SSH",View.FIND_VIEWS_WITH_TEXT);
                for(View view:matches) if(view instanceof Button && ((Button)view).getText().toString().equals("Flash + Enable SSH")) { view.performClick(); return; }
                throw new AssertionError("Flash action missing");
            });
            test.waitForIdleSync(); Thread.sleep(200);
            AccessibilityNodeInfo dialog=test.getUiAutomation().getRootInActiveWindow();
            if(dialog==null || dialog.findAccessibilityNodeInfosByText("This reprograms the head unit").isEmpty() || dialog.findAccessibilityNodeInfosByText("I AUTHORIZE WBA00000000000000").isEmpty()) throw new AssertionError("Required flash warning/authorization dialog missing");
            java.util.List<AccessibilityNodeInfo> starts=dialog.findAccessibilityNodeInfosByText("Start");
            boolean pressed=false; for(AccessibilityNodeInfo node:starts) if(node.getText()!=null && "Start".equalsIgnoreCase(node.getText().toString())) { pressed=node.performAction(AccessibilityNodeInfo.ACTION_CLICK); break; }
            if(!pressed) throw new AssertionError("Authorization button not accessible; matches: "+starts);
            test.waitForIdleSync(); Thread.sleep(150);
            if(BackendService.running || new File(test.getTargetContext().getFilesDir(),"last-job.json").exists()) throw new AssertionError("Empty flash authorization started a vehicle job");
        } finally { test.runOnMainSync(()->activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE)); }
    }
}
