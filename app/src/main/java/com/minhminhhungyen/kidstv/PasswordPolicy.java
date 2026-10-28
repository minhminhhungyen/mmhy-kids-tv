package com.minhminhhungyen.kidstv;

import android.content.Context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Locale;

/**
 * Mật khẩu phụ huynh. Mặc định = ngày hôm nay dạng ddMMyyyy (28/10/2026 → 28102026), tự đổi mỗi ngày.
 * Phụ huynh có thể đặt mật khẩu cố định 4–8 số; khi đó chỉ mật khẩu cố định có hiệu lực.
 */
final class PasswordPolicy {
    private PasswordPolicy() {
    }

    static final int DATE_LEN = 8;
    static final int MIN_LEN = 4;
    static final int MAX_LEN = 8;

    static String dateString(long nowMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMs);
        return String.format(Locale.US, "%02d%02d%04d",
                cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
    }

    static boolean hasCustom(Context c) {
        return !Prefs.pwdHash(c).isEmpty();
    }

    /** Số chữ số của mật khẩu đang có hiệu lực (để tự kiểm tra khi gõ đủ). */
    static int expectedLength(Context c) {
        return hasCustom(c) ? Prefs.pwdLen(c) : DATE_LEN;
    }

    static boolean check(Context c, String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        if (hasCustom(c)) {
            return hash(Prefs.pwdSalt(c), input).equals(Prefs.pwdHash(c));
        }
        return input.equals(dateString(System.currentTimeMillis()));
    }

    static void setCustom(Context c, String pwd) {
        byte[] raw = new byte[8];
        new SecureRandom().nextBytes(raw);
        String salt = hex(raw);
        Prefs.setPwd(c, salt, hash(salt, pwd), pwd.length());
    }

    static void clearCustom(Context c) {
        Prefs.setPwd(c, "", "", 0);
    }

    private static String hash(String salt, String pwd) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest((salt + ":" + pwd).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format(Locale.US, "%02x", x));
        }
        return sb.toString();
    }
}
