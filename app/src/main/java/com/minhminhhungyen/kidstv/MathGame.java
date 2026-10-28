package com.minhminhhungyen.kidstv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

/** Sinh bài toán cộng/trừ trong phạm vi 100, kèm 4 đáp án để bé chọn bằng remote. */
final class MathGame {
    static final class Problem {
        final int a;
        final int b;
        final boolean plus;
        final int answer;
        final String text;
        final int[] options;
        final int correctIndex;

        Problem(int a, int b, boolean plus, int answer, int[] options) {
            this.a = a;
            this.b = b;
            this.plus = plus;
            this.answer = answer;
            this.text = a + (plus ? " + " : " − ") + b + " = ?";
            this.options = options;
            int idx = -1;
            for (int i = 0; i < options.length; i++) {
                if (options[i] == answer) {
                    idx = i;
                }
            }
            this.correctIndex = idx;
        }
    }

    private final Random rnd;
    private String lastText = "";

    MathGame(Random rnd) {
        this.rnd = rnd;
    }

    Problem next() {
        Problem p = make();
        for (int guard = 0; guard < 8 && p.text.equals(lastText); guard++) {
            p = make();
        }
        lastText = p.text;
        return p;
    }

    private Problem make() {
        boolean plus = rnd.nextBoolean();
        int a;
        int b;
        int ans;
        if (plus) {
            a = 1 + rnd.nextInt(99);          // 1..99
            b = 1 + rnd.nextInt(100 - a);     // 1..(100-a): tổng tối đa 100
            ans = a + b;
        } else {
            a = 2 + rnd.nextInt(99);          // 2..100
            b = 1 + rnd.nextInt(a - 1);       // 1..(a-1): hiệu không âm
            ans = a - b;
        }
        return new Problem(a, b, plus, ans, options(ans));
    }

    /** 4 đáp án khác nhau, nằm trong 0..100, có đáp án đúng, xếp tăng dần. */
    private int[] options(int ans) {
        int[] deltas = {1, -1, 2, -2, 10, -10, 9, -9, 11, -11, 5, -5, 3, -3};
        List<Integer> cand = new ArrayList<>();
        for (int d : deltas) {
            int v = ans + d;
            if (v >= 0 && v <= 100) {
                cand.add(v);
            }
        }
        if (ans >= 10 && ans <= 99) {
            int swapped = (ans % 10) * 10 + ans / 10;
            if (swapped != ans) {
                cand.add(swapped);
            }
        }
        Collections.shuffle(cand, rnd);
        LinkedHashSet<Integer> set = new LinkedHashSet<>();
        set.add(ans);
        for (int v : cand) {
            if (set.size() >= 4) {
                break;
            }
            set.add(v);
        }
        while (set.size() < 4) {
            set.add(rnd.nextInt(101));
        }
        List<Integer> list = new ArrayList<>(set);
        Collections.sort(list);
        int[] out = new int[4];
        for (int i = 0; i < 4; i++) {
            out[i] = list.get(i);
        }
        return out;
    }
}
