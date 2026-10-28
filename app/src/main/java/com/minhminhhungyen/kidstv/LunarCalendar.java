package com.minhminhhungyen.kidstv;

/**
 * Âm lịch Việt Nam (múi giờ UTC+7).
 *
 * Thuật toán thiên văn (Jean Meeus) theo cách triển khai công khai của Hồ Ngọc Đức:
 * https://www.informatik.uni-leipzig.de/~duc/amlich/ — viết lại bằng Java, không phụ thuộc Android.
 */
final class LunarCalendar {
    private LunarCalendar() {
    }

    static final double TZ = 7.0;
    private static final double PI = Math.PI;

    /** Ngày âm lịch. month = 1..12; leap = true nếu là tháng nhuận. */
    static final class Lunar {
        final int day;
        final int month;
        final int year;
        final boolean leap;

        Lunar(int day, int month, int year, boolean leap) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.leap = leap;
        }
    }

    private static final String[] CAN = {"Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"};
    private static final String[] CHI = {"Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"};

    /** Số ngày Julian của ngày dương lịch (lịch Gregory; trước 15/10/1582 dùng lịch Julius). */
    static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        int jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        if (jd < 2299161) {
            jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
        }
        return jd;
    }

    /** Thời điểm (số ngày Julian) của trăng mới thứ k kể từ trăng mới 1/1/1900 13:52 UTC. */
    static double newMoon(int k) {
        double t = k / 1236.85;
        double t2 = t * t;
        double t3 = t2 * t;
        double dr = PI / 180;
        double jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3;
        jd1 = jd1 + 0.00033 * Math.sin((166.56 + 132.87 * t - 0.009173 * t2) * dr);
        double m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3;
        double mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3;
        double f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3;
        double c1 = (0.1734 - 0.000393 * t) * Math.sin(m * dr) + 0.0021 * Math.sin(2 * dr * m);
        c1 = c1 - 0.4068 * Math.sin(mpr * dr) + 0.0161 * Math.sin(dr * 2 * mpr);
        c1 = c1 - 0.0004 * Math.sin(dr * 3 * mpr);
        c1 = c1 + 0.0104 * Math.sin(dr * 2 * f) - 0.0051 * Math.sin(dr * (m + mpr));
        c1 = c1 - 0.0074 * Math.sin(dr * (m - mpr)) + 0.0004 * Math.sin(dr * (2 * f + m));
        c1 = c1 - 0.0004 * Math.sin(dr * (2 * f - m)) - 0.0006 * Math.sin(dr * (2 * f + mpr));
        c1 = c1 + 0.0010 * Math.sin(dr * (2 * f - mpr)) + 0.0005 * Math.sin(dr * (2 * mpr + m));
        double deltat;
        if (t < -11) {
            deltat = 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3;
        } else {
            deltat = -0.000278 + 0.000265 * t + 0.000262 * t2;
        }
        return jd1 + c1 - deltat;
    }

    /** Kinh độ Mặt Trời (radian, 0..2π) tại thời điểm jdn. */
    static double sunLongitude(double jdn) {
        double t = (jdn - 2451545.0) / 36525;
        double t2 = t * t;
        double dr = PI / 180;
        double m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2;
        double l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2;
        double dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * Math.sin(dr * m);
        dl = dl + (0.019993 - 0.000101 * t) * Math.sin(dr * 2 * m) + 0.000290 * Math.sin(dr * 3 * m);
        double theta = l0 + dl;
        double omega = 125.04 - 1934.136 * t;
        double lambda = theta - 0.00569 - 0.00478 * Math.sin(omega * dr);
        lambda = lambda * dr;
        lambda = lambda - PI * 2 * Math.floor(lambda / (PI * 2));
        return lambda;
    }

    /** Cung Mặt Trời (0..11, mỗi cung 30°) lúc nửa đêm địa phương của ngày dayNumber. */
    static int getSunLongitude(int dayNumber, double timeZone) {
        return (int) Math.floor(sunLongitude(dayNumber - 0.5 - timeZone / 24.0) / PI * 6);
    }

    static int getNewMoonDay(int k, double timeZone) {
        return (int) Math.floor(newMoon(k) + 0.5 + timeZone / 24.0);
    }

    /** Ngày bắt đầu tháng 11 âm lịch (tháng chứa Đông chí) của năm yy. */
    static int getLunarMonth11(int yy, double timeZone) {
        int off = jdFromDate(31, 12, yy) - 2415021;
        int k = (int) Math.floor(off / 29.530588853);
        int nm = getNewMoonDay(k, timeZone);
        int sunLong = getSunLongitude(nm, timeZone);
        if (sunLong >= 9) {
            nm = getNewMoonDay(k - 1, timeZone);
        }
        return nm;
    }

    /** Vị trí tháng nhuận so với tháng 11 (a11) trong năm có nhuận. */
    static int getLeapMonthOffset(int a11, double timeZone) {
        int k = (int) Math.floor((a11 - 2415021.076998695) / 29.530588853 + 0.5);
        int last;
        int i = 1;
        int arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone);
        do {
            last = arc;
            i++;
            arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone);
        } while (arc != last && i < 14);
        return i - 1;
    }

    /** Đổi ngày dương lịch sang âm lịch Việt Nam (UTC+7). */
    static Lunar solarToLunar(int dd, int mm, int yy) {
        return solarToLunar(dd, mm, yy, TZ);
    }

    static Lunar solarToLunar(int dd, int mm, int yy, double timeZone) {
        int dayNumber = jdFromDate(dd, mm, yy);
        int k = (int) Math.floor((dayNumber - 2415021.076998695) / 29.530588853);
        int monthStart = getNewMoonDay(k + 1, timeZone);
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, timeZone);
        }
        int a11 = getLunarMonth11(yy, timeZone);
        int b11 = a11;
        int lunarYear;
        if (a11 >= monthStart) {
            lunarYear = yy;
            a11 = getLunarMonth11(yy - 1, timeZone);
        } else {
            lunarYear = yy + 1;
            b11 = getLunarMonth11(yy + 1, timeZone);
        }
        int lunarDay = dayNumber - monthStart + 1;
        int diff = (int) Math.floor((monthStart - a11) / 29.0);
        boolean leap = false;
        int lunarMonth = diff + 11;
        if (b11 - a11 > 365) {
            int leapMonthDiff = getLeapMonthOffset(a11, timeZone);
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10;
                if (diff == leapMonthDiff) {
                    leap = true;
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth = lunarMonth - 12;
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1;
        }
        return new Lunar(lunarDay, lunarMonth, lunarYear, leap);
    }

    /** Can Chi của năm âm lịch, ví dụ 2026 → "Bính Ngọ". */
    static String canChiYear(int lunarYear) {
        return CAN[(lunarYear + 6) % 10] + " " + CHI[(lunarYear + 8) % 12];
    }

    /** Can Chi của ngày (theo số ngày Julian của ngày dương lịch). */
    static String canChiDay(int dd, int mm, int yy) {
        int jd = jdFromDate(dd, mm, yy);
        return CAN[(jd + 9) % 10] + " " + CHI[(jd + 1) % 12];
    }
}
