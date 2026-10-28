package com.minhminhhungyen.kidstv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Biểu tượng thời tiết vẽ bằng mã (không dùng emoji/phông chữ) để TV nào cũng hiện đúng.
 * Vẽ trong hình vuông đơn vị 1×1 rồi phóng theo kích thước View.
 */
final class WeatherIcon extends View {
    static final int CLEAR = 0;
    static final int MOSTLY_CLEAR = 1;
    static final int PARTLY = 2;
    static final int CLOUDY = 3;
    static final int FOG = 4;
    static final int DRIZZLE = 5;
    static final int RAIN = 6;
    static final int SNOW = 7;
    static final int STORM = 8;

    private static final int SUN = 0xFFFFB300;
    private static final int MOON = 0xFFFFD54F;
    private static final int CLOUD = 0xFF9FB3C0;
    private static final int CLOUD_DARK = 0xFF607D8B;
    private static final int DROP = 0xFF2196F3;
    private static final int SNOWC = 0xFF64B5F6;
    private static final int FOGC = 0xFFB0BEC5;

    private int kind = CLOUDY;
    private boolean day = true;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();
    private final Path path2 = new Path();

    WeatherIcon(Context c) {
        super(c);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    /** Đổi mã thời tiết WMO (Open-Meteo) sang loại biểu tượng. */
    static int kindOf(int code) {
        if (code == 0) {
            return CLEAR;
        }
        if (code == 1) {
            return MOSTLY_CLEAR;
        }
        if (code == 2) {
            return PARTLY;
        }
        if (code == 3) {
            return CLOUDY;
        }
        if (code == 45 || code == 48) {
            return FOG;
        }
        if (code >= 51 && code <= 57) {
            return DRIZZLE;
        }
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) {
            return RAIN;
        }
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
            return SNOW;
        }
        if (code >= 95) {
            return STORM;
        }
        return CLOUDY;
    }

    void setWeather(int code, boolean isDay) {
        int k = kindOf(code);
        if (k != kind || isDay != day) {
            kind = k;
            day = isDay;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth();
        float h = getHeight();
        float s = Math.min(w, h);
        if (s <= 0f) {
            return;
        }
        cv.save();
        cv.translate((w - s) / 2f, (h - s) / 2f);
        cv.scale(s, s);
        switch (kind) {
            case CLEAR:
                body(cv, 0.5f, 0.5f, 0.21f);
                break;
            case MOSTLY_CLEAR:
                body(cv, 0.42f, 0.42f, 0.19f);
                cloud(cv, 0.50f, 0.50f, 0.50f, CLOUD);
                break;
            case PARTLY:
                body(cv, 0.34f, 0.34f, 0.15f);
                cloud(cv, 0.18f, 0.16f, 0.80f, CLOUD);
                break;
            case FOG:
                cloud(cv, 0f, -0.06f, 1f, FOGC);
                lines(cv);
                break;
            case DRIZZLE:
                cloud(cv, 0f, -0.04f, 1f, CLOUD);
                drops(cv, 2);
                break;
            case RAIN:
                cloud(cv, 0f, -0.04f, 1f, CLOUD_DARK);
                drops(cv, 3);
                break;
            case SNOW:
                cloud(cv, 0f, -0.04f, 1f, CLOUD);
                flakes(cv);
                break;
            case STORM:
                cloud(cv, 0f, -0.06f, 1f, CLOUD_DARK);
                bolt(cv);
                break;
            case CLOUDY:
            default:
                cloud(cv, 0f, 0.04f, 1f, CLOUD);
                break;
        }
        cv.restore();
    }

    /** Mặt trời (ban ngày) hoặc trăng khuyết (ban đêm). */
    private void body(Canvas cv, float cx, float cy, float rad) {
        if (day) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(SUN);
            cv.drawCircle(cx, cy, rad, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(rad * 0.30f);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0;
                float c = (float) Math.cos(a);
                float sn = (float) Math.sin(a);
                cv.drawLine(cx + c * rad * 1.40f, cy + sn * rad * 1.40f, cx + c * rad * 1.80f, cy + sn * rad * 1.80f, p);
            }
        } else {
            float m = rad * 1.25f;
            path.reset();
            path.addCircle(cx, cy, m, Path.Direction.CW);
            path2.reset();
            path2.addCircle(cx + m * 0.55f, cy - m * 0.32f, m * 0.86f, Path.Direction.CW);
            path.op(path2, Path.Op.DIFFERENCE);
            p.setStyle(Paint.Style.FILL);
            p.setColor(MOON);
            cv.drawPath(path, p);
        }
    }

    private void cloud(Canvas cv, float ox, float oy, float sc, int color) {
        cv.save();
        cv.translate(ox, oy);
        cv.scale(sc, sc);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        cv.drawCircle(0.33f, 0.60f, 0.17f, p);
        cv.drawCircle(0.52f, 0.48f, 0.23f, p);
        cv.drawCircle(0.72f, 0.60f, 0.16f, p);
        r.set(0.30f, 0.60f, 0.76f, 0.77f);
        cv.drawRoundRect(r, 0.085f, 0.085f, p);
        cv.restore();
    }

    private void drops(Canvas cv, int n) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(DROP);
        p.setStrokeWidth(0.055f);
        for (int i = 0; i < n; i++) {
            float x = (n == 2 ? 0.43f : 0.36f) + i * 0.17f;
            cv.drawLine(x, 0.80f, x - 0.05f, 0.93f, p);
        }
    }

    private void flakes(Canvas cv) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(SNOWC);
        cv.drawCircle(0.38f, 0.86f, 0.040f, p);
        cv.drawCircle(0.55f, 0.92f, 0.040f, p);
        cv.drawCircle(0.72f, 0.86f, 0.040f, p);
    }

    private void lines(Canvas cv) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(FOGC);
        p.setStrokeWidth(0.05f);
        cv.drawLine(0.22f, 0.80f, 0.80f, 0.80f, p);
        cv.drawLine(0.30f, 0.91f, 0.72f, 0.91f, p);
    }

    private void bolt(Canvas cv) {
        path.reset();
        path.moveTo(0.57f, 0.72f);
        path.lineTo(0.44f, 0.89f);
        path.lineTo(0.54f, 0.89f);
        path.lineTo(0.48f, 1.00f);
        path.lineTo(0.68f, 0.82f);
        path.lineTo(0.57f, 0.82f);
        path.lineTo(0.64f, 0.72f);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(SUN);
        cv.drawPath(path, p);
    }
}
