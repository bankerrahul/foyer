package app.foyer.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.text.TextUtils;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class LauncherData {
    static final String PLAY_STORE = "com.android.vending", TV_SETTINGS = "com.android.tv.settings";
    final Context context;
    final SharedPreferences prefs;

    LauncherData(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("config", 0);
    }

    static Intent launchIntent(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        return i != null ? i : pm.getLaunchIntentForPackage(pkg);
    }

    static String label(PackageManager pm, String pkg) {
        try { return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception e) { return pkg; }
    }

    List<String> allApps() {
        PackageManager pm = context.getPackageManager();
        List<String> result = new ArrayList<>(), homes = new ArrayList<>();
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo ri : pm.queryIntentActivities(home, 0)) homes.add(ri.activityInfo.packageName);
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (!pkg.equals(context.getPackageName()) && !homes.contains(pkg) && !result.contains(pkg)
                        && ri.activityInfo.enabled && ri.activityInfo.applicationInfo.enabled
                        && launchIntent(pm, pkg) != null) result.add(pkg);
            }
        }
        Collections.sort(result, (a, b) -> {
            int value = label(pm, a).compareToIgnoreCase(label(pm, b));
            return value == 0 ? a.compareTo(b) : value;
        });
        return result;
    }

    List<String> orderedApps() {
        List<String> apps = allApps();
        apps.remove(PLAY_STORE);
        apps.remove(TV_SETTINGS);
        String saved = prefs.getString("order", prefs.getString("apps", null));
        List<String> preferred = saved == null ? Arrays.asList(Apps.LIST) : Arrays.asList(saved.split("[,\\s]+"));
        return AppOrder.reconcile(apps, preferred);
    }

    Set<String> hiddenApps() { return new HashSet<>(prefs.getStringSet("hidden", Collections.emptySet())); }

    List<String> homeApps() {
        List<String> result = orderedApps();
        result.removeAll(hiddenApps());
        return result;
    }

    void saveOrder(List<String> order) { prefs.edit().putString("order", TextUtils.join(",", order)).apply(); }

    void setHidden(String pkg, boolean hidden) {
        Set<String> set = hiddenApps();
        SharedPreferences.Editor e = prefs.edit();
        if (hidden) {
            if (set.add(pkg)) e.putString("undo_pkg", pkg).putInt("undo_position", homeApps().indexOf(pkg));
        } else {
            set.remove(pkg);
            if (pkg.equals(prefs.getString("undo_pkg", ""))) e.remove("undo_pkg").remove("undo_position");
        }
        e.putStringSet("hidden", set).apply();
    }

    String undoPackage() {
        String pkg = prefs.getString("undo_pkg", "");
        return hiddenApps().contains(pkg) && orderedApps().contains(pkg) ? pkg : null;
    }

    void undoRemoval() {
        String pkg = undoPackage();
        if (pkg == null) return;
        int position = prefs.getInt("undo_position", 0);
        List<String> visible = homeApps();
        visible.add(Math.max(0, Math.min(position, visible.size())), pkg);
        Set<String> hidden = hiddenApps();
        hidden.remove(pkg);
        prefs.edit().putStringSet("hidden", hidden)
                .putString("order", TextUtils.join(",", AppOrder.reconcile(orderedApps(), visible)))
                .remove("undo_pkg").remove("undo_position").apply();
    }

    void moveApp(String pkg, int delta) {
        List<String> visible = homeApps();
        int index = visible.indexOf(pkg);
        if (index < 0) return;
        saveOrder(AppOrder.reconcile(orderedApps(), AppOrder.move(visible, pkg, index + delta)));
    }

    Appearance appearance() {
        return new Appearance(prefs.getString("tile_size", "auto"), prefs.getString("spacing", "normal"),
                prefs.getString("theme", "gradient"), prefs.getBoolean("labels", false),
                prefs.getBoolean("date", true), prefs.getBoolean("clock", true));
    }

    void appearancePreset(boolean minimal) {
        prefs.edit().putString("tile_size", minimal ? "large" : "auto")
                .putString("spacing", "normal").putString("theme", minimal ? "black" : "gradient")
                .putBoolean("labels", minimal).putBoolean("date", !minimal).putBoolean("clock", !minimal).apply();
    }

    List<TvInputInfo> inputs() {
        List<TvInputInfo> available = new ArrayList<>();
        TvInputManager manager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        if (manager == null) return available;
        for (TvInputInfo in : manager.getTvInputList()) {
            boolean hidden = android.os.Build.VERSION.SDK_INT >= 24 && in.isHidden(context);
            if (in.getType() == TvInputInfo.TYPE_HDMI && in.getParentId() == null && !hidden) available.add(in);
        }
        Collections.sort(available, (a, b) -> a.getId().compareTo(b.getId()));
        List<String> ids = new ArrayList<>();
        for (TvInputInfo in : available) ids.add(in.getId());
        List<String> ordered = AppOrder.reconcile(ids, inputOrder());
        List<TvInputInfo> result = new ArrayList<>();
        for (String id : ordered) for (TvInputInfo in : available) if (id.equals(in.getId())) result.add(in);
        return result;
    }

    List<TvInputInfo> homeInputs() {
        List<TvInputInfo> result = inputs();
        Set<String> hidden = prefs.getStringSet("hidden_inputs", Collections.emptySet());
        for (int i = result.size() - 1; i >= 0; i--) if (hidden.contains(result.get(i).getId())) result.remove(i);
        return result.size() > 4 ? new ArrayList<>(result.subList(0, 4)) : result;
    }

    private List<String> inputOrder() {
        List<String> ids = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString("input_order", "[]"));
            for (int i = 0; i < array.length(); i++) ids.add(array.getString(i));
        } catch (Exception ignored) {}
        return ids;
    }

    void moveInput(String id, int delta) {
        List<String> ids = new ArrayList<>();
        for (TvInputInfo in : inputs()) ids.add(in.getId());
        int index = ids.indexOf(id);
        if (index < 0) return;
        List<String> order = AppOrder.mergePresent(inputOrder(), AppOrder.move(ids, id, index + delta));
        prefs.edit().putString("input_order", new JSONArray(order).toString()).apply();
    }

    boolean inputVisible(String id) {
        return !prefs.getStringSet("hidden_inputs", Collections.emptySet()).contains(id);
    }

    void setInputVisible(String id, boolean visible) {
        Set<String> hidden = new HashSet<>(prefs.getStringSet("hidden_inputs", Collections.emptySet()));
        if (visible) hidden.remove(id); else hidden.add(id);
        prefs.edit().putStringSet("hidden_inputs", hidden).apply();
    }

    static String systemInputLabel(Context context, TvInputInfo in) {
        CharSequence custom = android.os.Build.VERSION.SDK_INT >= 24 ? in.loadCustomLabel(context) : null;
        if (custom != null && custom.length() > 0) return custom.toString();
        CharSequence label = in.loadLabel(context);
        return label != null && label.length() > 0 ? label.toString() : "HDMI";
    }

    String inputLabel(TvInputInfo in) {
        String custom = prefs.getString("input_name:" + in.getId(), "").trim();
        return custom.isEmpty() ? systemInputLabel(context, in) : custom;
    }

    void renameInput(String id, String name) {
        String trimmed = name.trim();
        prefs.edit().putString("input_name:" + id, trimmed.substring(0, Math.min(40, trimmed.length()))).apply();
    }

    String inputIcon(String id) {
        String icon = prefs.getString("input_icon:" + id, "hdmi");
        return Arrays.asList("hdmi", "tv", "box", "game").contains(icon) ? icon : "hdmi";
    }
}
