package app.foyer.launcher;

public final class HomeRedirectTest {
    private static final String PACKAGE = "com.google.android.apps.tv.launcherx";
    private static final String HOME = PACKAGE + ".home.HomeActivity";
    private static void check(boolean actual, boolean expected) {
        if (actual != expected) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        HomeRedirect policy = new HomeRedirect();
        check(policy.shouldRedirect(null, HOME, 100, 100), false);
        check(policy.shouldRedirect(PACKAGE, null, 100, 100), false);
        check(policy.shouldRedirect("com.android.tv.settings", HOME, 100, 100), false);
        check(policy.shouldRedirect("com.google.android.tungsten.setupwraith", "com.google.android.tungsten.setupwraith.RecoveryActivity", 100, 100), false);
        check(policy.shouldRedirect("com.plexapp.android", HOME, 100, 100), false);
        check(policy.shouldRedirect(PACKAGE, PACKAGE + ".settings.SettingsActivity", 100, 100), false);
        check(policy.shouldRedirect(PACKAGE, "android.app.Dialog", 100, 100), false);
        check(policy.shouldRedirect(PACKAGE, HOME, -1, 100), false);
        check(policy.shouldRedirect(PACKAGE, HOME, 101, 100), false);
        check(policy.shouldRedirect(PACKAGE, HOME, 100, 1101), false);
        check(policy.shouldRedirect(PACKAGE, HOME, 100, 100), true);
        check(policy.shouldRedirect(PACKAGE, HOME, 100, 101), false);
        check(policy.shouldRedirect(PACKAGE, HOME, 101, 101), true);
        check(policy.shouldRedirect(PACKAGE, HOME, 101, 102), false);
        check(policy.shouldRedirect(PACKAGE, HOME, 499, 499), true);
        check(policy.shouldRedirect(PACKAGE, HOME, 500, 500), true);
        check(policy.shouldRedirect("app.foyer.launcher", "app.foyer.launcher.MainActivity", 600, 600), false);
        check(policy.shouldRedirect(PACKAGE, HOME, 1400, 1500), true);
        System.out.println("18 Home redirect cases passed");
    }
}
