package com.minhminhhungyen.kidstv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Phiên xem TV của bé: KHÓA (LOCKED) → ĐANG XEM (RUNNING) → TẠM DỪNG khi TV tắt màn hình (PAUSED).
 * Hết giờ thì khóa; mở lại bằng mật khẩu phụ huynh hoặc làm toán.
 * Hạn dùng lưu theo giờ đồng hồ (end) nên sống sót qua khởi động lại; báo thức AlarmManager đặt lại khi bật máy.
 */
final class Session {
    private Session() {
    }

    static final int LOCKED = 0;
    static final int RUNNING = 1;
    static final int PAUSED = 2;

    static final String ACT_END = "com.minhminhhungyen.kidstv.alarm.END";
    static final String ACT_WARN5 = "com.minhminhhungyen.kidstv.alarm.WARN5";
    static final String ACT_WARN1 = "com.minhminhhungyen.kidstv.alarm.WARN1";

    private static final int RC_END = 1;
    private static final int RC_WARN5 = 2;
    private static final int RC_WARN1 = 3;

    private static final Object LOCK = new Object();

    static int state(Context c) {
        return Prefs.sp(c).getInt(Prefs.K_STATE, LOCKED);
    }

    /** Đang được phép xem (kể cả lúc tạm dừng vì TV tắt màn hình). */
    static boolean allowed(Context c) {
        return state(c) != LOCKED;
    }

    static long remainingMs(Context c) {
        synchronized (LOCK) {
            int st = state(c);
            if (st == RUNNING) {
                return Math.max(0L, Prefs.sp(c).getLong(Prefs.K_END, 0L) - System.currentTimeMillis());
            }
            if (st == PAUSED) {
                return Math.max(0L, Prefs.sp(c).getLong(Prefs.K_LEFT, 0L));
            }
            return 0L;
        }
    }

    static void grantMinutes(Context c, int minutes, String why) {
        grantMs(c, minutes * 60_000L, why);
    }

    /** Cho xem thêm ms mili giây tính từ bây giờ (thay hẳn phiên cũ). */
    static void grantMs(Context c, long ms, String why) {
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            Prefs.sp(c).edit()
                    .putInt(Prefs.K_STATE, RUNNING)
                    .putLong(Prefs.K_END, now + ms)
                    .putLong(Prefs.K_LEFT, 0L)
                    .putInt("freeDay", Prefs.dayKey(now))
                    .putInt("grantDay", Prefs.dayKey(now))
                    .putInt("streak", 0)
                    .commit();
            arm(c, now + ms);
            Dbg.d("grant " + (ms / 1000) + "s (" + why + ")");
        }
    }

    static void expire(Context c, String why) {
        synchronized (LOCK) {
            Prefs.sp(c).edit()
                    .putInt(Prefs.K_STATE, LOCKED)
                    .putLong(Prefs.K_END, 0L)
                    .putLong(Prefs.K_LEFT, 0L)
                    .commit();
            cancel(c);
            Dbg.d("expire (" + why + ")");
        }
    }

    /** TV tắt màn hình: dừng đếm giờ, nhớ phần còn lại. */
    static void pause(Context c) {
        synchronized (LOCK) {
            if (state(c) != RUNNING) {
                return;
            }
            long left = Math.max(0L, Prefs.sp(c).getLong(Prefs.K_END, 0L) - System.currentTimeMillis());
            Prefs.sp(c).edit().putInt(Prefs.K_STATE, PAUSED).putLong(Prefs.K_LEFT, left).commit();
            cancel(c);
            Dbg.d("pause, left " + (left / 1000) + "s");
        }
    }

    /** TV bật màn hình: đếm tiếp phần còn lại. */
    static void resume(Context c) {
        synchronized (LOCK) {
            if (state(c) != PAUSED) {
                return;
            }
            if (Prefs.sp(c).getInt("grantDay", 0) != Prefs.dayKey(System.currentTimeMillis())) {
                // Giờ còn dư của hôm qua không mang sang ngày mới.
                expire(c, "ngay-moi");
                return;
            }
            long left = Math.max(0L, Prefs.sp(c).getLong(Prefs.K_LEFT, 0L));
            long end = System.currentTimeMillis() + left;
            Prefs.sp(c).edit().putInt(Prefs.K_STATE, RUNNING).putLong(Prefs.K_END, end).putLong(Prefs.K_LEFT, 0L).commit();
            arm(c, end);
            Dbg.d("resume, left " + (left / 1000) + "s");
        }
    }

    /** Hết giờ thì chuyển sang KHÓA. Trả về true nếu vừa khóa. */
    static boolean checkExpired(Context c) {
        synchronized (LOCK) {
            int st = state(c);
            if (st == RUNNING && System.currentTimeMillis() >= Prefs.sp(c).getLong(Prefs.K_END, 0L) - 300L) {
                expire(c, "time-up");
                return true;
            }
            if (st == PAUSED && Prefs.sp(c).getLong(Prefs.K_LEFT, 0L) <= 0L) {
                expire(c, "paused-empty");
                return true;
            }
            return false;
        }
    }

    /** Đặt lại báo thức (sau khi bật máy, cập nhật ứng dụng, hoặc báo thức đến sớm). */
    static void rearm(Context c) {
        synchronized (LOCK) {
            if (checkExpired(c)) {
                return;
            }
            if (state(c) == RUNNING) {
                arm(c, Prefs.sp(c).getLong(Prefs.K_END, 0L));
            }
        }
    }

    private static void arm(Context c, long endWall) {
        cancel(c);
        long now = System.currentTimeMillis();
        set(c, RC_END, ACT_END, endWall);
        if (endWall - 5 * 60_000L > now + 3000L) {
            set(c, RC_WARN5, ACT_WARN5, endWall - 5 * 60_000L);
        }
        if (endWall - 60_000L > now + 3000L) {
            set(c, RC_WARN1, ACT_WARN1, endWall - 60_000L);
        }
    }

    private static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        am.cancel(pending(c, RC_END, ACT_END));
        am.cancel(pending(c, RC_WARN5, ACT_WARN5));
        am.cancel(pending(c, RC_WARN1, ACT_WARN1));
    }

    private static PendingIntent pending(Context c, int code, String action) {
        Intent i = new Intent(c, TimeUpReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void set(Context c, int code, String action, long atWall) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        PendingIntent pi = pending(c, code, action);
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC, atWall, pi);
        } catch (SecurityException e) {
            Dbg.d("exact alarm refused, using inexact: " + e);
            am.setAndAllowWhileIdle(AlarmManager.RTC, atWall, pi);
        }
    }
}
