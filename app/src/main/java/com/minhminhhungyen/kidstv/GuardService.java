package com.minhminhhungyen.kidstv;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.HashMap;
import java.util.List;

/**
 * Dịch vụ bảo vệ (trợ năng). Làm 3 việc:
 * 1) Khi một cửa sổ ứng dụng lạ hiện ra (Cài đặt, Live TV, Play Store, màn hình chính gốc...) thì bấm Home để về màn hình trẻ em.
 * 2) Khi đang khóa (hết giờ) mà YouTube hay bất kỳ ứng dụng nào lên trước thì đưa về màn hình khóa.
 * 3) Theo dõi màn hình TV bật/tắt để tạm dừng/đếm tiếp giờ xem.
 * Không đọc nội dung màn hình (canRetrieveWindowContent = false).
 */
public class GuardService extends AccessibilityService {
    static volatile GuardService instance;
    static volatile String lastPkg = "";

    private static final long TICK_MS = 3000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HashMap<String, Boolean> appCache = new HashMap<>();
    private long cacheAt = 0L;
    private long lastHomeAt = 0L;
    private boolean wasParent = false;
    private boolean dreaming = false;
    private BroadcastReceiver receiver;
    private java.util.Set<String> imePackages = null;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                onTick();
            } catch (Throwable t) {
                Dbg.d("tick loi: " + t);
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Dbg.d("guard: da ket noi");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String a = i.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(a)) {
                    onScreen(true);
                } else if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                    onScreen(false);
                } else if (Intent.ACTION_DREAMING_STARTED.equals(a)) {
                    dreaming = true;
                } else if (Intent.ACTION_DREAMING_STOPPED.equals(a)) {
                    dreaming = false;
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_DREAMING_STARTED);
        f.addAction(Intent.ACTION_DREAMING_STOPPED);
        registerReceiver(receiver, f);
        wasParent = Prefs.parentActive(this);
        onScreen(isInteractive());
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, TICK_MS);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        cleanup();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    private void cleanup() {
        instance = null;
        handler.removeCallbacksAndMessages(null);
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Throwable ignored) {
                // đã gỡ
            }
            receiver = null;
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence p = e.getPackageName();
        if (p == null) {
            return;
        }
        String pkg = p.toString();
        CharSequence cn = e.getClassName();
        lastPkg = pkg;
        Dbg.d("cua so: " + pkg + " / " + (cn == null ? "" : cn));
        try {
            evaluate(pkg);
        } catch (Throwable t) {
            Dbg.d("evaluate loi: " + t);
        }
    }

    private void evaluate(String pkg) {
        if (Prefs.parentActive(this)) {
            return;
        }
        if (pkg.equals(getPackageName())) {
            return;
        }
        int st = Session.state(this);
        if (Kids.isKidApp(pkg)) {
            if (st == Session.LOCKED) {
                goHome("YouTube khi dang khoa");
            }
            return;
        }
        if (Kids.ALWAYS_OK.contains(pkg) || isInputMethod(pkg)) {
            return;
        }
        if (Kids.DENY.contains(pkg) || isApp(pkg)) {
            goHome("chan " + pkg);
        }
    }

    private void onScreen(boolean on) {
        Dbg.d("man hinh " + (on ? "bat" : "tat"));
        if (on) {
            Session.resume(this);
            if (Session.checkExpired(this) && !Prefs.parentActive(this)) {
                Home.bringToFront(this, "het gio khi bat man hinh");
            }
        } else {
            Session.pause(this);
        }
    }

    private void onTick() {
        boolean expired = Session.checkExpired(this);
        boolean parent = Prefs.parentActive(this);
        if (wasParent && !parent) {
            wasParent = false;
            Prefs.setPassThrough(this, false);
            goHome("het che do phu huynh");
            return;
        }
        wasParent = parent;
        if (parent || dreaming || !isInteractive()) {
            return;
        }
        if (Session.state(this) == Session.LOCKED && !MainActivity.resumed) {
            goHome(expired ? "het gio" : "dang khoa");
        }
    }

    private boolean isInteractive() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm == null || pm.isInteractive();
    }

    private void goHome(String why) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastHomeAt < 800L) {
            return;
        }
        lastHomeAt = now;
        Dbg.d("goHome: " + why + " (cuoi=" + lastPkg + ")");
        if (Kids.weAreDefaultHome(this)) {
            performGlobalAction(GLOBAL_ACTION_HOME);
        } else {
            Home.bringToFront(this, why);
        }
    }

    /** Gói có biểu tượng mở được từ màn hình chính (launcher thường hoặc leanback). */
    private boolean isApp(String pkg) {
        long now = SystemClock.elapsedRealtime();
        if (now - cacheAt > 10 * 60_000L) {
            appCache.clear();
            imePackages = null;
            cacheAt = now;
        }
        Boolean b = appCache.get(pkg);
        if (b != null) {
            return b;
        }
        PackageManager pm = getPackageManager();
        boolean r = pm.getLaunchIntentForPackage(pkg) != null || pm.getLeanbackLaunchIntentForPackage(pkg) != null;
        appCache.put(pkg, r);
        return r;
    }

    private boolean isInputMethod(String pkg) {
        if (imePackages == null) {
            java.util.Set<String> s = new java.util.HashSet<>();
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                List<InputMethodInfo> l = imm == null ? null : imm.getInputMethodList();
                if (l != null) {
                    for (InputMethodInfo i : l) {
                        s.add(i.getPackageName());
                    }
                }
            } catch (Throwable t) {
                Dbg.d("ime loi: " + t);
            }
            imePackages = s;
        }
        return imePackages.contains(pkg);
    }
}
