package com.minhminhhungyen.kidstv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Cổng điều khiển bằng adb (receiver chỉ nhận từ tài khoản có quyền DUMP, tức adb shell):
 *   am broadcast -n com.minhminhhungyen.kidstv/.AdminReceiver -a com.minhminhhungyen.kidstv.STATUS
 * Lệnh: STATUS, GRANT (--ei minutes N | --ei seconds N), EXPIRE, RESET_DAY (về trạng thái đầu ngày mới), RESET_PWD,
 *       PARENT_ON (--ei minutes N), PARENT_OFF,
 *       CONFIG (--ei session N --ei bonus N --ei streak N --ei cap N --ei free 0|1 --ei gate N --ei freeDay N;
 *               gate 0 = tắt toán khi bật TV; freeDay 0 = coi như hôm nay chưa dùng phiên miễn phí),
 *       POWERCYCLE (giả lập "TV vừa tắt"), BOOT (giả lập "TV vừa khởi động xong"; adb không gửi được BOOT_COMPLETED thật),
 *       EVENTS (các sự kiện tắt/bật gần nhất, còn nguyên sau khi TV khởi động lại), SHUTDOWN (gỡ dịch vụ bảo vệ).
 */
public class AdminReceiver extends BroadcastReceiver {
    static final String P = "com.minhminhhungyen.kidstv.";

    @Override
    public void onReceive(Context c, Intent i) {
        String raw = (i == null || i.getAction() == null) ? "" : i.getAction();
        String act = raw.startsWith(P) ? raw.substring(P.length()) : raw;
        String out;
        switch (act) {
            case "GRANT": {
                int sec = i.getIntExtra("seconds", 0);
                int min = i.getIntExtra("minutes", 0);
                long ms = sec > 0 ? sec * 1000L : (min > 0 ? min * 60_000L : Prefs.sessionMin(c) * 60_000L);
                Session.grantMs(c, ms, "adb");
                out = "granted " + (ms / 1000) + "s";
                break;
            }
            case "EXPIRE":
                Session.expire(c, "adb");
                Home.bringToFront(c, "adb-expire");
                out = "expired";
                break;
            case "RESET_DAY":
                Session.expire(c, "adb-reset-day");
                Prefs.setFreeDay(c, 0);
                Prefs.setStreak(c, 0);
                Prefs.resetBonusToday(c);
                Prefs.setParentUntil(c, 0L);
                Prefs.setPassThrough(c, false);
                out = "day reset (next time the home screen opens, the free daily session starts)";
                break;
            case "RESET_PWD":
                PasswordPolicy.clearCustom(c);
                Prefs.setFailCount(c, 0);
                Prefs.setLockUntil(c, 0L);
                out = "password reset to date (ddMMyyyy)";
                break;
            case "PARENT_ON": {
                int min = i.getIntExtra("minutes", 10);
                Prefs.setParentUntil(c, System.currentTimeMillis() + min * 60_000L);
                out = "parent mode " + min + " min";
                break;
            }
            case "PARENT_OFF":
                Prefs.setParentUntil(c, 0L);
                Prefs.setPassThrough(c, false);
                out = "parent mode off";
                break;
            case "CONFIG":
                if (i.hasExtra("session")) {
                    Prefs.setSessionMin(c, i.getIntExtra("session", 30));
                }
                if (i.hasExtra("bonus")) {
                    Prefs.setBonusMin(c, i.getIntExtra("bonus", 15));
                }
                if (i.hasExtra("streak")) {
                    Prefs.setStreakGoal(c, Math.max(1, i.getIntExtra("streak", 20)));
                }
                if (i.hasExtra("cap")) {
                    Prefs.setBonusCap(c, i.getIntExtra("cap", 0));
                }
                if (i.hasExtra("free")) {
                    Prefs.setFreeDaily(c, i.getIntExtra("free", 1) != 0);
                }
                if (i.hasExtra("gate")) {
                    Prefs.setGateCount(c, i.getIntExtra("gate", 10));
                }
                if (i.hasExtra("freeDay")) {
                    // Để thử: 0 = hôm nay bé chưa dùng phiên miễn phí (không đụng tới phiên đang chạy).
                    Prefs.setFreeDay(c, i.getIntExtra("freeDay", 0));
                }
                out = "config ok";
                break;
            case "POWERCYCLE":
                out = Session.powerCycle(c, "adb") ? "gate started" : "gate is off (gateCount=0), nothing done";
                break;
            case "BOOT":
                new BootReceiver().onReceive(c, new Intent(Intent.ACTION_BOOT_COMPLETED));
                out = "boot simulated";
                break;
            case "EVENTS":
                out = Dbg.events(c, 60);
                break;
            case "SHUTDOWN":
                GuardEnabler.disable(c);
                Prefs.setParentUntil(c, System.currentTimeMillis() + 24L * 3600_000L);
                out = "guard disabled, parent mode 24h";
                break;
            case "STATUS":
            default:
                out = status(c);
                break;
        }
        Dbg.d("admin " + act + " -> " + out);
        if (isOrderedBroadcast()) {
            setResultData(out);
        }
    }

    static String status(Context c) {
        int st = Session.state(c);
        String[] names = {"LOCKED", "RUNNING", "PAUSED"};
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("state=").append(names[Math.max(0, Math.min(2, st))]);
        sb.append(" remaining=").append(Session.remainingMs(c) / 1000).append("s");
        sb.append(" parent=").append(Prefs.parentActive(c));
        sb.append(" passThrough=").append(Prefs.passThrough(c));
        boolean gateMode = Prefs.gate(c) && Prefs.gateCount(c) > 0;
        sb.append(" streak=").append(Prefs.streak(c)).append('/').append(gateMode ? Prefs.gateCount(c) : Prefs.streakGoal(c));
        sb.append(" gate=").append(Prefs.gate(c)).append(" gateCount=").append(Prefs.gateCount(c));
        sb.append(" session=").append(Prefs.sessionMin(c)).append("m bonus=").append(Prefs.bonusMin(c)).append('m');
        sb.append(" bonusToday=").append(Prefs.bonusToday(c)).append("/cap").append(Prefs.bonusCap(c));
        sb.append(" freeDaily=").append(Prefs.freeDaily(c)).append(" freeDay=").append(Prefs.freeDay(c));
        sb.append(" today=").append(Prefs.dayKey(now));
        sb.append(" customPwd=").append(PasswordPolicy.hasCustom(c));
        sb.append(" guardEnabled=").append(GuardEnabler.isEnabled(c));
        sb.append(" guardRunning=").append(GuardService.instance != null);
        sb.append(" lastPkg=").append(GuardService.lastPkg);
        sb.append(" defaultHome=").append(Kids.weAreDefaultHome(c));
        sb.append(" kidApp=").append(Kids.installed(c));
        sb.append(" kidPkg=").append(Kids.PKG);
        sb.append(" homeResumed=").append(MainActivity.resumed);
        return sb.toString();
    }
}
