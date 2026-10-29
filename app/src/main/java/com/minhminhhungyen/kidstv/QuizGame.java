package com.minhminhhungyen.kidstv;

import java.util.Random;

/**
 * Chọn câu hỏi cho màn hình khóa theo "dạng bài cho bé" mà phụ huynh đặt:
 * Toán (cộng trừ trong 100), Tiếng Việt (bé dưới 6 tuổi) hoặc cả hai xen kẽ.
 */
final class QuizGame {
    /** Giá trị lưu trong Prefs.subject. */
    static final int SUBJECT_MATH = 0;
    static final int SUBJECT_VI = 1;
    static final int SUBJECT_MIX = 2;

    private final Random rnd;
    private final MathGame math;
    private final ViGame vi;
    private int lastSubject = -1;
    private int run = 0;

    QuizGame(Random rnd, ViGame.Glyphs glyphs) {
        this.rnd = rnd;
        this.math = new MathGame(rnd);
        this.vi = new ViGame(rnd, glyphs);
    }

    /**
     * Câu tiếp theo. viKind 1..6 ép dạng bài tiếng Việt (chỉ để thử bằng adb), 0 = ngẫu nhiên.
     * Chế độ xen kẽ: ngẫu nhiên 50/50 nhưng không quá 2 câu liền cùng một môn.
     */
    Quiz next(int subject, int viKind) {
        int s = subject;
        if (s != SUBJECT_MATH && s != SUBJECT_VI) {
            s = rnd.nextBoolean() ? SUBJECT_MATH : SUBJECT_VI;
            if (s == lastSubject && run >= 2) {
                s = s == SUBJECT_MATH ? SUBJECT_VI : SUBJECT_MATH;
            }
        }
        run = s == lastSubject ? run + 1 : 1;
        lastSubject = s;
        return s == SUBJECT_MATH ? Quiz.fromMath(math.next()) : vi.next(viKind);
    }
}
