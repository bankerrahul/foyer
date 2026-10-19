package app.foyer.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

// Tiny white outline icons on a 24x24 grid, so the app needs no image resources.
// Each icon is a list of ops: 0=moveTo x y, 1=lineTo x y, 2=close, 3=circle cx cy r.
final class LineIcon extends Drawable {
    static final int[] APPS = {0, 40, 40, 1, 100, 40, 1, 100, 100, 1, 40, 100, 2,
            0, 140, 40, 1, 200, 40, 1, 200, 100, 1, 140, 100, 2,
            0, 40, 140, 1, 100, 140, 1, 100, 200, 1, 40, 200, 2,
            0, 140, 140, 1, 200, 140, 1, 200, 200, 1, 140, 200, 2};
    static final int[] BACK = {0, 200, 120, 1, 40, 120, 0, 110, 50, 1, 40, 120, 1, 110, 190};
    static final int[] PLAY = {0, 60, 45, 1, 60, 195, 1, 190, 120, 2};
    static final int[] HDMI = {0, 30, 90, 1, 210, 90, 1, 210, 130, 1, 185, 160, 1, 55, 160, 1, 30, 130, 2,
            0, 80, 120, 1, 160, 120};
    static final int[] SETTINGS = {0, 40, 70, 1, 140, 70, 0, 180, 70, 1, 200, 70, 0, 40, 170, 1, 60, 170,
            0, 100, 170, 1, 200, 170, 3, 160, 70, 20, 3, 80, 170, 20};

    static final int[] TV = {0, 30, 40, 1, 210, 40, 1, 210, 170, 1, 30, 170, 2,
            0, 120, 170, 1, 120, 210, 0, 75, 210, 1, 165, 210};
    static final int[] BOX = {0, 35, 65, 1, 205, 65, 1, 205, 180, 1, 35, 180, 2, 3, 170, 145, 4};
    static final int[] GAME = {0, 65, 65, 1, 175, 65, 1, 215, 180, 1, 175, 180, 1, 150, 145,
            1, 90, 145, 1, 65, 180, 1, 25, 180, 2, 0, 65, 100, 1, 65, 135,
            0, 48, 118, 1, 82, 118, 3, 170, 105, 3, 3, 185, 125, 3};

    static int[] forInput(String icon) {
        if ("tv".equals(icon)) return TV;
        if ("box".equals(icon)) return BOX;
        if ("game".equals(icon)) return GAME;
        return HDMI;
    }

    private final int[] ops;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    LineIcon(int[] ops) {
        this.ops = ops;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(0xEBFFFFFF);
    }

    @Override
    public void draw(Canvas c) {
        if (ops == null) return;
        float s = getBounds().width() / 240f; // ops are in tenths of the 24-unit grid
        paint.setStrokeWidth(16 * s);
        path.reset();
        for (int i = 0; i < ops.length; ) {
            switch (ops[i]) {
                case 0: path.moveTo(ops[i + 1] * s, ops[i + 2] * s); i += 3; break;
                case 1: path.lineTo(ops[i + 1] * s, ops[i + 2] * s); i += 3; break;
                case 2: path.close(); i += 1; break;
                default: path.addCircle(ops[i + 1] * s, ops[i + 2] * s, ops[i + 3] * s, Path.Direction.CW); i += 4;
            }
        }
        c.save();
        c.translate(getBounds().left, getBounds().top);
        c.drawPath(path, paint);
        c.restore();
    }

    @Override public void setAlpha(int a) { paint.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return 44; }
    @Override public int getIntrinsicHeight() { return 44; }
}
