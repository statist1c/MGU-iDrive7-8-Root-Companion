// Adapted from the user’s MGU Store Installer project (GPL-3.0).
// See licenses/MGU-Store-Installer-GPL-3.0.txt and THIRD-PARTY.md.
package dev.mgu.standalone;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict SemVer precedence; build metadata has no effect on ordering. */
final class ReleaseVersion implements Comparable<ReleaseVersion> {
    private static final String NUMBER = "(0|[1-9][0-9]*)";
    private static final Pattern FORMAT = Pattern.compile("^v?" + NUMBER + "\\." + NUMBER + "\\." + NUMBER
            + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$");
    final BigInteger[] core;
    final String[] pre;

    private ReleaseVersion(Matcher m) {
        core = new BigInteger[]{new BigInteger(m.group(1)), new BigInteger(m.group(2)), new BigInteger(m.group(3))};
        pre = m.group(4) == null ? new String[0] : m.group(4).split("\\.");
        for (String part : pre) {
            if (part.matches("[0-9]+") && part.length() > 1 && part.startsWith("0")) {
                throw new IllegalArgumentException("Invalid numeric prerelease identifier");
            }
        }
    }

    static ReleaseVersion parse(String text) {
        Matcher m = FORMAT.matcher(text == null ? "" : text);
        if (!m.matches()) throw new IllegalArgumentException("Expected a release tag such as v0.9.0");
        return new ReleaseVersion(m);
    }

    @Override public int compareTo(ReleaseVersion other) {
        for (int i = 0; i < 3; i++) {
            int result = core[i].compareTo(other.core[i]);
            if (result != 0) return result;
        }
        if (pre.length == 0 || other.pre.length == 0) {
            return pre.length == other.pre.length ? 0 : pre.length == 0 ? 1 : -1;
        }
        for (int i = 0; i < Math.min(pre.length, other.pre.length); i++) {
            String a = pre[i], b = other.pre[i];
            boolean an = a.matches("[0-9]+"), bn = b.matches("[0-9]+");
            int result = an && bn ? new BigInteger(a).compareTo(new BigInteger(b))
                    : an != bn ? (an ? -1 : 1) : a.compareTo(b);
            if (result != 0) return result;
        }
        return Integer.compare(pre.length, other.pre.length);
    }
}
