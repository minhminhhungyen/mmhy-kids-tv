package com.minhminhhungyen.kidstv;

import org.json.JSONArray;
import org.json.JSONObject;

/** Dữ liệu thời tiết từ Open-Meteo (https://open-meteo.com), mã thời tiết theo chuẩn WMO. */
final class WeatherData {
    double temp = Double.NaN;
    double feels = Double.NaN;
    int humidity = -1;
    int code = -1;
    boolean isDay = true;
    int days = 0;
    final double[] max = {Double.NaN, Double.NaN, Double.NaN};
    final double[] min = {Double.NaN, Double.NaN, Double.NaN};
    final int[] dcode = {-1, -1, -1};
    final int[] rain = {-1, -1, -1};

    /** Trả về null nếu JSON không đọc được. Hiểu cả dạng "current" (mới) và "current_weather" (cũ). */
    static WeatherData parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(json);
            WeatherData w = new WeatherData();
            JSONObject cur = root.optJSONObject("current");
            if (cur != null) {
                w.temp = cur.optDouble("temperature_2m", Double.NaN);
                w.feels = cur.optDouble("apparent_temperature", Double.NaN);
                w.humidity = cur.optInt("relative_humidity_2m", -1);
                w.code = cur.optInt("weather_code", cur.optInt("weathercode", -1));
                w.isDay = cur.optInt("is_day", 1) == 1;
            } else {
                JSONObject cw = root.optJSONObject("current_weather");
                if (cw != null) {
                    w.temp = cw.optDouble("temperature", Double.NaN);
                    w.code = cw.optInt("weathercode", cw.optInt("weather_code", -1));
                    w.isDay = cw.optInt("is_day", 1) == 1;
                }
            }
            JSONObject d = root.optJSONObject("daily");
            if (d != null) {
                JSONArray mx = d.optJSONArray("temperature_2m_max");
                JSONArray mn = d.optJSONArray("temperature_2m_min");
                JSONArray wc = d.optJSONArray("weather_code");
                JSONArray pr = d.optJSONArray("precipitation_probability_max");
                int n = mx == null ? 0 : Math.min(3, mx.length());
                for (int i = 0; i < n; i++) {
                    w.max[i] = mx.optDouble(i, Double.NaN);
                    w.min[i] = mn == null ? Double.NaN : mn.optDouble(i, Double.NaN);
                    w.dcode[i] = wc == null ? -1 : wc.optInt(i, -1);
                    w.rain[i] = pr == null ? -1 : pr.optInt(i, -1);
                }
                w.days = n;
            }
            if (Double.isNaN(w.temp) && w.days == 0) {
                return null;
            }
            return w;
        } catch (Exception e) {
            return null;
        }
    }

    static String text(int c) {
        if (c == 0) {
            return "Trời quang";
        }
        if (c == 1) {
            return "Ít mây";
        }
        if (c == 2) {
            return "Mây rải rác";
        }
        if (c == 3) {
            return "Nhiều mây";
        }
        if (c == 45 || c == 48) {
            return "Sương mù";
        }
        if (c >= 51 && c <= 55) {
            return "Mưa phùn";
        }
        if (c == 56 || c == 57) {
            return "Mưa phùn lạnh";
        }
        if (c == 61) {
            return "Mưa nhỏ";
        }
        if (c == 63) {
            return "Mưa vừa";
        }
        if (c == 65) {
            return "Mưa to";
        }
        if (c == 66 || c == 67) {
            return "Mưa lạnh";
        }
        if (c >= 71 && c <= 77) {
            return "Tuyết";
        }
        if (c == 80) {
            return "Mưa rào nhẹ";
        }
        if (c == 81) {
            return "Mưa rào";
        }
        if (c == 82) {
            return "Mưa rào rất to";
        }
        if (c == 85 || c == 86) {
            return "Tuyết rơi";
        }
        if (c == 95) {
            return "Có dông";
        }
        if (c == 96 || c == 99) {
            return "Dông, mưa đá";
        }
        return "Chưa rõ";
    }
}
