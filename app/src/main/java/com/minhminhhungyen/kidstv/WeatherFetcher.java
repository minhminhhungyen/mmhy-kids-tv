package com.minhminhhungyen.kidstv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Tải thời tiết Yên Mỹ (Hưng Yên) từ Open-Meteo, lưu bản gần nhất để TV mất mạng vẫn hiện được. */
final class WeatherFetcher {
    private WeatherFetcher() {
    }

    static final String URL_TEXT = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=20.87&longitude=106.02"
            + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,is_day"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
            + "&timezone=Asia%2FBangkok&forecast_days=3";

    private static volatile boolean busy = false;
    private static volatile long lastTry = 0L;

    static boolean stale(Context c, long maxAgeMs) {
        long now = System.currentTimeMillis();
        if (now - lastTry < 300_000L) {
            return false;
        }
        return now - Prefs.wxTime(c) > maxAgeMs;
    }

    static void refreshAsync(final Context ctx, final Runnable onDone) {
        if (busy) {
            return;
        }
        busy = true;
        lastTry = System.currentTimeMillis();
        final Context app = ctx.getApplicationContext();
        Thread t = new Thread(() -> {
            try {
                String json = http(URL_TEXT);
                if (WeatherData.parse(json) != null) {
                    Prefs.setWx(app, json, System.currentTimeMillis());
                    Dbg.d("thoi tiet: da cap nhat");
                } else {
                    Dbg.d("thoi tiet: khong doc duoc du lieu");
                }
            } catch (Throwable e) {
                Dbg.d("thoi tiet loi: " + e);
            } finally {
                busy = false;
                if (onDone != null) {
                    new Handler(Looper.getMainLooper()).post(onDone);
                }
            }
        }, "weather");
        t.setDaemon(true);
        t.start();
    }

    private static String http(String u) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
        try {
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "MMHY-KidsTV/1.0");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IOException("HTTP " + code);
            }
            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > 200_000) {
                        throw new IOException("qua lon");
                    }
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            conn.disconnect();
        }
    }
}
