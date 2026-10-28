package com.minhminhhungyen.kidstv;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PasswordPolicyTest {
    private static TimeZone old;

    @BeforeClass
    public static void zone() {
        old = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    @AfterClass
    public static void restore() {
        TimeZone.setDefault(old);
    }

    private static long at(int y, int m, int d, int h, int min) {
        return new GregorianCalendar(y, m - 1, d, h, min).getTimeInMillis();
    }

    /** Ví dụ của Sếp: hôm nay 28/10/2026 → 28102026, mai → 29102026. */
    @Test
    public void defaultPasswordIsTodayDdMMyyyy() {
        assertEquals("28102026", PasswordPolicy.dateString(at(2026, 10, 28, 9, 0)));
        assertEquals("29102026", PasswordPolicy.dateString(at(2026, 10, 29, 0, 1)));
        assertEquals("05012027", PasswordPolicy.dateString(at(2027, 1, 5, 23, 59)));
        assertEquals("01032028", PasswordPolicy.dateString(at(2028, 3, 1, 12, 0)));
    }

    /** "Ngày" của phiên miễn phí đổi lúc 05:00 sáng. */
    @Test
    public void dayKeyChangesAtFiveAm() {
        assertEquals(20261028, Prefs.dayKey(at(2026, 10, 29, 4, 59)));
        assertEquals(20261029, Prefs.dayKey(at(2026, 10, 29, 5, 0)));
        assertEquals(20261028, Prefs.dayKey(at(2026, 10, 28, 5, 0)));
        assertEquals(20261231, Prefs.dayKey(at(2027, 1, 1, 4, 30)));
        assertEquals(20270101, Prefs.dayKey(at(2027, 1, 1, 5, 30)));
    }

    @Test
    public void lengths() {
        assertEquals(8, PasswordPolicy.DATE_LEN);
        assertEquals(4, PasswordPolicy.MIN_LEN);
        assertEquals(8, PasswordPolicy.MAX_LEN);
    }
}
