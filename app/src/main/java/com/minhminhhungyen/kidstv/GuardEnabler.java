package com.minhminhhungyen.kidstv;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

/**
 * Bật/tắt dịch vụ bảo vệ (GuardService) trong cài đặt trợ năng.
 * Cần quyền WRITE_SECURE_SETTINGS, cấp một lần qua adb (script 5-CAI-LAUNCHER-BE).
 */
final class GuardEnabler {
    private GuardEnabler() {
    }

    static boolean isEnabled(Context c) {
        ContentResolver cr = c.getContentResolver();
        String cur = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return contains(cur, new ComponentName(c, GuardService.class))
                && Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;
    }

    /** Bảo đảm dịch vụ đang bật; trả về true nếu đang bật sau khi gọi. */
    static boolean ensure(Context c) {
        try {
            if (isEnabled(c)) {
                return true;
            }
            if (c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                Dbg.d("guard: thieu quyen WRITE_SECURE_SETTINGS");
                return false;
            }
            ContentResolver cr = c.getContentResolver();
            ComponentName cn = new ComponentName(c, GuardService.class);
            String cur = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (!contains(cur, cn)) {
                String flat = cn.flattenToString();
                boolean empty = cur == null || cur.isEmpty() || cur.equals("null");
                Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, empty ? flat : cur + ":" + flat);
            }
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            Dbg.d("guard: da bat dich vu bao ve");
            return true;
        } catch (Throwable t) {
            Dbg.d("guard ensure fail: " + t);
            return false;
        }
    }

    /** Gỡ dịch vụ khỏi danh sách bật (dùng khi thoát chế độ trẻ em). Các dịch vụ khác giữ nguyên. */
    static void disable(Context c) {
        try {
            if (c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            ContentResolver cr = c.getContentResolver();
            ComponentName cn = new ComponentName(c, GuardService.class);
            String cur = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (cur == null || cur.isEmpty()) {
                return;
            }
            StringBuilder keep = new StringBuilder();
            for (String part : cur.split(":")) {
                ComponentName x = ComponentName.unflattenFromString(part);
                if (x != null && x.equals(cn)) {
                    continue;
                }
                if (part.isEmpty()) {
                    continue;
                }
                if (keep.length() > 0) {
                    keep.append(':');
                }
                keep.append(part);
            }
            if (keep.length() == 0) {
                Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "");
                Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            } else {
                Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, keep.toString());
            }
            Dbg.d("guard: da tat dich vu bao ve");
        } catch (Throwable t) {
            Dbg.d("guard disable fail: " + t);
        }
    }

    private static boolean contains(String list, ComponentName cn) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (String part : list.split(":")) {
            ComponentName x = ComponentName.unflattenFromString(part);
            if (x != null && x.equals(cn)) {
                return true;
            }
        }
        return false;
    }
}
