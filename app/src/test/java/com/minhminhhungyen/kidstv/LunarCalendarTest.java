package com.minhminhhungyen.kidstv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;

public class LunarCalendarTest {

    private static String fmt(LunarCalendar.Lunar l) {
        return l.day + "/" + l.month + "/" + l.year + (l.leap ? " nhuận" : "");
    }

    /** Mùng 1 Tết (dương lịch) khớp lịch Việt Nam; bỏ 2007 và 2030 vì Việt Nam (UTC+7) lệch Trung Quốc 1 ngày. */
    @Test
    public void tetDays() {
        int[][] tet = {{2000, 2, 5}, {2001, 1, 24}, {2002, 2, 12}, {2003, 2, 1}, {2004, 1, 22}, {2005, 2, 9},
                {2006, 1, 29}, {2008, 2, 7}, {2009, 1, 26}, {2010, 2, 14}, {2011, 2, 3}, {2012, 1, 23},
                {2013, 2, 10}, {2014, 1, 31}, {2015, 2, 19}, {2016, 2, 8}, {2017, 1, 28}, {2018, 2, 16},
                {2019, 2, 5}, {2020, 1, 25}, {2021, 2, 12}, {2022, 2, 1}, {2023, 1, 22}, {2024, 2, 10},
                {2025, 1, 29}, {2026, 2, 17}, {2027, 2, 6}, {2028, 1, 26}, {2029, 2, 13}};
        for (int[] t : tet) {
            LunarCalendar.Lunar l = LunarCalendar.solarToLunar(t[2], t[1], t[0]);
            assertEquals("Tết " + t[0], "1/1/" + t[0], fmt(l));
            Calendar c = new GregorianCalendar(t[0], t[1] - 1, t[2]);
            c.add(Calendar.DAY_OF_MONTH, -1);
            LunarCalendar.Lunar eve = LunarCalendar.solarToLunar(
                    c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
            assertEquals("Giao thừa " + t[0], 12, eve.month);
            assertTrue("Giao thừa " + t[0], eve.day == 29 || eve.day == 30);
        }
    }

    @Test
    public void midAutumn() {
        int[][] tt = {{2022, 9, 10}, {2023, 9, 29}, {2024, 9, 17}, {2025, 10, 6}, {2026, 9, 25}};
        for (int[] t : tt) {
            LunarCalendar.Lunar l = LunarCalendar.solarToLunar(t[2], t[1], t[0]);
            assertEquals("Trung thu " + t[0], "15/8", l.day + "/" + l.month);
        }
    }

    @Test
    public void leapMonths() {
        assertEquals("8/6/2025 nhuận", fmt(LunarCalendar.solarToLunar(1, 8, 2025)));
        assertEquals("30/6/2025", fmt(LunarCalendar.solarToLunar(24, 7, 2025)));
        assertEquals("11/2/2023 nhuận", fmt(LunarCalendar.solarToLunar(1, 4, 2023)));
    }

    @Test
    public void knownDates() {
        assertEquals("13/11/2025", fmt(LunarCalendar.solarToLunar(1, 1, 2026)));
        // Ngày viết mật khẩu mẫu của Sếp: 28/10/2026 = 19 tháng 9 năm Bính Ngọ.
        assertEquals("19/9/2026", fmt(LunarCalendar.solarToLunar(28, 10, 2026)));
    }

    @Test
    public void canChi() {
        assertEquals("Bính Ngọ", LunarCalendar.canChiYear(2026));
        assertEquals("Ất Tỵ", LunarCalendar.canChiYear(2025));
        assertEquals("Giáp Thìn", LunarCalendar.canChiYear(2024));
        assertEquals("Giáp Tý", LunarCalendar.canChiYear(1984));
        assertEquals("Mậu Ngọ", LunarCalendar.canChiDay(1, 1, 2000));
    }

    /** Mỗi ngày dương liền nhau là ngày âm liền nhau (hoặc mùng 1 của tháng kế), trong 12 năm. */
    @Test
    public void everyDayIsConsecutive() {
        Calendar c = new GregorianCalendar(2020, 0, 1);
        LunarCalendar.Lunar prev = null;
        for (int i = 0; i < 365 * 12; i++) {
            LunarCalendar.Lunar l = LunarCalendar.solarToLunar(
                    c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
            String when = c.get(Calendar.DAY_OF_MONTH) + "/" + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.YEAR);
            assertTrue("ngày âm 1..30 tại " + when, l.day >= 1 && l.day <= 30);
            assertTrue("tháng âm 1..12 tại " + when, l.month >= 1 && l.month <= 12);
            if (prev != null) {
                boolean sameMonth = prev.month == l.month && prev.leap == l.leap;
                if (sameMonth) {
                    assertEquals("liên tiếp tại " + when, prev.day + 1, l.day);
                } else {
                    assertEquals("tháng mới bắt đầu mùng 1 tại " + when, 1, l.day);
                }
            }
            prev = l;
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
}
