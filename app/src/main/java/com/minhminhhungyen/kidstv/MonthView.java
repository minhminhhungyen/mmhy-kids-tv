package com.minhminhhungyen.kidstv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import java.util.Calendar;
import java.util.GregorianCalendar;

/** Lịch tháng dương lịch, mỗi ô có thêm ngày âm lịch nhỏ bên dưới. Tuần bắt đầu từ thứ Hai. */
final class MonthView extends View {
    private static final String[] WEEK = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private int year = 2026;
    private int month = 1;
    private int today = 0;
    private int daysInMonth = 30;
    private int firstCol = 0;
    private String title = "";
    private String sub = "";
    private int[] lunarDay = new int[32];
    private int[] lunarMonth = new int[32];

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    MonthView(Context c) {
        super(c);
        p.setTextAlign(Paint.Align.CENTER);
    }

    /** y, m (1..12); d = ngày hôm nay nếu cùng tháng, ngược lại 0. */
    void setDate(int y, int m, int d) {
        year = y;
        month = m;
        today = d;
        Calendar cal = new GregorianCalendar(y, m - 1, 1);
        daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        firstCol = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        for (int day = 1; day <= daysInMonth; day++) {
            LunarCalendar.Lunar l = LunarCalendar.solarToLunar(day, m, y);
            lunarDay[day] = l.day;
            lunarMonth[day] = l.month;
        }
        title = "Tháng " + m + " năm " + y;
        int ref = d > 0 ? d : 15;
        LunarCalendar.Lunar lr = LunarCalendar.solarToLunar(ref, m, y);
        sub = "Âm lịch: tháng " + lr.month + (lr.leap ? " nhuận" : "") + " · năm " + LunarCalendar.canChiYear(lr.year);
        invalidate();
    }

    /** Đặt cỡ chữ theo mong muốn, thu nhỏ lại nếu chuỗi rộng hơn maxW để không bị cắt chữ. */
    private void fit(String text, float size, float maxW) {
        p.setTextSize(size);
        float tw = p.measureText(text);
        if (tw > maxW && tw > 0f) {
            p.setTextSize(size * maxW / tw);
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float titleH = h * 0.115f;
        float subH = h * 0.085f;
        float headH = h * 0.085f;
        float top = titleH + subH + headH;
        float colW = w / 7f;
        float rowH = (h - top) / 6f;

        p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(Ui.GREEN);
        fit(title, titleH * 0.78f, w * 0.98f);
        cv.drawText(title, w / 2f, titleH * 0.80f, p);

        p.setTypeface(Typeface.DEFAULT);
        p.setColor(Ui.ORANGE);
        fit(sub, subH * 0.80f, w * 0.98f);
        cv.drawText(sub, w / 2f, titleH + subH * 0.80f, p);

        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(headH * 0.78f);
        for (int i = 0; i < 7; i++) {
            p.setColor(i == 6 ? Ui.RED : Ui.MUTED);
            cv.drawText(WEEK[i], colW * (i + 0.5f), titleH + subH + headH * 0.80f, p);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            int idx = firstCol + day - 1;
            int col = idx % 7;
            int row = idx / 7;
            float left = col * colW;
            float t = top + row * rowH;
            boolean isToday = day == today;
            if (isToday) {
                p.setColor(Ui.GREEN);
                r.set(left + colW * 0.06f, t + rowH * 0.04f, left + colW * 0.94f, t + rowH * 0.96f);
                cv.drawRoundRect(r, rowH * 0.22f, rowH * 0.22f, p);
            }
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(rowH * 0.44f);
            p.setColor(isToday ? Ui.WHITE : (col == 6 ? Ui.RED : Ui.INK));
            cv.drawText(String.valueOf(day), left + colW / 2f, t + rowH * 0.54f, p);

            boolean special = lunarDay[day] == 1 || lunarDay[day] == 15;
            String ls = lunarDay[day] == 1 ? lunarDay[day] + "/" + lunarMonth[day] : String.valueOf(lunarDay[day]);
            p.setTypeface(special ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            p.setTextSize(rowH * 0.27f);
            p.setColor(isToday ? 0xFFFFE0B2 : (special ? Ui.ORANGE : Ui.MUTED));
            cv.drawText(ls, left + colW / 2f, t + rowH * 0.90f, p);
        }
    }
}
