package app.foyer.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public final class HomeButtonService extends AccessibilityService {
    private final HomeRedirect redirect = new HomeRedirect();

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        String pkg = event.getPackageName() == null ? null : event.getPackageName().toString();
        String activity = event.getClassName() == null ? null : event.getClassName().toString();
        if (!redirect.shouldRedirect(pkg, activity, event.getEventTime(), SystemClock.uptimeMillis())) return;
        Intent home = new Intent(this, MainActivity.class).setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try { startActivity(home); }
        catch (RuntimeException error) { Log.w("FoyerHome", "Home redirect could not open Foyer", error); }
    }

    @Override public void onInterrupt() {}
}
