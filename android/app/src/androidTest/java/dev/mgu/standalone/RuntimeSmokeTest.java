// SPDX-License-Identifier: GPL-3.0-only
package dev.mgu.standalone;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.jcraft.jsch.JSch;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;
import java.io.File;
import java.security.Security;

/** No network, root, diagnostic or flash execution occurs in this test. */
public final class RuntimeSmokeTest extends Instrumentation {
    private Bundle options;
    public static final class Callback {
        public String value="";
        public void onLine(String text) { value=text; }
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); options=args; start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            if(options!=null && "true".equals(options.getString("design"))) {
                DesignChecks.run(this);
                result.putString("stream","PASS: all four tabs, input retention, safety dialog and blocked empty authorization. No vehicle traffic.\n");
                finish(-1,result); return;
            }
            if(options!=null && "true".equals(options.getString("themes"))){ThemeChecks.run(this);result.putString("stream","PASS: light/dark/auto themes, Wi-Fi profile, settings and Java IPv4 policy. No vehicle traffic.\n");finish(-1,result);return;}
            if(options!=null && "true".equals(options.getString("updates"))){UpdateChecks.run(this);result.putString("stream","PASS: live GitHub metadata check, daily job enable/disable, vehicle-work guards, unverified APK rejection and updater Settings controls. No APK download/install or vehicle traffic.\n");finish(-1,result);return;}
            Context context=getTargetContext();
            File backend=BackendService.extractBackend(context);
            Security.removeProvider("BC"); Security.addProvider(new BouncyCastleProvider());
            JSch ssh=new JSch(); ssh.addIdentity(new File(backend,"data/assets/05_SSH_ROOT_KEY.pem").getPath());
            if(ssh.getIdentityNames().size()!=1) throw new AssertionError("Bundled SSH private key did not load");
            if(!Python.isStarted()) Python.start(new AndroidPlatform(context));
            Callback callback=new Callback();
            String report=Python.getInstance().getModule("runtime_smoke").callAttr("run",backend.getPath(),callback).toString();
            JSONObject parsed=new JSONObject(report);
            if(parsed.optBoolean("vehicle_traffic",true) || !callback.value.equals("Embedded Python-to-Java callback OK")) throw new AssertionError("Runtime callback/check failed");
            result.putString("stream","PASS: embedded runtime, crypto, SSH key parsing, backend assets and callback. No vehicle traffic.\n"+report+"\n");
            finish(-1,result);
        } catch(Throwable e) {
            result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e)); finish(0,result);
        }
    }
}
