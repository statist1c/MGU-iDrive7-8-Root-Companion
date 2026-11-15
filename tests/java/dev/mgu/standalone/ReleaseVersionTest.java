package dev.mgu.standalone;

/** Run with the JDK alone; this does not build the Android app. */
public final class ReleaseVersionTest {
    private static void order(String older, String newer) {
        if (ReleaseVersion.parse(older).compareTo(ReleaseVersion.parse(newer)) >= 0)
            throw new AssertionError(older + " must sort before " + newer);
    }
    public static void main(String[] args) {
        order("v0.1.0-preview", "0.1.0");
        order("0.1.9", "0.1.10");
        order("0.9.99", "1.0.0");
        order("1.0.0-alpha", "1.0.0-alpha.1");
        order("1.0.0-alpha.2", "1.0.0-alpha.10");
        order("1.0.0-9", "1.0.0-alpha");
        order("1.0.0-rc.1", "1.0.0");
        order("999999999999999999999.0.0", "1000000000000000000000.0.0");
        if (ReleaseVersion.parse("v1.2.3+one").compareTo(ReleaseVersion.parse("1.2.3+two")) != 0)
            throw new AssertionError("Build metadata must not affect precedence");
        for (String invalid : new String[]{"", "v1.2", "1.02.0", "1.0.0-01", "main", "1.0.0-", "1.0.0+"}) {
            try { ReleaseVersion.parse(invalid); throw new AssertionError("Accepted " + invalid); }
            catch (IllegalArgumentException expected) { }
        }
        System.out.println("PASS: 16 semantic-version ordering and validation cases");
    }
}
