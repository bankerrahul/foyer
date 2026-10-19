package app.foyer.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SettingsActivity extends Activity {
    private LauncherData data;
    private String page = "menu", inputId;
    private LinearLayout content;
    private ScrollView scroll;
    private TextView title, subtitle;
    private float scale;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        data = new LauncherData(this);
        if (state != null) {
            page = state.getString("page", "menu");
            inputId = state.getString("input");
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(px(120), px(56), px(120), px(32));
        root.setBackgroundColor(0xFF0B1019);
        title = text("Foyer settings", 38, Color.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        root.addView(title);
        subtitle = text("", 24, 0xFFB7BFCC);
        subtitle.setPadding(0, px(14), 0, px(30));
        root.addView(subtitle);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(4), px(4), px(4), px(16));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView back = button(root, "Back", "back", this::onBackPressed);
        LinearLayout.LayoutParams bp = (LinearLayout.LayoutParams) back.getLayoutParams();
        bp.width = px(200);
        setContentView(root);
        render(null, false);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("page", page);
        state.putString("input", inputId);
    }

    @Override protected void onResume() {
        super.onResume();
        View focused = getCurrentFocus();
        Object tag = focused == null ? null : focused.getTag();
        render(tag instanceof String ? (String) tag : null, true);
    }

    private void navigate(String target) {
        page = target;
        render(null, false);
    }

    @Override public void onBackPressed() {
        if (page.equals("input")) {
            page = "inputs";
            render("input:" + inputId, false);
        } else if (!page.equals("menu")) {
            String previous = page;
            page = "menu";
            render(previous, false);
        } else finish();
    }

    private void render(String focus, boolean keepScroll) {
        int position = keepScroll ? scroll.getScrollY() : 0;
        content.removeAllViews();
        title.setText(page.equals("menu") ? "Foyer settings" : page.equals("home") ? "Manage Home"
                : page.equals("appearance") ? "Appearance" : page.equals("inputs") ? "Inputs" : "Customize input");
        subtitle.setText(page.equals("home") ? "Choose your Home apps. Move them with the controls beside each app."
                : page.equals("appearance") ? "Make Foyer feel at home on your TV."
                : page.equals("inputs") || page.equals("input") ? "Changes apply to Foyer's shortcuts only."
                : "Your apps, your layout, your inputs.");
        if (page.equals("menu")) {
            button(content, "Manage Home", "home", () -> navigate("home"));
            note("Choose apps, change their order, or undo a removal.");
            button(content, "Appearance", "appearance", () -> navigate("appearance"));
            note("Tile size, spacing, labels, background, date and clock.");
            button(content, "Inputs", "inputs", () -> navigate("inputs"));
            note("Name, arrange and hide HDMI shortcuts.");
        } else if (page.equals("home")) renderHome();
        else if (page.equals("appearance")) renderAppearance();
        else if (page.equals("inputs")) renderInputs();
        else renderInput();
        View target = focus == null ? null : content.findViewWithTag(focus);
        if (target != null && !target.isEnabled() && focus.contains(":"))
            target = content.findViewWithTag("app:" + focus.substring(focus.indexOf(':') + 1));
        if (target == null || !target.isEnabled()) target = firstFocusable(content);
        if (target == null) target = getWindow().getDecorView().findViewWithTag("back");
        if (target != null) target.requestFocus();
        scroll.post(() -> {
            scroll.scrollTo(0, position);
            View focused = getCurrentFocus();
            if (focused != null && focused != getWindow().getDecorView().findViewWithTag("back")) {
                Rect bounds = new Rect();
                focused.getDrawingRect(bounds);
                focused.requestRectangleOnScreen(bounds, true);
            }
        });
    }

    private View firstFocusable(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View v = parent.getChildAt(i);
            if (v.isFocusable() && v.isEnabled()) return v;
            if (v instanceof ViewGroup) {
                View found = firstFocusable((ViewGroup) v);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void renderHome() {
        String undo = data.undoPackage();
        TextView undoButton = button(content, undo == null ? "Undo removal" : "Undo removal: " + LauncherData.label(getPackageManager(), undo),
                "undo", () -> { data.undoRemoval(); render("app:" + undo, true); });
        undoButton.setEnabled(undo != null);
        List<String> visible = data.homeApps();
        List<String> apps = new ArrayList<>(visible);
        for (String pkg : data.orderedApps()) if (!apps.contains(pkg)) apps.add(pkg);
        Set<String> hidden = data.hiddenApps();
        if (apps.isEmpty()) note("No launchable apps found. Install apps through the Play Store.");
        for (String pkg : apps) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, px(4), 0, px(4));
            ImageView icon = new ImageView(this);
            try { icon.setImageDrawable(getPackageManager().getApplicationIcon(pkg)); } catch (Exception ignored) {}
            row.addView(icon, new LinearLayout.LayoutParams(px(56), px(56)));
            CheckBox check = checkbox(row, LauncherData.label(getPackageManager(), pkg), "app:" + pkg,
                    !hidden.contains(pkg), checked -> { data.setHidden(pkg, !checked); render("app:" + pkg, true); });
            check.setContentDescription("Home app: " + LauncherData.label(getPackageManager(), pkg));
            check.setLayoutParams(new LinearLayout.LayoutParams(0, px(88), 1));
            int index = visible.indexOf(pkg);
            TextView up = button(row, "Move up", "up:" + pkg, () -> { data.moveApp(pkg, -1); render("up:" + pkg, true); });
            TextView down = button(row, "Move down", "down:" + pkg, () -> { data.moveApp(pkg, 1); render("down:" + pkg, true); });
            up.setContentDescription("Move up: " + LauncherData.label(getPackageManager(), pkg));
            down.setContentDescription("Move down: " + LauncherData.label(getPackageManager(), pkg));
            up.setEnabled(index > 0);
            down.setEnabled(index >= 0 && index < visible.size() - 1);
            content.addView(row);
        }
    }

    private interface Toggle { void changed(boolean checked); }

    private CheckBox checkbox(LinearLayout parent, String label, String tag, boolean checked, Toggle change) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(28));
        box.setTextColor(Color.WHITE);
        box.setButtonTintList(ColorStateList.valueOf(0xFFB9D9FF));
        box.setChecked(checked);
        box.setTag(tag);
        box.setPadding(px(22), 0, px(22), 0);
        box.setBackground(controlBackground());
        box.setOnCheckedChangeListener((v, value) -> change.changed(value));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(84));
        lp.bottomMargin = px(10);
        parent.addView(box, lp);
        return box;
    }

    private void renderAppearance() {
        Appearance a = data.appearance();
        choice("Tile size", "tile_size", new String[] {"Auto", "Small", "Medium", "Large"},
                new String[] {"auto", "small", "medium", "large"}, a.size);
        choice("Spacing", "spacing", new String[] {"Normal", "Compact", "Spacious"},
                new String[] {"normal", "compact", "spacious"}, a.spacing);
        choice("Background", "theme", new String[] {"Original gradient", "Charcoal", "Black"},
                new String[] {"gradient", "charcoal", "black"}, a.theme);
        checkbox(content, "Always show app names", "labels", a.labels, value -> data.prefs.edit().putBoolean("labels", value).apply());
        checkbox(content, "Show date", "date", a.date, value -> data.prefs.edit().putBoolean("date", value).apply());
        checkbox(content, "Show clock", "clock", a.clock, value -> data.prefs.edit().putBoolean("clock", value).apply());
        button(content, "Use Minimal preset", "minimal", () -> { data.appearancePreset(true); render("minimal", true); });
        button(content, "Restore appearance defaults", "defaults", () -> { data.appearancePreset(false); render("defaults", true); });
    }

    private void choice(String label, String key, String[] labels, String[] values, String current) {
        int selected = java.util.Arrays.asList(values).indexOf(current);
        button(content, label + ": " + labels[Math.max(0, selected)], key, () ->
            new AlertDialog.Builder(this).setTitle(label).setSingleChoiceItems(labels, selected, (dialog, which) -> {
                data.prefs.edit().putString(key, values[which]).apply();
                dialog.dismiss();
                render(key, true);
            }).setNegativeButton("Cancel", null).show());
    }

    private void renderInputs() {
        List<TvInputInfo> inputs = data.inputs();
        if (inputs.isEmpty()) note("This TV is not exposing HDMI shortcuts. Use the TV's input selector to choose a source.");
        else note("The first four shown inputs appear on Home. Select an input to customize it.");
        for (TvInputInfo in : inputs) {
            String id = in.getId();
            button(content, data.inputLabel(in) + (data.inputVisible(id) ? "  ·  Shown" : "  ·  Hidden"), "input:" + id,
                    () -> { inputId = id; navigate("input"); });
        }
    }

    private void renderInput() {
        List<TvInputInfo> inputs = data.inputs();
        TvInputInfo input = null;
        for (TvInputInfo in : inputs) if (in.getId().equals(inputId)) input = in;
        if (input == null) { note("This input is currently unavailable. Its customizations are saved."); return; }
        final TvInputInfo in = input;
        title.setText(data.inputLabel(in));
        note("TV name: " + LauncherData.systemInputLabel(this, in));
        button(content, "Rename", "rename", () -> rename(in));
        choice("Icon", "input_icon:" + inputId, new String[] {"HDMI", "TV", "Media box", "Game console"},
                new String[] {"hdmi", "tv", "box", "game"}, data.inputIcon(inputId));
        checkbox(content, "Show on Home", "input_visible", data.inputVisible(inputId), value -> data.setInputVisible(inputId, value));
        List<String> ids = new ArrayList<>();
        for (TvInputInfo item : inputs) ids.add(item.getId());
        int index = ids.indexOf(inputId);
        TextView up = button(content, "Move up", "input_up", () -> { data.moveInput(inputId, -1); render("input_up", true); });
        TextView down = button(content, "Move down", "input_down", () -> { data.moveInput(inputId, 1); render("input_down", true); });
        up.setEnabled(index > 0);
        down.setEnabled(index < inputs.size() - 1);
    }

    private void rename(TvInputInfo in) {
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setText(data.prefs.getString("input_name:" + inputId, ""));
        name.setHint(LauncherData.systemInputLabel(this, in));
        name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(40)});
        name.setSelectAllOnFocus(true);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(px(36), px(8), px(36), px(8));
        wrap.addView(name);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Rename input").setMessage("Leave blank to use the TV's name.")
                .setView(wrap).setPositiveButton("Save", (d, which) -> {
                    data.renameInput(in.getId(), name.getText().toString());
                    render("rename", true);
                }).setNegativeButton("Cancel", null).create();
        dialog.setOnShowListener(d -> {
            name.requestFocus();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        dialog.show();
    }

    private TextView button(LinearLayout parent, String label, String tag, Runnable action) {
        TextView view = text(label, 28, Color.WHITE);
        view.setTag(tag);
        view.setFocusable(true);
        view.setClickable(true);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(px(26), 0, px(26), 0);
        view.setBackground(controlBackground());
        view.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(parent == content ? -1 : px(220), px(78));
        lp.bottomMargin = px(12);
        lp.leftMargin = parent == content ? 0 : px(12);
        parent.addView(view, lp);
        return view;
    }

    private StateListDrawable controlBackground() {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {-android.R.attr.state_enabled}, fill(0xFF151B25, false));
        states.addState(new int[] {android.R.attr.state_focused}, fill(0xFF28394F, true));
        states.addState(new int[] {android.R.attr.state_pressed}, fill(0xFF28394F, true));
        states.addState(new int[] {}, fill(0xFF182231, false));
        return states;
    }

    private GradientDrawable fill(int color, boolean focused) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(px(18));
        if (focused) d.setStroke(px(3), 0xFFB9D9FF);
        return d;
    }

    private void note(String value) {
        TextView note = text(value, 24, 0xFFB7BFCC);
        note.setPadding(px(4), px(2), 0, px(24));
        content.addView(note);
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        t.setTextColor(color);
        return t;
    }

    private int px(int value) { return Math.round(value * scale); }
}
