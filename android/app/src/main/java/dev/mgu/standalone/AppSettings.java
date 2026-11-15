// SPDX-License-Identifier: GPL-3.0-only
package dev.mgu.standalone;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
final class AppSettings {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("appearance-network",0);}
 static boolean dark(Context c){String t=prefs(c).getString("theme","auto");return t.equals("dark") || (t.equals("auto") && (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);}
 static String mode(Context c){return prefs(c).getString("transport","ethernet");}
 static String signature(Context c){return dark(c)+":"+mode(c)+":"+prefs(c).getString("ssh_override","");}
}
