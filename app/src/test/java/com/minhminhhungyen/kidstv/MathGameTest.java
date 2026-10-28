package com.minhminhhungyen.kidstv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

public class MathGameTest {

    @Test
    public void problemsStayWithin100() {
        MathGame g = new MathGame(new Random(42));
        int plus = 0;
        int minus = 0;
        String last = "";
        for (int i = 0; i < 20000; i++) {
            MathGame.Problem p = g.next();
            assertTrue("không lặp liên tiếp: " + p.text, !p.text.equals(last));
            last = p.text;
            assertTrue(p.text, p.text.matches("\\d+ [+\u2212] \\d+ = \\?"));
            if (p.plus) {
                plus++;
                assertTrue(p.text, p.a >= 1 && p.b >= 1 && p.a + p.b <= 100);
                assertEquals(p.text, p.a + p.b, p.answer);
            } else {
                minus++;
                assertTrue(p.text, p.b >= 1 && p.a <= 100 && p.b < p.a);
                assertEquals(p.text, p.a - p.b, p.answer);
            }
            assertTrue(p.text, p.answer >= 0 && p.answer <= 100);
            assertEquals(4, p.options.length);
            Set<Integer> seen = new HashSet<>();
            for (int k = 0; k < 4; k++) {
                int o = p.options[k];
                assertTrue(p.text + " đáp án " + o, o >= 0 && o <= 100);
                seen.add(o);
                if (k > 0) {
                    assertTrue(p.text + " xếp tăng dần", p.options[k - 1] < o);
                }
            }
            assertEquals(p.text + " 4 đáp án khác nhau", 4, seen.size());
            assertTrue(p.text, p.correctIndex >= 0 && p.options[p.correctIndex] == p.answer);
        }
        assertTrue("có cả phép cộng", plus > 5000);
        assertTrue("có cả phép trừ", minus > 5000);
    }
}
