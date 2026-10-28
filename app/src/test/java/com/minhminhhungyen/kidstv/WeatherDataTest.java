package com.minhminhhungyen.kidstv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WeatherDataTest {
    /** Dạng trả về của Open-Meteo (/v1/forecast với current + daily). */
    private static final String SAMPLE = "{\"latitude\":20.875,\"longitude\":106.0,\"generationtime_ms\":0.08,"
            + "\"utc_offset_seconds\":25200,\"timezone\":\"Asia/Bangkok\",\"timezone_abbreviation\":\"+07\","
            + "\"elevation\":3.0,"
            + "\"current_units\":{\"time\":\"iso8601\",\"interval\":\"seconds\",\"temperature_2m\":\"\\u00b0C\","
            + "\"relative_humidity_2m\":\"%\",\"apparent_temperature\":\"\\u00b0C\",\"weather_code\":\"wmo code\",\"is_day\":\"\"},"
            + "\"current\":{\"time\":\"2026-10-28T16:45\",\"interval\":900,\"temperature_2m\":27.4,"
            + "\"relative_humidity_2m\":78,\"apparent_temperature\":31.2,\"weather_code\":3,\"is_day\":1},"
            + "\"daily_units\":{\"time\":\"iso8601\",\"weather_code\":\"wmo code\",\"temperature_2m_max\":\"\\u00b0C\","
            + "\"temperature_2m_min\":\"\\u00b0C\",\"precipitation_probability_max\":\"%\"},"
            + "\"daily\":{\"time\":[\"2026-10-28\",\"2026-10-29\",\"2026-10-30\"],\"weather_code\":[3,61,80],"
            + "\"temperature_2m_max\":[29.1,28.3,27.9],\"temperature_2m_min\":[23.4,22.8,22.1],"
            + "\"precipitation_probability_max\":[20,65,80]}}";

    @Test
    public void parsesCurrentAndDaily() {
        WeatherData w = WeatherData.parse(SAMPLE);
        assertNotNull(w);
        assertEquals(27.4, w.temp, 0.001);
        assertEquals(31.2, w.feels, 0.001);
        assertEquals(78, w.humidity);
        assertEquals(3, w.code);
        assertTrue(w.isDay);
        assertEquals(3, w.days);
        assertEquals(29.1, w.max[0], 0.001);
        assertEquals(22.8, w.min[1], 0.001);
        assertEquals(61, w.dcode[1]);
        assertEquals(80, w.dcode[2]);
        assertEquals(65, w.rain[1]);
    }

    @Test
    public void nightFlag() {
        WeatherData w = WeatherData.parse(SAMPLE.replace("\"is_day\":1", "\"is_day\":0"));
        assertNotNull(w);
        assertFalse(w.isDay);
    }

    @Test
    public void readsOldCurrentWeatherShape() {
        String old = "{\"current_weather\":{\"temperature\":25.5,\"weathercode\":61,\"is_day\":1}}";
        WeatherData w = WeatherData.parse(old);
        assertNotNull(w);
        assertEquals(25.5, w.temp, 0.001);
        assertEquals(61, w.code);
        assertEquals(0, w.days);
    }

    @Test
    public void rejectsGarbage() {
        assertNull(WeatherData.parse(null));
        assertNull(WeatherData.parse(""));
        assertNull(WeatherData.parse("<html>403</html>"));
        assertNull(WeatherData.parse("{\"reason\":\"rate limited\",\"error\":true}"));
    }

    @Test
    public void vietnameseTexts() {
        assertEquals("Trời quang", WeatherData.text(0));
        assertEquals("Nhiều mây", WeatherData.text(3));
        assertEquals("Mưa nhỏ", WeatherData.text(61));
        assertEquals("Mưa rào", WeatherData.text(81));
        assertEquals("Có dông", WeatherData.text(95));
        assertEquals("Chưa rõ", WeatherData.text(-1));
    }
}
