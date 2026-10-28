package com.minhminhhungyen.kidstv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

/**
 * Màn hình chính của TV ở chế độ trẻ em (HOME): chỉ có một ô YouTube, đồng hồ, thời tiết, lịch âm dương.
 * Hết giờ → màn hình khóa với bài toán; mật khẩu phụ huynh (mặc định = ngày hôm nay ddMMyyyy) mở Menu phụ huynh.
 */
public class MainActivity extends Activity {
    /** Màn hình chính đang hiện trên cùng (GuardService dùng để biết có cần kéo về hay không). */
    static volatile boolean resumed = false;

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static final int S_HOME = 0;
    private static final int S_LOCK = 1;
    private static final int S_PWD = 2;
    private static final int S_PARENT = 3;

    private static final int M_CHECK = 0;
    private static final int M_NEW = 1;
    private static final int M_CONFIRM = 2;

    private static final int[] SESSION_OPTS = {15, 20, 30, 45, 60};
    private static final int[] BONUS_OPTS = {5, 10, 15, 20, 30};
    private static final int[] STREAK_OPTS = {5, 10, 15, 20, 30};
    private static final int[] CAP_OPTS = {0, 1, 2, 3, 5};
    private static final int[] GATE_OPTS = {0, 5, 10, 15, 20};
    private static final String[] DAYS = {"Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private final MathGame game = new MathGame(rnd);

    private int screen = -1;
    private Intent lastHandled = null;
    private long lastInput = 0L;
    private int shownDay = 0;

    private FrameLayout stage;
    private View vHome;
    private View vLock;
    private View vPwd;
    private View vParent;

    // Màn hình chính
    private TextView tvClock;
    private TextView tvDate;
    private TextView tvLunar;
    private WeatherIcon wxIcon;
    private TextView tvWxTemp;
    private TextView tvWxDesc;
    private TextView tvWxExtra;
    private final TextView[] fcName = new TextView[3];
    private final WeatherIcon[] fcIcon = new WeatherIcon[3];
    private final TextView[] fcRange = new TextView[3];
    private MonthView month;
    private View tile;
    private TextView tvTileTitle;
    private TextView tvTileSub;
    private TextView tvRemain;
    private TextView btnParentHome;

    // Màn hình khóa (bài toán)
    private TextView tvLockTitle;
    private TextView tvLockSub;
    private TextView tvProgress;
    private TextView tvQuestion;
    private TextView tvFeedback;
    private TextView btnParentLock;
    private View barFill;
    private View mathBox;
    private final TextView[] opt = new TextView[4];
    private MathGame.Problem problem;
    private boolean answering = false;
    private long acceptAfter = 0L;

    // Nhập mật khẩu
    private TextView tvPwdTitle;
    private TextView tvPwdMsg;
    private final View[] dots = new View[8];
    private final StringBuilder pwdBuf = new StringBuilder();
    private int pwdMode = M_CHECK;
    private String pwdFirst = "";

    // Menu phụ huynh
    private LinearLayout parentList;
    private ScrollView parentScroll;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!resumed) {
                return;
            }
            try {
                onTick();
            } catch (Throwable t) {
                Dbg.d("tick UI loi: " + t);
            }
            ui.postDelayed(this, 1000L);
        }
    };

    // ======================================================================================
    // Vòng đời
    // ======================================================================================

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        ImageView bg = new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bmp = loadWallpaper();
        if (bmp != null) {
            bg.setImageBitmap(bmp);
        } else {
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFFEAF6EF, 0xFFBFE3CC, 0xFF0B6B35});
            bg.setBackground(g);
        }
        root.addView(bg, new FrameLayout.LayoutParams(MATCH, MATCH));

        stage = new FrameLayout(this);
        root.addView(stage, new FrameLayout.LayoutParams(Ui.u(960), Ui.u(540), Gravity.CENTER));

        vHome = buildHome();
        vLock = buildLock();
        vPwd = buildPwd();
        vParent = buildParent();
        View[] all = {vHome, vLock, vPwd, vParent};
        for (View v : all) {
            v.setVisibility(View.GONE);
            stage.addView(v, new FrameLayout.LayoutParams(MATCH, MATCH));
        }
        setContentView(root);
        Dbg.d("onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        lastInput = System.currentTimeMillis();
        GuardEnabler.ensure(this);
        Session.rearm(this);
        enter(getIntent());
        if (WeatherFetcher.stale(this, 20 * 60_000L)) {
            WeatherFetcher.refreshAsync(this, () -> {
                if (resumed && screen == S_HOME) {
                    shownDay = 0;
                    refreshHome();
                }
            });
        }
        ui.removeCallbacks(tick);
        ui.post(tick);
    }

    @Override
    protected void onPause() {
        resumed = false;
        ui.removeCallbacks(tick);
        if (screen == S_PWD || screen == S_PARENT) {
            screen = -1;
        }
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        if (resumed) {
            enter(i);
        }
    }

    @Override
    public void onUserInteraction() {
        lastInput = System.currentTimeMillis();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (screen == S_PWD) {
            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                pwdDigit((char) ('0' + keyCode - KeyEvent.KEYCODE_0));
                return true;
            }
            if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
                pwdDigit((char) ('0' + keyCode - KeyEvent.KEYCODE_NUMPAD_0));
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                pwdBackspace();
                return true;
            }
        }
        return super.onKeyDown(keyCode, e);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (screen == S_PWD) {
            if (pwdMode != M_CHECK) {
                showParent();
            } else {
                showHomeOrLock();
            }
            return;
        }
        if (screen == S_PARENT) {
            showHomeOrLock();
        }
        // Màn hình chính và màn hình khóa: Back không làm gì.
    }

    // ======================================================================================
    // Điều hướng
    // ======================================================================================

    private void enter(Intent i) {
        boolean fresh = i != lastHandled;
        lastHandled = i;
        boolean home = i != null && i.hasCategory(Intent.CATEGORY_HOME);
        if (fresh && home && (screen == S_PWD || screen == S_PARENT)) {
            screen = -1;
        }
        if (fresh && home && Prefs.parentActive(this) && Prefs.passThrough(this)) {
            if (Kids.startStockHome(this)) {
                return;
            }
        }
        route();
    }

    private void route() {
        Session.checkExpired(this);
        int st = Session.state(this);
        long now = System.currentTimeMillis();
        if (st == Session.LOCKED && Prefs.freeDaily(this) && Prefs.freeDay(this) != Prefs.dayKey(now)
                && !Prefs.parentActive(this)) {
            Session.grantMinutes(this, Prefs.sessionMin(this), "mien phi dau ngay");
            st = Session.state(this);
        }
        if (screen == S_PWD || screen == S_PARENT) {
            return;
        }
        if (st == Session.LOCKED) {
            showLock();
        } else {
            showHome();
        }
    }

    private void showHomeOrLock() {
        screen = -1;
        route();
    }

    private void show(int s) {
        screen = s;
        vHome.setVisibility(s == S_HOME ? View.VISIBLE : View.GONE);
        vLock.setVisibility(s == S_LOCK ? View.VISIBLE : View.GONE);
        vPwd.setVisibility(s == S_PWD ? View.VISIBLE : View.GONE);
        vParent.setVisibility(s == S_PARENT ? View.VISIBLE : View.GONE);
        lastInput = System.currentTimeMillis();
    }

    private void showHome() {
        show(S_HOME);
        shownDay = 0;
        refreshHome();
        tile.requestFocus();
    }

    /** Đang khóa vì TV vừa tắt/bật (chờ giải toán để bắt đầu một phiên mới), không phải vì hết giờ. */
    private boolean gateMode() {
        return Prefs.gate(this) && Prefs.gateCount(this) > 0;
    }

    /** Số câu đúng liên tiếp cần đạt ở màn hình khóa hiện tại. */
    private int goal() {
        return gateMode() ? Prefs.gateCount(this) : Math.max(1, Prefs.streakGoal(this));
    }

    private void showLock() {
        boolean wasLock = screen == S_LOCK;
        show(S_LOCK);
        final boolean gate = gateMode();
        // Lượt thưởng mỗi ngày chỉ áp dụng cho khóa hết giờ; toán khi bật TV không bị giới hạn bởi nó.
        boolean capped = !gate && Prefs.bonusCap(this) > 0 && Prefs.bonusToday(this) >= Prefs.bonusCap(this);
        int goal = goal();
        int bonus = Prefs.bonusMin(this);
        tvLockTitle.setText(gate ? "Chào bé! Làm toán rồi mới xem nhé!" : "Hết giờ xem rồi!");
        btnParentLock.setText(parentLabel());
        if (gate) {
            tvLockSub.setText("Giải toán đúng liên tiếp " + goal + " câu để được xem " + Prefs.sessionMin(this) + " phút");
            mathBox.setVisibility(View.VISIBLE);
            if (problem == null) {
                newProblem();
            }
            updateProgress();
        } else if (capped) {
            tvLockSub.setText("Hôm nay bé đã hết lượt thưởng rồi. Hẹn bé ngày mai nhé!");
            mathBox.setVisibility(View.GONE);
        } else {
            tvLockSub.setText("Giải toán đúng liên tiếp " + goal + " câu để được xem thêm " + bonus + " phút");
            mathBox.setVisibility(View.VISIBLE);
            if (problem == null) {
                newProblem();
            }
            updateProgress();
        }
        killKids();
        if (!wasLock) {
            if (capped) {
                btnParentLock.requestFocus();
            } else {
                opt[1].requestFocus();
            }
        }
    }

    private void killKids() {
        Kids.kill(this);
        ui.postDelayed(() -> Kids.kill(MainActivity.this), 900L);
        ui.postDelayed(() -> Kids.kill(MainActivity.this), 3500L);
    }

    private void showPwd(int mode) {
        pwdMode = mode;
        pwdFirst = "";
        clearPwd();
        if (mode == M_CHECK) {
            tvPwdTitle.setText("Nhập mật khẩu phụ huynh");
            pwdMsg(PasswordPolicy.hasCustom(this) ? "" : "Mật khẩu mặc định: ngày hôm nay (ddMMyyyy)");
        } else if (mode == M_NEW) {
            tvPwdTitle.setText("Mật khẩu cố định mới (4–8 số)");
            pwdMsg("Gõ xong bấm OK");
        } else {
            tvPwdTitle.setText("Nhập lại mật khẩu mới");
            pwdMsg("Gõ xong bấm OK");
        }
        show(S_PWD);
    }

    private void showParent() {
        buildParentRows(0);
        show(S_PARENT);
    }

    private void onTick() {
        long now = System.currentTimeMillis();
        if (Session.checkExpired(this) && screen != S_PWD && screen != S_PARENT) {
            route();
        } else if (screen == S_LOCK && Session.state(this) != Session.LOCKED) {
            route(); // được cấp giờ từ nơi khác (ví dụ lệnh adb) thì chuyển về màn hình chính
        }
        if (screen == S_HOME) {
            refreshHome();
            if (WeatherFetcher.stale(this, 30 * 60_000L)) {
                WeatherFetcher.refreshAsync(this, () -> {
                    if (resumed && screen == S_HOME) {
                        shownDay = 0;
                        refreshHome();
                    }
                });
            }
        }
        if ((screen == S_PARENT || screen == S_PWD) && now - lastInput > 90_000L) {
            showHomeOrLock();
        }
    }

    // ======================================================================================
    // Màn hình chính
    // ======================================================================================

    private View buildHome() {
        FrameLayout f = new FrameLayout(this);

        // Thẻ trái: đồng hồ, ngày dương, ngày âm, thời tiết (thu nhỏ theo hệ số S cho gọn)
        final float S = 0.85f;
        LinearLayout left = vbox();
        left.setBackground(Ui.cardBg());
        left.setPadding(Ui.u(16 * S), Ui.u(8 * S), Ui.u(16 * S), Ui.u(8 * S));
        tvClock = Ui.tv(this, "00:00", 50 * S, Ui.GREEN, true);
        left.addView(tvClock);
        tvDate = Ui.tv(this, "", 18 * S, Ui.INK, true);
        left.addView(tvDate);
        tvLunar = Ui.tv(this, "", 15 * S, Ui.ORANGE, true);
        left.addView(tvLunar);
        View div = new View(this);
        div.setBackgroundColor(0x33000000);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(MATCH, Math.max(1, Ui.u(1)));
        dlp.topMargin = Ui.u(6 * S);
        dlp.bottomMargin = Ui.u(3 * S);
        left.addView(div, dlp);
        left.addView(Ui.tv(this, "Thời tiết Yên Mỹ, Hưng Yên", 10, Ui.MUTED, false));
        LinearLayout wx = new LinearLayout(this);
        wx.setOrientation(LinearLayout.HORIZONTAL);
        wx.setGravity(Gravity.CENTER_VERTICAL);
        wxIcon = new WeatherIcon(this);
        wx.addView(wxIcon, new LinearLayout.LayoutParams(Ui.u(50 * S), Ui.u(50 * S)));
        LinearLayout col = vbox();
        col.setPadding(Ui.u(8 * S), 0, 0, 0);
        tvWxTemp = Ui.tv(this, "--°C", 30 * S, Ui.INK, true);
        tvWxDesc = Ui.tv(this, "Đang tải…", 14 * S, Ui.INK, false);
        col.addView(tvWxTemp);
        col.addView(tvWxDesc);
        wx.addView(col);
        left.addView(wx);
        tvWxExtra = Ui.tv(this, "", 10.5f, Ui.MUTED, false);
        left.addView(tvWxExtra);
        LinearLayout fc = new LinearLayout(this);
        fc.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams fclp = new LinearLayout.LayoutParams(MATCH, WRAP);
        fclp.topMargin = Ui.u(4 * S);
        for (int i = 0; i < 3; i++) {
            LinearLayout cell = vbox();
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            fcName[i] = Ui.tv(this, "", 10, Ui.MUTED, true);
            fcName[i].setGravity(Gravity.CENTER);
            fcIcon[i] = new WeatherIcon(this);
            fcRange[i] = Ui.tv(this, "", 10.5f, Ui.INK, true);
            fcRange[i].setGravity(Gravity.CENTER);
            cell.addView(fcName[i], new LinearLayout.LayoutParams(MATCH, WRAP));
            cell.addView(fcIcon[i], new LinearLayout.LayoutParams(Ui.u(28 * S), Ui.u(28 * S)));
            cell.addView(fcRange[i], new LinearLayout.LayoutParams(MATCH, WRAP));
            fc.addView(cell, new LinearLayout.LayoutParams(0, WRAP, 1f));
        }
        left.addView(fc, fclp);
        FrameLayout.LayoutParams leftLp = fl(36, 26, Math.round(276 * S), 0);
        leftLp.height = WRAP; // cao theo nội dung để không cắt dòng dự báo khi phông chữ của TV khác
        f.addView(left, leftLp);

        // Thẻ phải: lịch âm dương (cùng hệ số S, sát lề phải)
        FrameLayout right = new FrameLayout(this);
        right.setBackground(Ui.cardBg());
        right.setPadding(Ui.u(8 * S), Ui.u(6 * S), Ui.u(8 * S), Ui.u(6 * S));
        month = new MonthView(this);
        right.addView(month, new FrameLayout.LayoutParams(MATCH, MATCH));
        int calW = Math.round(240 * S);
        f.addView(right, fl(960 - 36 - calW, 26, calW, Math.round(270 * S)));

        // Ô duy nhất: YouTube (hồ sơ của bé do phụ huynh cài trong ứng dụng YouTube)
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.HORIZONTAL);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(Ui.u(18), Ui.u(12), Ui.u(18), Ui.u(12));
        t.setFocusable(true);
        t.setClickable(true);
        Ui.styleFocusable(t, Ui.rounded(Ui.CARD, 22, 0x88FFFFFF, 2), Ui.rounded(Ui.WHITE, 22, Ui.ORANGE, 5));
        t.addView(new PlayBadge(this), new LinearLayout.LayoutParams(Ui.u(66), Ui.u(66)));
        LinearLayout txt = vbox();
        txt.setPadding(Ui.u(14), 0, 0, 0);
        tvTileTitle = Ui.tv(this, "YouTube", 26, Ui.INK, true);
        tvTileSub = Ui.tv(this, "Nhấn OK để xem", 15, Ui.MUTED, false);
        txt.addView(tvTileTitle);
        txt.addView(tvTileSub);
        t.addView(txt, new LinearLayout.LayoutParams(0, WRAP, 1f));
        t.setOnClickListener(v -> onTile());
        tile = t;
        f.addView(t, fl(330, 350, 300, 112));

        tvRemain = Ui.tv(this, "", 16, Ui.GREEN_D, true);
        tvRemain.setGravity(Gravity.CENTER);
        tvRemain.setBackground(Ui.rounded(Ui.CARD, 14, 0, 0));
        tvRemain.setPadding(Ui.u(10), Ui.u(3), Ui.u(10), Ui.u(3));
        f.addView(tvRemain, fl(380, 474, 200, 28));

        btnParentHome = Ui.button(this, "Phụ huynh", 14);
        btnParentHome.setOnClickListener(v -> onParentButton());
        f.addView(btnParentHome, fl(36, 480, 170, 36));
        return f;
    }

    /** Nút "Phụ huynh": nhập mật khẩu; nếu đang ở chế độ phụ huynh thì là nút thoát chế độ đó. */
    private void onParentButton() {
        if (Prefs.parentActive(this)) {
            Prefs.setParentUntil(this, 0L);
            Prefs.setPassThrough(this, false);
            Home.toast(this, "Đã thoát chế độ phụ huynh");
            showHomeOrLock();
        } else {
            showPwd(M_CHECK);
        }
    }

    private String parentLabel() {
        return Prefs.parentActive(this) ? "Thoát phụ huynh" : "Phụ huynh";
    }

    private void onTile() {
        if (Session.state(this) == Session.LOCKED) {
            route();
            return;
        }
        if (!Kids.launch(this)) {
            Home.toast(this, "Chưa có YouTube trên TV này. Nhờ bố mẹ cài đặt nhé.");
        }
    }

    private void refreshHome() {
        Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH) + 1;
        int d = cal.get(Calendar.DAY_OF_MONTH);
        setIf(tvClock, String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
        int key = y * 10000 + m * 100 + d;
        if (key != shownDay) {
            shownDay = key;
            setIf(tvDate, DAYS[cal.get(Calendar.DAY_OF_WEEK) - 1] + ", " + d + "/" + m + "/" + y);
            LunarCalendar.Lunar l = LunarCalendar.solarToLunar(d, m, y);
            setIf(tvLunar, "Âm lịch: " + l.day + "/" + l.month + (l.leap ? " nhuận" : "") + " · năm " + LunarCalendar.canChiYear(l.year));
            month.setDate(y, m, d);
            refreshWeather();
        }
        boolean installed = Kids.installed(this);
        setIf(tvTileTitle, installed ? "YouTube" : "Chưa có YouTube");
        setIf(tvTileSub, installed ? "Nhấn OK để xem" : "Nhờ bố mẹ cài đặt");
        int st = Session.state(this);
        long rem = Session.remainingMs(this);
        setIf(tvRemain, st == Session.PAUSED ? "Tạm dừng · còn " + fmt(rem) : "Còn " + fmt(rem));
        setIf(btnParentHome, parentLabel());
    }

    /** Chỉ đặt chữ khi thật sự đổi: đồng hồ chạy mỗi giây không được làm TV phát sự kiện trợ năng liên tục. */
    private static void setIf(TextView t, CharSequence s) {
        if (!TextUtils.equals(t.getText(), s)) {
            t.setText(s);
        }
    }

    private void refreshWeather() {
        WeatherData w = WeatherData.parse(Prefs.wxJson(this));
        if (w == null) {
            wxIcon.setWeather(3, true);
            tvWxTemp.setText("--°C");
            tvWxDesc.setText(Prefs.wxTime(this) == 0L ? "Đang tải…" : "Chưa có dữ liệu");
            tvWxExtra.setText("");
            for (int i = 0; i < 3; i++) {
                fcName[i].setText("");
                fcRange[i].setText("");
                fcIcon[i].setVisibility(View.INVISIBLE);
            }
            return;
        }
        wxIcon.setWeather(w.code, w.isDay);
        tvWxTemp.setText(Double.isNaN(w.temp) ? "--°C" : Math.round(w.temp) + "°C");
        tvWxDesc.setText(WeatherData.text(w.code));
        StringBuilder sb = new StringBuilder();
        if (!Double.isNaN(w.feels)) {
            sb.append("Cảm giác ").append(Math.round(w.feels)).append("°");
        }
        if (w.humidity >= 0) {
            sb.append(sb.length() > 0 ? " · " : "").append("Ẩm ").append(w.humidity).append("%");
        }
        if (w.days > 0 && w.rain[0] >= 0) {
            sb.append(sb.length() > 0 ? " · " : "").append("Mưa ").append(w.rain[0]).append("%");
        }
        tvWxExtra.setText(sb.toString());
        String[] names = {"Hôm nay", "Ngày mai", "Ngày kia"};
        for (int i = 0; i < 3; i++) {
            if (i < w.days && !Double.isNaN(w.max[i])) {
                fcName[i].setText(names[i]);
                fcIcon[i].setWeather(w.dcode[i], true);
                fcIcon[i].setVisibility(View.VISIBLE);
                fcRange[i].setText(Double.isNaN(w.min[i])
                        ? Math.round(w.max[i]) + "°"
                        : Math.round(w.min[i]) + "–" + Math.round(w.max[i]) + "°");
            } else {
                fcName[i].setText("");
                fcRange[i].setText("");
                fcIcon[i].setVisibility(View.INVISIBLE);
            }
        }
    }

    private static String fmt(long ms) {
        if (ms >= 60_000L) {
            return ((ms + 59_999L) / 60_000L) + " phút";
        }
        return (ms / 1000L) + " giây";
    }

    // ======================================================================================
    // Màn hình khóa + bài toán
    // ======================================================================================

    private View buildLock() {
        FrameLayout f = new FrameLayout(this);
        LinearLayout card = vbox();
        card.setBackground(Ui.cardBg());
        card.setPadding(Ui.u(24), Ui.u(14), Ui.u(24), Ui.u(14));
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        tvLockTitle = Ui.tv(this, "Hết giờ xem rồi!", 34, Ui.ORANGE, true);
        tvLockTitle.setGravity(Gravity.CENTER);
        card.addView(tvLockTitle, new LinearLayout.LayoutParams(MATCH, WRAP));
        tvLockSub = Ui.tv(this, "", 17, Ui.INK, false);
        tvLockSub.setGravity(Gravity.CENTER);
        card.addView(tvLockSub, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout box = vbox();
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        mathBox = box;

        tvProgress = Ui.tv(this, "", 18, Ui.GREEN, true);
        tvProgress.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(MATCH, WRAP);
        plp.topMargin = Ui.u(12);
        box.addView(tvProgress, plp);

        FrameLayout track = new FrameLayout(this);
        track.setBackground(Ui.rounded(Ui.TRACK, 8, 0, 0));
        barFill = new View(this);
        barFill.setBackground(Ui.rounded(Ui.GREEN, 8, 0, 0));
        track.addView(barFill, new FrameLayout.LayoutParams(0, MATCH));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(Ui.u(600), Ui.u(14));
        tlp.topMargin = Ui.u(6);
        box.addView(track, tlp);

        tvQuestion = Ui.tv(this, "", 68, Ui.INK, true);
        tvQuestion.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        qlp.topMargin = Ui.u(14);
        box.addView(tvQuestion, qlp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            TextView b = Ui.button(this, "", 36);
            b.setOnClickListener(v -> onOption(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.u(150), Ui.u(72));
            lp.leftMargin = Ui.u(8);
            lp.rightMargin = Ui.u(8);
            row.addView(b, lp);
            opt[i] = b;
        }
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        rlp.topMargin = Ui.u(10);
        box.addView(row, rlp);

        tvFeedback = Ui.tv(this, " ", 21, Ui.OK, true);
        tvFeedback.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(MATCH, WRAP);
        flp.topMargin = Ui.u(10);
        box.addView(tvFeedback, flp);

        card.addView(box, new LinearLayout.LayoutParams(MATCH, WRAP));

        View spacer = new View(this);
        card.addView(spacer, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        btnParentLock = Ui.button(this, "Phụ huynh", 14);
        btnParentLock.setOnClickListener(v -> onParentButton());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.u(170), Ui.u(36));
        blp.gravity = Gravity.START;
        card.addView(btnParentLock, blp);

        f.addView(card, fl(110, 34, 740, 472));
        return f;
    }

    private void newProblem() {
        problem = game.next();
        tvQuestion.setText(problem.text);
        for (int i = 0; i < 4; i++) {
            opt[i].setText(String.valueOf(problem.options[i]));
        }
        tvFeedback.setText(" ");
        answering = false;
        acceptAfter = System.currentTimeMillis() + 350L;
    }

    private void onOption(int idx) {
        if (problem == null || answering || System.currentTimeMillis() < acceptAfter) {
            return;
        }
        answering = true;
        boolean ok = idx == problem.correctIndex;
        int goal = goal();
        if (ok) {
            int s = Prefs.streak(this) + 1;
            Prefs.setStreak(this, s);
            updateProgress();
            if (s >= goal) {
                reward();
            } else {
                feedback("Đúng rồi! Giỏi quá!", Ui.OK);
                ui.postDelayed(this::afterAnswer, 650L);
            }
        } else {
            Prefs.setStreak(this, 0);
            updateProgress();
            feedback("Chưa đúng. Đáp án là " + problem.answer + ". Mình làm lại từ đầu nhé!", Ui.RED);
            ui.postDelayed(this::afterAnswer, 2600L);
        }
    }

    private void afterAnswer() {
        if (screen == S_LOCK) {
            newProblem();
            opt[1].requestFocus();
        } else {
            answering = false;
        }
    }

    private void reward() {
        // Toán khi bật TV: một phiên mới đầy đủ (không tính là lượt thưởng). Toán khi hết giờ: thưởng thêm.
        final boolean gate = gateMode();
        final int minutes = gate ? Prefs.sessionMin(this) : Prefs.bonusMin(this);
        feedback(gate ? "Tuyệt vời! Bé được xem " + minutes + " phút!" : "Tuyệt vời! Bé được xem thêm " + minutes + " phút!", Ui.OK);
        ui.postDelayed(() -> {
            if (Session.state(MainActivity.this) != Session.LOCKED) {
                return;
            }
            if (!gate) {
                Prefs.addBonusToday(MainActivity.this);
            }
            Session.grantMinutes(MainActivity.this, minutes, gate ? "giai toan khi bat TV" : "giai toan");
            problem = null;
            screen = -1;
            showHome();
            Kids.launch(MainActivity.this);
        }, 1700L);
    }

    private void feedback(String s, int color) {
        tvFeedback.setText(s);
        tvFeedback.setTextColor(color);
    }

    private void updateProgress() {
        int goal = goal();
        int s = Math.min(goal, Prefs.streak(this));
        tvProgress.setText("Đúng liên tiếp: " + s + " / " + goal);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) barFill.getLayoutParams();
        lp.width = Math.round(Ui.u(600) * (s / (float) goal));
        barFill.setLayoutParams(lp);
    }

    // ======================================================================================
    // Nhập mật khẩu
    // ======================================================================================

    private View buildPwd() {
        FrameLayout f = new FrameLayout(this);
        LinearLayout card = vbox();
        card.setBackground(Ui.cardBg());
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.u(20), Ui.u(14), Ui.u(20), Ui.u(12));

        tvPwdTitle = Ui.tv(this, "Nhập mật khẩu phụ huynh", 24, Ui.INK, true);
        tvPwdTitle.setGravity(Gravity.CENTER);
        card.addView(tvPwdTitle, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout dotRow = new LinearLayout(this);
        dotRow.setOrientation(LinearLayout.HORIZONTAL);
        dotRow.setGravity(Gravity.CENTER);
        for (int i = 0; i < 8; i++) {
            View d = new View(this);
            d.setBackground(oval(Ui.TRACK));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.u(20), Ui.u(20));
            lp.leftMargin = Ui.u(6);
            lp.rightMargin = Ui.u(6);
            dotRow.addView(d, lp);
            dots[i] = d;
        }
        LinearLayout.LayoutParams drlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        drlp.topMargin = Ui.u(10);
        card.addView(dotRow, drlp);

        tvPwdMsg = Ui.tv(this, " ", 15, Ui.RED, true);
        tvPwdMsg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        mlp.topMargin = Ui.u(8);
        mlp.bottomMargin = Ui.u(6);
        card.addView(tvPwdMsg, mlp);

        String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"Xóa", "0", "OK"}};
        for (String[] r : rows) {
            LinearLayout hr = new LinearLayout(this);
            hr.setOrientation(LinearLayout.HORIZONTAL);
            hr.setGravity(Gravity.CENTER);
            for (final String label : r) {
                TextView k = Ui.button(this, label, 26);
                k.setOnClickListener(v -> {
                    if ("Xóa".equals(label)) {
                        pwdBackspace();
                    } else if ("OK".equals(label)) {
                        pwdSubmit();
                    } else {
                        pwdDigit(label.charAt(0));
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.u(98), Ui.u(54));
                lp.setMargins(Ui.u(5), Ui.u(4), Ui.u(5), Ui.u(4));
                hr.addView(k, lp);
            }
            card.addView(hr, new LinearLayout.LayoutParams(MATCH, WRAP));
        }

        TextView back = Ui.button(this, "Quay lại", 16);
        back.setOnClickListener(v -> onBackPressed());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.u(160), Ui.u(38));
        blp.topMargin = Ui.u(8);
        card.addView(back, blp);

        f.addView(card, fl(240, 22, 480, 496));
        return f;
    }

    private void pwdDigit(char d) {
        if (pwdBuf.length() >= PasswordPolicy.MAX_LEN) {
            return;
        }
        pwdBuf.append(d);
        pwdRefresh();
        if (pwdMode == M_CHECK && pwdBuf.length() == PasswordPolicy.expectedLength(this)) {
            pwdSubmit();
        }
    }

    private void pwdBackspace() {
        if (pwdBuf.length() > 0) {
            pwdBuf.setLength(pwdBuf.length() - 1);
            pwdRefresh();
        }
    }

    private void clearPwd() {
        pwdBuf.setLength(0);
        pwdRefresh();
    }

    private void pwdRefresh() {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackground(oval(i < pwdBuf.length() ? Ui.GREEN : Ui.TRACK));
        }
    }

    private void pwdMsg(String s) {
        tvPwdMsg.setText(s.isEmpty() ? " " : s);
    }

    private void pwdSubmit() {
        String in = pwdBuf.toString();
        long now = System.currentTimeMillis();
        if (pwdMode == M_CHECK) {
            long lu = Prefs.lockUntil(this);
            if (now < lu) {
                pwdMsg("Thử lại sau " + ((lu - now + 999L) / 1000L) + " giây");
                clearPwd();
                return;
            }
            if (PasswordPolicy.check(this, in)) {
                Prefs.setFailCount(this, 0);
                showParent();
                return;
            }
            int f = Prefs.failCount(this) + 1;
            if (f >= 5) {
                Prefs.setFailCount(this, 0);
                Prefs.setLockUntil(this, now + 60_000L);
                pwdMsg("Sai nhiều lần. Chờ 60 giây rồi thử lại.");
            } else {
                Prefs.setFailCount(this, f);
                pwdMsg("Sai mật khẩu (" + f + "/5)");
            }
            clearPwd();
        } else if (pwdMode == M_NEW) {
            if (in.length() < PasswordPolicy.MIN_LEN) {
                pwdMsg("Cần ít nhất " + PasswordPolicy.MIN_LEN + " số");
                return;
            }
            pwdFirst = in;
            pwdMode = M_CONFIRM;
            tvPwdTitle.setText("Nhập lại mật khẩu mới");
            pwdMsg("Gõ xong bấm OK");
            clearPwd();
        } else {
            if (in.equals(pwdFirst)) {
                PasswordPolicy.setCustom(this, in);
                Home.toast(this, "Đã đặt mật khẩu cố định");
                showParent();
            } else {
                pwdMode = M_NEW;
                pwdFirst = "";
                tvPwdTitle.setText("Mật khẩu cố định mới (4–8 số)");
                pwdMsg("Hai lần không khớp, nhập lại");
                clearPwd();
            }
        }
    }

    // ======================================================================================
    // Menu phụ huynh
    // ======================================================================================

    private View buildParent() {
        FrameLayout f = new FrameLayout(this);
        LinearLayout card = vbox();
        card.setBackground(Ui.cardBg());
        card.setPadding(Ui.u(22), Ui.u(8), Ui.u(22), Ui.u(8));

        TextView title = Ui.tv(this, "Menu phụ huynh", 26, Ui.GREEN, true);
        card.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        parentScroll = new ScrollView(this);
        parentScroll.setFillViewport(true);
        parentList = vbox();
        parentScroll.addView(parentList, new FrameLayout.LayoutParams(MATCH, WRAP));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(MATCH, 0, 1f);
        slp.topMargin = Ui.u(8);
        card.addView(parentScroll, slp);

        f.addView(card, fl(140, 10, 680, 520));
        return f;
    }

    private void buildParentRows(final int focusIndex) {
        parentList.removeAllViews();
        final Context c = this;
        addRow("Cho bé xem " + Prefs.sessionMin(c) + " phút", "Bắt đầu", () -> {
            Session.grantMinutes(c, Prefs.sessionMin(c), "phu huynh");
            Prefs.setParentUntil(c, 0L);
            problem = null;
            screen = -1;
            showHome();
            Kids.launch(c);
        });
        addRow("Thời gian mỗi lần xem", Prefs.sessionMin(c) + " phút", () -> {
            Prefs.setSessionMin(c, next(Prefs.sessionMin(c), SESSION_OPTS));
            buildParentRows(1);
        });
        addRow("Thưởng khi giải toán đúng", Prefs.bonusMin(c) + " phút", () -> {
            Prefs.setBonusMin(c, next(Prefs.bonusMin(c), BONUS_OPTS));
            buildParentRows(2);
        });
        addRow("Số câu toán khi hết giờ", Prefs.streakGoal(c) + " câu", () -> {
            Prefs.setStreakGoal(c, next(Prefs.streakGoal(c), STREAK_OPTS));
            buildParentRows(3);
        });
        int cap = Prefs.bonusCap(c);
        addRow("Giới hạn lượt thưởng mỗi ngày", cap == 0 ? "Không giới hạn" : cap + " lần", () -> {
            Prefs.setBonusCap(c, next(Prefs.bonusCap(c), CAP_OPTS));
            buildParentRows(4);
        });
        addRow("Phiên đầu tiên mỗi ngày miễn phí", Prefs.freeDaily(c) ? "Bật" : "Tắt", () -> {
            Prefs.setFreeDaily(c, !Prefs.freeDaily(c));
            buildParentRows(5);
        });
        int gc = Prefs.gateCount(c);
        addRow("Số câu toán mỗi lần bật TV", gc == 0 ? "Tắt" : gc + " câu", () -> {
            Prefs.setGateCount(c, next(Prefs.gateCount(c), GATE_OPTS));
            buildParentRows(6);
        });
        addRow("Mật khẩu phụ huynh", PasswordPolicy.hasCustom(c) ? "Cố định" : "Theo ngày (ddMMyyyy)", () -> showPwd(M_NEW));
        if (PasswordPolicy.hasCustom(c)) {
            addRow("Dùng lại mật khẩu theo ngày", "", () -> {
                PasswordPolicy.clearCustom(c);
                buildParentRows(7);
            });
        }
        addRow("Mở Cài đặt TV", "30 phút", () -> {
            Prefs.setParentUntil(c, System.currentTimeMillis() + 30 * 60_000L);
            Prefs.setPassThrough(c, false);
            if (!Kids.startSettings(c)) {
                Home.toast(c, "Không mở được Cài đặt");
            }
        });
        addRow("Dùng TV bình thường", "60 phút", () -> {
            Prefs.setParentUntil(c, System.currentTimeMillis() + 60 * 60_000L);
            Prefs.setPassThrough(c, true);
            if (!Kids.startStockHome(c)) {
                Home.toast(c, "Không mở được màn hình gốc của TV");
            }
        });
        addRow("Khóa TV ngay", "", () -> {
            Session.expire(c, "phu huynh khoa");
            Prefs.setParentUntil(c, 0L);
            Prefs.setPassThrough(c, false);
            problem = null;
            showHomeOrLock();
        });
        addRow("Quay lại", "", this::showHomeOrLock);
        final int fi = Math.max(0, Math.min(focusIndex, parentList.getChildCount() - 1));
        parentList.post(() -> {
            View v = parentList.getChildAt(fi);
            if (v != null) {
                v.requestFocus();
            }
        });
    }

    private void addRow(String label, String value, final Runnable action) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.u(16), Ui.u(4), Ui.u(16), Ui.u(4));
        r.setFocusable(true);
        r.setClickable(true);
        Ui.styleFocusable(r, Ui.rounded(0xFFF1F7F3, 12, 0xFFD2E3D8, 1.5f), Ui.rounded(Ui.ORANGE, 12, Ui.WHITE, 3));
        final TextView l = Ui.tv(this, label, 18, Ui.INK, true);
        final TextView v = Ui.tv(this, value, 16, Ui.GREEN, true);
        r.addView(l, new LinearLayout.LayoutParams(0, WRAP, 1f));
        r.addView(v, new LinearLayout.LayoutParams(WRAP, WRAP));
        r.setOnFocusChangeListener((view, has) -> {
            l.setTextColor(has ? Ui.WHITE : Ui.INK);
            v.setTextColor(has ? Ui.WHITE : Ui.GREEN);
        });
        r.setOnClickListener(x -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, Ui.u(34));
        lp.bottomMargin = Ui.u(4);
        parentList.addView(r, lp);
    }

    private static int next(int cur, int[] opts) {
        for (int i = 0; i < opts.length; i++) {
            if (opts[i] == cur) {
                return opts[(i + 1) % opts.length];
            }
        }
        return opts[0];
    }

    // ======================================================================================
    // Tiện ích dựng giao diện
    // ======================================================================================

    private LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private static FrameLayout.LayoutParams fl(int x, int y, int w, int h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.u(w), Ui.u(h));
        lp.leftMargin = Ui.u(x);
        lp.topMargin = Ui.u(y);
        lp.gravity = Gravity.TOP | Gravity.START;
        return lp;
    }

    private static GradientDrawable oval(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    private Bitmap loadWallpaper() {
        try {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int sample = Math.max(dm.widthPixels, dm.heightPixels) < 1300 ? 2 : 1;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            try (InputStream in = getAssets().open("bg_company.jpg")) {
                return BitmapFactory.decodeStream(in, null, o);
            }
        } catch (Throwable t) {
            Dbg.d("khong nap duoc hinh nen: " + t);
            return null;
        }
    }

    /** Nút phát đỏ của ô YouTube (vẽ bằng mã, không phụ thuộc phông chữ). */
    private static final class PlayBadge extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path tri = new Path();

        PlayBadge(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth();
            float h = getHeight();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFE53935);
            cv.drawCircle(w / 2f, h / 2f, Math.min(w, h) / 2f, p);
            p.setColor(Color.WHITE);
            tri.reset();
            tri.moveTo(w * 0.40f, h * 0.28f);
            tri.lineTo(w * 0.40f, h * 0.72f);
            tri.lineTo(w * 0.74f, h * 0.50f);
            tri.close();
            cv.drawPath(tri, p);
        }
    }
}
