package com.minhminhhungyen.kidstv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/** Toàn bộ cài đặt và trạng thái lưu của ứng dụng. */
final class Prefs {
    private Prefs() {
    }

    private static final String FILE = "kidstv";

    // Trạng thái phiên xem (dùng trong Session)
    static final String K_STATE = "st";
    static final String K_END = "end";
    static final String K_LEFT = "left";
    /** true: đang khóa vì TV vừa tắt/bật, chờ bé giải toán (khác với khóa do hết giờ). */
    static final String K_GATE = "gate";

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // ---- Cài đặt phụ huynh ------------------------------------------------------------------
    static int sessionMin(Context c) {
        return sp(c).getInt("sessionMin", 30);
    }

    static void setSessionMin(Context c, int v) {
        sp(c).edit().putInt("sessionMin", v).commit();
    }

    static int bonusMin(Context c) {
        return sp(c).getInt("bonusMin", 15);
    }

    static void setBonusMin(Context c, int v) {
        sp(c).edit().putInt("bonusMin", v).commit();
    }

    static int streakGoal(Context c) {
        return sp(c).getInt("streakGoal", 20);
    }

    static void setStreakGoal(Context c, int v) {
        sp(c).edit().putInt("streakGoal", v).commit();
    }

    /** Số lần thưởng bằng bài toán tối đa mỗi ngày; 0 = không giới hạn. */
    static int bonusCap(Context c) {
        return sp(c).getInt("bonusCap", 0);
    }

    static void setBonusCap(Context c, int v) {
        sp(c).edit().putInt("bonusCap", v).commit();
    }

    /**
     * Số câu toán đúng liên tiếp bé phải giải mỗi lần bật TV (từ lần bật thứ 2 trong ngày) rồi mới được xem một phiên mới.
     * 0 = tắt tính năng (tắt màn hình chỉ tạm dừng giờ, bật lại thì đếm tiếp).
     */
    static int gateCount(Context c) {
        return sp(c).getInt("gateCount", 10);
    }

    static void setGateCount(Context c, int v) {
        sp(c).edit().putInt("gateCount", Math.max(0, v)).commit();
    }

    /** Đang chờ bé giải toán sau khi bật TV. */
    static boolean gate(Context c) {
        return sp(c).getBoolean(K_GATE, false);
    }

    /**
     * Dạng bài trên màn hình khóa: 0 = Toán (cộng trừ trong 100), 1 = Tiếng Việt (bé dưới 6 tuổi),
     * 2 = cả hai xen kẽ (mặc định: máy tính bỏ túi chỉ giải được nửa số câu).
     */
    static int subject(Context c) {
        int v = sp(c).getInt("subject", QuizGame.SUBJECT_MIX);
        return v < QuizGame.SUBJECT_MATH || v > QuizGame.SUBJECT_MIX ? QuizGame.SUBJECT_MIX : v;
    }

    static void setSubject(Context c, int v) {
        sp(c).edit().putInt("subject", v).commit();
    }

    /** Chỉ để thử bằng adb: 1..6 ép một dạng bài tiếng Việt, 0 = ngẫu nhiên (bình thường). */
    static int viKind(Context c) {
        return sp(c).getInt("viKind", 0);
    }

    static void setViKind(Context c, int v) {
        sp(c).edit().putInt("viKind", v).commit();
    }

    /** Mỗi ngày bé được xem phiên đầu tiên mà không cần mật khẩu hay làm toán. */
    static boolean freeDaily(Context c) {
        return sp(c).getBoolean("freeDaily", true);
    }

    static void setFreeDaily(Context c, boolean v) {
        sp(c).edit().putBoolean("freeDaily", v).commit();
    }

    // ---- Trạng thái trong ngày ---------------------------------------------------------------
    /** Ngày (yyyymmdd) đã có phiên xem đầu tiên. */
    static int freeDay(Context c) {
        return sp(c).getInt("freeDay", 0);
    }

    static void setFreeDay(Context c, int v) {
        sp(c).edit().putInt("freeDay", v).commit();
    }

    static int streak(Context c) {
        return sp(c).getInt("streak", 0);
    }

    static void setStreak(Context c, int v) {
        sp(c).edit().putInt("streak", v).commit();
    }

    static int bonusToday(Context c) {
        SharedPreferences s = sp(c);
        if (s.getInt("bonusDay", 0) != dayKey(System.currentTimeMillis())) {
            return 0;
        }
        return s.getInt("bonusCount", 0);
    }

    static void resetBonusToday(Context c) {
        sp(c).edit().putInt("bonusDay", 0).putInt("bonusCount", 0).commit();
    }

    static void addBonusToday(Context c) {
        int today = dayKey(System.currentTimeMillis());
        int n = bonusToday(c) + 1;
        sp(c).edit().putInt("bonusDay", today).putInt("bonusCount", n).commit();
    }

    // ---- Chế độ phụ huynh (tạm thời gỡ chặn) --------------------------------------------------
    static long parentUntil(Context c) {
        return sp(c).getLong("parentUntil", 0L);
    }

    static void setParentUntil(Context c, long v) {
        sp(c).edit().putLong("parentUntil", v).commit();
    }

    static boolean parentActive(Context c) {
        return System.currentTimeMillis() < parentUntil(c);
    }

    /** Trong chế độ phụ huynh, nút Home đưa thẳng về màn hình gốc của TV. */
    static boolean passThrough(Context c) {
        return sp(c).getBoolean("passThrough", false);
    }

    static void setPassThrough(Context c, boolean v) {
        sp(c).edit().putBoolean("passThrough", v).commit();
    }

    // ---- Mật khẩu cố định (tùy chọn) ----------------------------------------------------------
    static String pwdHash(Context c) {
        return sp(c).getString("pwdHash", "");
    }

    static String pwdSalt(Context c) {
        return sp(c).getString("pwdSalt", "");
    }

    static int pwdLen(Context c) {
        return sp(c).getInt("pwdLen", 0);
    }

    static void setPwd(Context c, String salt, String hash, int len) {
        sp(c).edit().putString("pwdSalt", salt).putString("pwdHash", hash).putInt("pwdLen", len).commit();
    }

    // ---- Chống đoán mật khẩu ----------------------------------------------------------------------
    static int failCount(Context c) {
        return sp(c).getInt("failCount", 0);
    }

    static void setFailCount(Context c, int v) {
        sp(c).edit().putInt("failCount", v).commit();
    }

    static long lockUntil(Context c) {
        return sp(c).getLong("lockUntil", 0L);
    }

    static void setLockUntil(Context c, long v) {
        sp(c).edit().putLong("lockUntil", v).commit();
    }

    // ---- Thời tiết (bản lưu gần nhất) ------------------------------------------------------------
    static String wxJson(Context c) {
        return sp(c).getString("wxJson", "");
    }

    static long wxTime(Context c) {
        return sp(c).getLong("wxTime", 0L);
    }

    static void setWx(Context c, String json, long time) {
        sp(c).edit().putString("wxJson", json).putLong("wxTime", time).apply();
    }

    /** "Ngày" của TV đổi lúc 05:00 sáng (yyyymmdd), để bé thức khuya không được thêm phiên miễn phí lúc nửa đêm. */
    static int dayKey(long nowMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMs - 5L * 3600_000L);
        return cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH);
    }
}
