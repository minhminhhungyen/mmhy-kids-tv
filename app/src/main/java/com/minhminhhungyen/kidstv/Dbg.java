package com.minhminhhungyen.kidstv;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Nhật ký cho logcat (thẻ KidsTV). Xem bằng bat 7-XEM-NHAT-KY.
 * Các sự kiện quan trọng (tắt/bật màn hình, khởi động, cấp giờ, khóa...) còn được ghi vào một tệp nhỏ
 * để xem lại được sau khi TV khởi động lại (lúc đó logcat bị xóa): lệnh adb EVENTS.
 */
final class Dbg {
    static final String TAG = "KidsTV";

    private static final String EVENTS_FILE = "events.log";
    private static final int EVENTS_MAX_BYTES = 16 * 1024;
    private static final Object FILE_LOCK = new Object();

    private Dbg() {
    }

    static void d(String msg) {
        Log.i(TAG, msg);
    }

    /** Ghi logcat và thêm một dòng (giờ, thời gian TV đã chạy, nội dung) vào tệp sự kiện. */
    static void event(Context c, String msg) {
        Log.i(TAG, msg);
        try {
            String line = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date())
                    + " up=" + (SystemClock.elapsedRealtime() / 1000L) + "s " + msg + "\n";
            File f = new File(c.getApplicationContext().getFilesDir(), EVENTS_FILE);
            synchronized (FILE_LOCK) {
                if (f.length() > EVENTS_MAX_BYTES) {
                    String all = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    int cut = all.indexOf('\n', all.length() / 2);
                    try (FileOutputStream o = new FileOutputStream(f, false)) {
                        o.write(all.substring(cut < 0 ? all.length() / 2 : cut + 1).getBytes(StandardCharsets.UTF_8));
                    }
                }
                try (FileOutputStream o = new FileOutputStream(f, true)) {
                    o.write(line.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "khong ghi duoc tep su kien: " + e);
        }
    }

    /** Tối đa n dòng sự kiện cuối, nối bằng " | " (để in gọn trong kết quả lệnh adb). */
    static String events(Context c, int n) {
        try {
            File f = new File(c.getApplicationContext().getFilesDir(), EVENTS_FILE);
            String all;
            synchronized (FILE_LOCK) {
                if (!f.exists()) {
                    return "(chua co su kien)";
                }
                all = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            }
            String[] lines = all.split("\n");
            StringBuilder sb = new StringBuilder();
            for (int i = Math.max(0, lines.length - n); i < lines.length; i++) {
                if (lines[i].isEmpty()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(lines[i].replace('"', '\''));
            }
            return sb.toString();
        } catch (IOException | RuntimeException e) {
            return "(loi doc su kien: " + e + ")";
        }
    }
}
