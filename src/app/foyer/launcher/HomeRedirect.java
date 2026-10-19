package app.foyer.launcher;

final class HomeRedirect {
    private long lastEventTime = -1;

    boolean shouldRedirect(String pkg, String activity, long eventTime, long now) {
        if (!"com.google.android.apps.tv.launcherx".equals(pkg)
                || !"com.google.android.apps.tv.launcherx.home.HomeActivity".equals(activity)) return false;
        if (eventTime < 0 || eventTime > now || now - eventTime > 1000) return false;
        if (eventTime <= lastEventTime) return false;
        lastEventTime = eventTime;
        return true;
    }
}
