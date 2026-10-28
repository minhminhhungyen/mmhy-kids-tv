package com.minhminhhungyen.kidstv;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/**
 * Tiện ích dựng giao diện bằng mã. Mọi kích thước tính theo "đơn vị thiết kế" trên màn hình 960×540
 * (không phụ thuộc mật độ điểm ảnh của TV), phóng đúng tỉ lệ lên màn hình thật.
 */
final class Ui {
    private Ui() {
    }

    static final int GREEN = 0xFF0B6B35;
    static final int GREEN_D = 0xFF0A4A27;
    static final int ORANGE = 0xFFF57C00;
    static final int INK = 0xFF123524;
    static final int MUTED = 0xFF5D7366;
    static final int RED = 0xFFD32F2F;
    static final int OK = 0xFF2E9D4F;
    static final int WHITE = 0xFFFFFFFF;
    static final int CARD = 0xF2FFFFFF;
    static final int TRACK = 0xFFDDE9E1;

    private static float scale = 1f;

    static void init(Context c) {
        // Kích thước thật của màn hình (không trừ thanh điều hướng): launcher chạy toàn màn hình, ẩn mọi thanh hệ thống.
        DisplayMetrics dm = new DisplayMetrics();
        try {
            android.view.WindowManager wm = (android.view.WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getRealMetrics(dm);
        } catch (Throwable t) {
            dm = c.getResources().getDisplayMetrics();
        }
        if (dm.widthPixels <= 0 || dm.heightPixels <= 0) {
            dm = c.getResources().getDisplayMetrics();
        }
        float w = Math.max(dm.widthPixels, dm.heightPixels);
        float h = Math.min(dm.widthPixels, dm.heightPixels);
        scale = Math.min(w / 960f, h / 540f);
    }

    static int u(float v) {
        return Math.round(v * scale);
    }

    static GradientDrawable rounded(int fill, float radiusU, int stroke, float strokeU) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(u(radiusU));
        if (strokeU > 0) {
            g.setStroke(Math.max(1, u(strokeU)), stroke);
        }
        return g;
    }

    static GradientDrawable cardBg() {
        return rounded(CARD, 20, 0x66FFFFFF, 1.5f);
    }

    static TextView tv(Context c, String s, float sizeU, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(sizeU));
        t.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return t;
    }

    /** Nút bấm bằng remote: sáng cam khi được chọn. */
    static TextView button(Context c, String label, float sizeU) {
        TextView b = tv(c, label, sizeU, INK, true);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        b.setPadding(u(14), u(8), u(14), u(8));
        styleFocusable(b, rounded(0xFFE8F3EC, 14, 0xFFB9D3C3, 2), rounded(ORANGE, 14, WHITE, 3));
        b.setTextColor(new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {android.R.attr.state_pressed}, {}},
                new int[]{WHITE, WHITE, INK}));
        return b;
    }

    /** Nền thường / nền khi được chọn + phóng nhẹ khi chọn. */
    static void styleFocusable(android.view.View v, GradientDrawable normal, GradientDrawable focused) {
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_focused}, focused);
        sld.addState(new int[]{android.R.attr.state_pressed}, focused);
        sld.addState(new int[]{}, normal);
        v.setBackground(sld);
        v.setOnFocusChangeListener((view, has) ->
                view.animate().scaleX(has ? 1.05f : 1f).scaleY(has ? 1.05f : 1f).setDuration(110).start());
    }
}
