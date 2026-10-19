package app.foyer.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String PREFS = "config";
    private static final String PLAY_STORE = "com.android.vending";
    private static final String SETTINGS = "settings";
    private static final String FOYER_SETTINGS = "foyer_settings";
    private static final String ALL_APPS = "all_apps";
    private static final String BACK_HOME = "back_home";
    private static final String INPUTS = "inputs";
    private static final String VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS";
    private int columns = 6;
    private LauncherData data;
    private Appearance appearance;
    private TextClock dateView, timeView;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferencesChanged = (prefs, key) -> dirty = true;

    // Layout is designed at 1920x1080 and scaled to the TV's real UI size by px().
    private static final int RADIUS = 42;
    private float scale = 1f;

    private static Typeface regular, semibold;

    private FrameLayout root;
    private final List<View> tiles = new ArrayList<>();
    private boolean dirty = true;
    private String focusedKey;
    private ScrollView scroll;
    private FrameLayout grid;
    private FrameLayout utilities;
    private TextView hint, pageTitle;
    private boolean showingAllApps;
    private String homeFocus;
    private int homeScroll;
    private List<String> displayedApps = new ArrayList<>();
    private List<String> appOrder = new ArrayList<>();
    private List<String> beforeMove;
    private String movingKey;
    private boolean selecting, held;
    private View selectTarget;
    private final Runnable hold = () -> {
        if (selecting && selectTarget != null && displayedApps.contains(selectTarget.getTag())) {
            held = true;
            String key = (String) selectTarget.getTag();
            selecting = false;
            showAppActions(key);
        }
    };

    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            dirty = true;
            if (hasWindowFocus() && movingKey == null) refreshApps();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        data = new LauncherData(this);
        data.prefs.registerOnSharedPreferenceChangeListener(preferencesChanged);
        appearance = data.appearance();
        saveConfig(getIntent());
        loadFonts();
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        getWindow().setBackgroundDrawable(background());

        root = new FrameLayout(this);
        root.setClipChildren(false);

        LinearLayout clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        dateView = clock("EEEE, d MMMM", "EEEE, d MMMM", px(28), 0x99FFFFFF, regular);
        clock.addView(dateView);
        root.addView(clock, at(px(120), px(68), -2, -2));
        timeView = clock("h:mm", "H:mm", px(28), 0xCCFFFFFF, semibold);
        timeView.setGravity(Gravity.RIGHT);
        timeView.setFontFeatureSettings("tnum");
        root.addView(timeView, at(px(1600), px(68), px(200), -2));

        pageTitle = text(px(30), Color.WHITE, semibold);
        pageTitle.setGravity(Gravity.CENTER);
        root.addView(pageTitle, at(px(720), px(68), px(480), -2));
        scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(px(24), px(20), px(24), px(12));
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        grid = new FrameLayout(this);
        grid.setClipChildren(false);
        scroll.addView(grid, new ScrollView.LayoutParams(-1, -1));
        root.addView(scroll, at(0, px(156), -1, px(724)));
        utilities = new FrameLayout(this);
        utilities.setClipChildren(false);
        root.addView(utilities, at(0, px(900), -1, px(170)));
        hint = text(px(22), 0x99FFFFFF, regular);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint, at(0, px(1044), -1, -2));
        setContentView(root);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(packagesChanged, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(packagesChanged, f);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packagesChanged);
        data.prefs.unregisterOnSharedPreferenceChangeListener(preferencesChanged);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            cancelMove();
            if (showingAllApps) closeAllApps();
        }
        if (saveConfig(intent)) { cancelMove(); refreshApps(); }
    }

    // Saves an app list passed with --es apps "...". Returns true if it changed.
    private boolean saveConfig(Intent intent) {
        String apps = intent == null ? null : intent.getStringExtra("apps");
        if (apps == null) return false;
        intent.removeExtra("apps");
        apps = apps.trim();
        SharedPreferences.Editor e = getSharedPreferences(PREFS, 0).edit();
        e.remove("order");
        if (apps.isEmpty() || apps.equalsIgnoreCase("auto")) e.remove("apps");
        else e.putString("apps", apps);
        e.apply();
        dirty = true;
        return true;
    }

    private List<String> discoverApps() { return data.allApps(); }

    private void refreshApps() {
        dirty = false;
        appearance = data.appearance();
        getWindow().setBackgroundDrawable(background());
        dateView.setVisibility(appearance.date ? View.VISIBLE : View.INVISIBLE);
        timeView.setVisibility(appearance.clock ? View.VISIBLE : View.INVISIBLE);
        appOrder = data.homeApps();
        buildTiles();
    }

    @Override protected void onResume() {
        super.onResume();
        if (dirty) refreshApps();
        if (getCurrentFocus() == null && !tiles.isEmpty()) tiles.get(0).requestFocus();
    }

    @Override protected void onPause() {
        root.removeCallbacks(hold);
        selecting = false;
        cancelMove();
        super.onPause();
    }

    private void beginMove(String key) {
        if (movingKey != null) return;
        beforeMove = new ArrayList<>(appOrder);
        movingKey = key;
        focusedKey = key;
        buildTiles();
    }

    private void finishMove() {
        getSharedPreferences(PREFS, 0).edit().putString("order", android.text.TextUtils.join(",", appOrder)).apply();
        movingKey = null;
        beforeMove = null;
        if (dirty) refreshApps(); else buildTiles();
    }

    private void cancelMove() {
        if (movingKey == null) return;
        appOrder = beforeMove;
        movingKey = null;
        beforeMove = null;
        if (dirty) refreshApps(); else buildTiles();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (ok) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) {
                    selecting = true;
                    held = false;
                    selectTarget = getCurrentFocus();
                    if (movingKey == null) root.postDelayed(hold, ViewConfiguration.getLongPressTimeout());
                } else if (event.isLongPress() && movingKey == null) hold.run();
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                root.removeCallbacks(hold);
                if (selecting && !held && !event.isCanceled()) {
                    if (movingKey != null) finishMove();
                    else if (selectTarget != null) selectTarget.performClick();
                }
                selecting = false;
                selectTarget = null;
            }
            return true;
        }
        if (movingKey != null) {
            if (code == KeyEvent.KEYCODE_BACK) {
                if (event.getAction() == KeyEvent.ACTION_UP) cancelMove();
                return true;
            }
            int index = appOrder.indexOf(movingKey), target = index;
            if (code == KeyEvent.KEYCODE_DPAD_LEFT) target--;
            else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) target++;
            else if (code == KeyEvent.KEYCODE_DPAD_UP) target -= columns;
            else if (code == KeyEvent.KEYCODE_DPAD_DOWN) target += columns;
            else return super.dispatchKeyEvent(event);
            if (event.getAction() == KeyEvent.ACTION_DOWN && target >= 0 && target < appOrder.size()) {
                appOrder = AppOrder.move(appOrder, movingKey, target);
                buildTiles();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public void onBackPressed() {
        if (movingKey != null) cancelMove();
        else if (showingAllApps) closeAllApps();
        else if (!tiles.isEmpty()) {
            tiles.get(0).requestFocus();
            scroll.scrollTo(0, 0);
        }
    }

    private void showAppActions(String pkg) {
        root.removeCallbacks(hold);
        PackageManager pm = getPackageManager();
        boolean utility = PLAY_STORE.equals(pkg) || "com.android.tv.settings".equals(pkg);
        boolean hidden = getSharedPreferences(PREFS, 0).getStringSet("hidden", Collections.emptySet()).contains(pkg);
        String[] actions = showingAllApps
                ? (utility ? new String[] {"Open"} : new String[] {"Open", hidden ? "Add to Home" : "Remove from Home"})
                : new String[] {"Move", "Remove from Home"};
        new AlertDialog.Builder(this).setTitle(appLabel(pm, pkg))
                .setItems(actions, (dialog, which) -> {
                    if (showingAllApps && which == 0) open(pkg);
                    else if (!showingAllApps && which == 0) beginMove(pkg);
                    else setHidden(pkg, !hidden);
                }).setNegativeButton("Cancel", null).show();
    }

    private void setHidden(String pkg, boolean hidden) {
        data.setHidden(pkg, hidden);
        if (!showingAllApps && hidden) {
            int index = appOrder.indexOf(pkg);
            focusedKey = appOrder.size() > 1 ? appOrder.get(index == appOrder.size() - 1 ? index - 1 : index + 1) : ALL_APPS;
        }
        refreshApps();
    }

    // A static background avoids keeping a video or image service running.
    private Drawable background() {
        if (appearance.theme.equals("black")) return new android.graphics.drawable.ColorDrawable(Color.BLACK);
        if (appearance.theme.equals("charcoal")) return new android.graphics.drawable.ColorDrawable(0xFF17191D);
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xFF111822, 0xFF0B1019, 0xFF05070D});
        GradientDrawable blue = radial(0x282D67B5, 0.18f, 0.18f, px(1200));
        GradientDrawable violet = radial(0x1F7355A6, 0.88f, 0.70f, px(1050));
        for (GradientDrawable g : new GradientDrawable[] {base, blue, violet}) g.setDither(true);
        return new LayerDrawable(new Drawable[] {base, blue, violet});
    }

    private static GradientDrawable radial(int color, float cx, float cy, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setColors(new int[] {color, color & 0x00FFFFFF});
        g.setGradientCenter(cx, cy);
        g.setGradientRadius(r);
        return g;
    }

    private void buildTiles() {
        int previousScroll = scroll.getScrollY();
        grid.removeAllViews();
        utilities.removeAllViews();
        tiles.clear();
        PackageManager pm = getPackageManager();
        displayedApps = showingAllApps ? discoverApps() : appOrder;
        pageTitle.setText(showingAllApps ? "All apps" : "");
        int width = getResources().getDisplayMetrics().widthPixels;
        int countApps = displayedApps.size();
        columns = appearance.columns(countApps);
        int w = px(appearance.tileSize(countApps)), gap = px(appearance.gap(countApps));
        int stride = px(appearance.stride(countApps));
        int rows = (countApps + columns - 1) / columns;
        int offset = rows > 1 ? px(10) : px(170);
        grid.setMinimumHeight(offset + Math.max(0, rows - 1) * stride + w + px(48));
        for (int n = 0; n < displayedApps.size(); n++) {
            String pkg = displayedApps.get(n);
            int rowCount = Math.min(columns, displayedApps.size() - (n / columns) * columns);
            int left = (width - px(48) - rowCount * w - Math.max(0, rowCount - 1) * gap) / 2;
            Drawable icon = null;
            try { icon = pm.getApplicationIcon(pkg); } catch (Exception ignored) {}
            addTile(grid, pkg, appLabel(pm, pkg), null, null, icon,
                    left + (n % columns) * (w + gap), offset + (n / columns) * stride, w, w);
        }
        if (displayedApps.isEmpty()) {
            TextView empty = text(px(28), Color.WHITE, regular);
            empty.setText("Add apps from All apps");
            empty.setGravity(Gravity.CENTER);
            grid.addView(empty, at(0, px(220), -1, -2));
        }
        int utilitySize = px(132), utilityGap = px(44);
        if (showingAllApps) {
            addTile(utilities, BACK_HOME, "Back", null, LineIcon.BACK, null,
                    (width - utilitySize) / 2, 0, utilitySize, utilitySize);
        } else {
            List<TvInputInfo> inputs = hdmiInputs();
            boolean picker = data.inputs().isEmpty() && inputPicker() != null;
            int count = inputs.size() + 3 + (launchIntent(pm, PLAY_STORE) != null ? 1 : 0) + (picker ? 1 : 0);
            int left = (width - count * utilitySize - (count - 1) * utilityGap) / 2;
            int col = 0;
            addTile(utilities, ALL_APPS, "All apps", null, LineIcon.APPS, null, left + col++ * (utilitySize + utilityGap), 0, utilitySize, utilitySize);
            if (launchIntent(pm, PLAY_STORE) != null)
                addTile(utilities, PLAY_STORE, "Play Store", null, LineIcon.PLAY, null, left + col++ * (utilitySize + utilityGap), 0, utilitySize, utilitySize);
            for (TvInputInfo in : inputs)
                addTile(utilities, in.getId(), inputLabel(in), null, LineIcon.forInput(data.inputIcon(in.getId())), null, left + col++ * (utilitySize + utilityGap), 0, utilitySize, utilitySize);
            if (picker)
                addTile(utilities, INPUTS, "Inputs", null, LineIcon.HDMI, null, left + col++ * (utilitySize + utilityGap), 0, utilitySize, utilitySize);
            addTile(utilities, FOYER_SETTINGS, "Foyer settings", null, LineIcon.SETTINGS, null, left + col++ * (utilitySize + utilityGap), 0, utilitySize, utilitySize);
            addTile(utilities, SETTINGS, "Settings", null, LineIcon.SETTINGS, null, left + col * (utilitySize + utilityGap), 0, utilitySize, utilitySize);
        }
        hint.setText(movingKey == null ? "" : "Arrows to move  ·  OK to save  ·  Back to cancel");
        scroll.scrollTo(0, previousScroll);
        for (View tile : tiles) {
            if (tile.getTag().equals(focusedKey)) {
                tile.requestFocus();
                if (movingKey != null) tile.post(() -> {
                    View wrap = (View) tile.getParent();
                    scroll.smoothScrollTo(0, Math.max(0, wrap.getTop() - px(28)));
                });
                break;
            }
        }
        if (getCurrentFocus() == null && !tiles.isEmpty()) tiles.get(0).requestFocus();
    }

    private void addTile(FrameLayout parent, final String key, String label, Drawable banner, int[] lineIcon, Drawable appIcon,
                         int x, int y, int w, int h) {
        FrameLayout card = new FrameLayout(this);
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(RADIUS));
        if (banner != null || appIcon != null) {
            bg.setColor(0xFF1E1A1C);
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(fit(banner != null ? banner : appIcon, w, h));
            card.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            // Quiet "glass" tile: icon above label.
            bg.setColor(0x13FFFFFF);
            bg.setStroke(Math.max(1, px(2)), 0x17FFFFFF);
            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER);
            ImageView iv = new ImageView(this);
            int size = lineIcon != null ? px(38) : px(170);
            if (lineIcon != null) iv.setImageDrawable(new LineIcon(lineIcon));
            else if (appIcon != null) iv.setImageDrawable(fit(appIcon, size, size));
            inner.addView(iv, new LinearLayout.LayoutParams(size, size));
            TextView t = text(px(19), 0xCCFFFFFF, regular);
            t.setText(FOYER_SETTINGS.equals(key) ? "Customize" : label);
            t.setSingleLine(true);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            t.setGravity(Gravity.CENTER);
            t.setPadding(px(16), 0, px(16), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = px(12);
            if (lineIcon != null) inner.addView(t, lp);
            card.addView(inner, new FrameLayout.LayoutParams(-1, -1));
        }
        card.setBackground(bg);
        card.setClipToOutline(true);
        card.setElevation(px(12));
        card.setFocusable(true);
        card.setTag(key);
        card.setContentDescription(label);
        final GradientDrawable focusRing = new GradientDrawable();
        focusRing.setCornerRadius(px(RADIUS));
        focusRing.setColor(Color.TRANSPARENT);
        card.setSelected(key.equals(movingKey));
        card.setForeground(focusRing);

        final TextView caption = lineIcon != null ? null : text(px(24), Color.WHITE, regular);
        if (caption != null) {
            caption.setText(label);
            caption.setGravity(Gravity.CENTER_HORIZONTAL);
            caption.setSingleLine(true);
            caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
            caption.setVisibility(showingAllApps || appearance.labels ? View.VISIBLE : View.INVISIBLE);
        }

        final boolean glass = banner == null && appIcon == null;
        card.setOnFocusChangeListener((v, has) -> {
            if (has) focusedKey = key;
            if (glass) bg.setColor(has ? 0x38FFFFFF : 0x13FFFFFF);
            focusRing.setStroke(Math.max(1, px(3)), key.equals(movingKey) ? 0xFF75BFFF : (has ? 0xD0FFFFFF : Color.TRANSPARENT));
            v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).translationZ(has ? px(26) : 0)
                    .setDuration(160).withLayer().start();
            if (caption != null) caption.setVisibility(has || showingAllApps || appearance.labels ? View.VISIBLE : View.INVISIBLE);
        });
        card.setOnClickListener(v -> open(key));
        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        wrap.addView(card, new FrameLayout.LayoutParams(w, h));
        if (caption != null) {
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(w, -2);
            cp.topMargin = h + px(14);
            wrap.addView(caption, cp);
        }
        parent.addView(wrap, at(x, y, w, h + px(48)));
        tiles.add(card);
    }

    private void open(String key) {
        Intent i;
        if (BACK_HOME.equals(key)) {
            closeAllApps();
            return;
        } else if (INPUTS.equals(key)) {
            i = inputPicker();
            if (i == null) return;
        } else if (ALL_APPS.equals(key)) {
            showAllApps();
            return;
        } else if (FOYER_SETTINGS.equals(key)) {
            i = new Intent(this, SettingsActivity.class);
        } else if (SETTINGS.equals(key)) {
            i = new Intent(Settings.ACTION_SETTINGS);
        } else if (isInput(key)) {
            i = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(key));
            // Prefer the stock Live TV app when it's there; otherwise let the TV pick.
            Intent live = new Intent(i).setClassName("com.android.tv", "com.android.tv.MainActivity");
            if (live.resolveActivity(getPackageManager()) != null) i = live;
        } else {
            i = launchIntent(getPackageManager(), key);
            if (i == null) return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception e) { Toast.makeText(this, "Unable to open this item", Toast.LENGTH_SHORT).show(); }
    }

    private void showAllApps() {
        homeFocus = focusedKey;
        homeScroll = scroll.getScrollY();
        showingAllApps = true;
        focusedKey = null;
        scroll.scrollTo(0, 0);
        buildTiles();
        if (!tiles.isEmpty()) tiles.get(0).requestFocus();
    }

    private void closeAllApps() {
        showingAllApps = false;
        focusedKey = homeFocus;
        scroll.scrollTo(0, homeScroll);
        buildTiles();
    }

    private Intent inputPicker() {
        Intent query = new Intent(VIEW_INPUTS);
        ResolveInfo ri = getPackageManager().resolveActivity(query, PackageManager.MATCH_DEFAULT_ONLY);
        if (ri == null || !ri.activityInfo.exported) return null;
        String permission = ri.activityInfo.permission;
        if (permission != null && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) return null;
        return query.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
    }

    private static String appLabel(PackageManager pm, String pkg) {
        try { return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception e) { return pkg; }
    }

    private static boolean isInput(String key) {
        return key.indexOf('/') > 0;
    }

    private List<TvInputInfo> hdmiInputs() { return data.homeInputs(); }

    private String inputLabel(TvInputInfo in) { return data.inputLabel(in); }

    private static Intent launchIntent(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        return i != null ? i : pm.getLaunchIntentForPackage(pkg);
    }

    // Xiaomi TVs ship MiLan Pro but don't register it by name; every other TV gets Roboto.
    private static void loadFonts() {
        if (regular != null) return;
        regular = fromFile("/system/fonts/MILanPro_Normal.ttf", Typeface.create("sans-serif", Typeface.NORMAL));
        semibold = fromFile("/system/fonts/MILanPro_SemiBold.ttf", Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    private static Typeface fromFile(String path, Typeface fallback) {
        try { return new File(path).exists() ? Typeface.createFromFile(path) : fallback; }
        catch (Exception e) { return fallback; }
    }

    // Draws a banner once into a tile-sized bitmap (center-cropped). Some apps ship huge banners
    // that Android 9 refuses to draw ("Canvas: trying to draw too large bitmap"); this also saves RAM.
    private Drawable fit(Drawable d, int w, int h) {
        try {
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            int iw = d.getIntrinsicWidth(), ih = d.getIntrinsicHeight();
            if (iw <= 0 || ih <= 0) { iw = w; ih = h; }
            float s = Math.max((float) w / iw, (float) h / ih);
            int dw = Math.round(iw * s), dh = Math.round(ih * s);
            d.setBounds((w - dw) / 2, (h - dh) / 2, (w - dw) / 2 + dw, (h - dh) / 2 + dh);
            d.draw(new Canvas(b));
            return new BitmapDrawable(getResources(), b);
        } catch (Throwable t) {
            return d;
        }
    }

    private int px(int v) {
        return Math.round(v * scale);
    }

    private TextClock clock(String f12, String f24, int px, int color, Typeface tf) {
        TextClock c = new TextClock(this);
        c.setFormat12Hour(f12);
        c.setFormat24Hour(f24);
        c.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        c.setTextColor(color);
        c.setTypeface(tf);
        return c;
    }

    private TextView text(int px, int color, Typeface tf) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTextColor(color);
        if (tf != null) t.setTypeface(tf);
        return t;
    }

    private static FrameLayout.LayoutParams at(int x, int y, int w, int h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.leftMargin = x;
        lp.topMargin = y;
        return lp;
    }
}
