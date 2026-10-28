package com.minhminhhungyen.kidstv;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ứng dụng YouTube Kids và danh sách gói được phép / bị chặn. */
final class Kids {
    private Kids() {
    }

    static final String PKG = "com.google.android.youtube.tvkids";
    static final String STOCK_HOME_PKG = "com.google.android.apps.tv.launcherx";

    /** Cửa sổ hệ thống / dịch vụ luôn được phép (âm lượng, hộp thoại quyền, giọng nói...). */
    static final Set<String> ALWAYS_OK = new HashSet<>(Arrays.asList(
            "android",
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.google.android.katniss",
            "com.google.android.googlequicksearchbox",
            "com.google.android.tv.remote.service",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.tcl.suspension"
    ));

    /** Ứng dụng chắc chắn phải chặn khi bé đang dùng TV (ngoài mọi ứng dụng mở được từ màn hình chính). */
    static final Set<String> DENY = new HashSet<>(Arrays.asList(
            "com.android.tv.settings",
            "com.android.settings",
            "com.android.vending",
            STOCK_HOME_PKG,
            "com.google.android.tvlauncher",
            "com.google.android.tvrecommendations",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.tcl.tv",
            "com.tcl.ui_mediaCenter",
            "com.tcl.guard",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
    ));

    static boolean installed(Context c) {
        try {
            ApplicationInfo ai = c.getPackageManager().getApplicationInfo(PKG, 0);
            return ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static Intent launchIntent(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(PKG);
        if (i == null) {
            i = pm.getLaunchIntentForPackage(PKG);
        }
        return i;
    }

    static boolean launch(Context c) {
        Intent i = launchIntent(c);
        if (i == null) {
            Dbg.d("YouTube Kids chua cai hoac dang tat");
            return false;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
            return true;
        } catch (Throwable t) {
            Dbg.d("launch YouTube Kids fail: " + t);
            return false;
        }
    }

    /** Tắt hẳn tiến trình YouTube Kids đang chạy nền (âm thanh/video dừng hẳn). */
    static void kill(Context c) {
        try {
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(PKG);
            }
        } catch (Throwable t) {
            Dbg.d("kill fail: " + t);
        }
    }

    /** Màn hình chính gốc của TV (Google TV launcherx), để phụ huynh dùng TV bình thường. */
    static Intent stockHome(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
        ResolveInfo best = null;
        for (ResolveInfo ri : list) {
            String p = ri.activityInfo.packageName;
            String n = ri.activityInfo.name;
            if (p.equals(c.getPackageName()) || n.contains("Fallback") || p.contains("setupwraith")) {
                continue;
            }
            if (p.equals(STOCK_HOME_PKG)) {
                best = ri;
                break;
            }
            if (best == null) {
                best = ri;
            }
        }
        if (best == null) {
            return null;
        }
        return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setClassName(best.activityInfo.packageName, best.activityInfo.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    static boolean startStockHome(Context c) {
        Intent i = stockHome(c);
        if (i == null) {
            return false;
        }
        try {
            c.startActivity(i);
            return true;
        } catch (Throwable t) {
            Dbg.d("start stock home fail: " + t);
            return false;
        }
    }

    static boolean startSettings(Context c) {
        try {
            Intent i = new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Throwable t) {
            try {
                Intent i = new Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                return true;
            } catch (Throwable t2) {
                Dbg.d("start settings fail: " + t2);
                return false;
            }
        }
    }

    /** Ứng dụng này đang là màn hình chính mặc định của TV? */
    static boolean weAreDefaultHome(Context c) {
        Intent h = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = c.getPackageManager().resolveActivity(h, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null && c.getPackageName().equals(ri.activityInfo.packageName);
    }
}
