package com.minhminhhungyen.kidstv;

/**
 * Một câu hỏi trên màn hình khóa: dòng lớn (phép tính, chữ cái, hình, chữ hoặc tiếng kêu), dòng nhỏ dẫn câu hỏi
 * và 4 đáp án (số, chữ hoặc hình) để bé chọn bằng remote. Java thuần để thử được ngay trên máy tính.
 */
final class Quiz {
    static final int MATH = 0;
    /** Hiện chữ HOA, bé chọn chữ thường. */
    static final int LETTER_UP = 1;
    /** Hiện chữ thường, bé chọn chữ HOA. */
    static final int LETTER_LOW = 2;
    /** Hiện hình + từ, bé chọn chữ cái đầu của từ. */
    static final int FIRST_LETTER = 3;
    /** Hiện một từ, bé chọn hình đúng. */
    static final int WORD_PICTURE = 4;
    /** Hiện tiếng kêu, bé chọn con vật. */
    static final int ANIMAL_SOUND = 5;
    /** Hiện "b + a = ?", bé chọn tiếng ghép đúng. */
    static final int SYLLABLE = 6;

    static final int KIND_COUNT = 7;
    static final String[] NAMES = {
        "math", "letter-up", "letter-low", "first-letter", "word-picture", "animal-sound", "syllable"
    };

    final int kind;
    /** Dòng lớn nhất. */
    final String big;
    /** Cỡ chữ dòng lớn (đơn vị thiết kế của Ui). */
    final float bigSize;
    /** Dòng nhỏ dẫn câu hỏi, rỗng nếu không có. */
    final String hint;
    final String[] options;
    /** Đáp án là hình (emoji). */
    final boolean pictures;
    final float optSize;
    final int correctIndex;
    /** Câu nói đáp án đúng, dùng khi bé trả lời sai ("Đáp án là 37."). */
    final String answerText;
    /** Dùng để không ra lại câu vừa làm. */
    final String key;

    Quiz(int kind, String big, float bigSize, String hint, String[] options, boolean pictures, float optSize,
         int correctIndex, String answerText) {
        if (options.length != 4 || correctIndex < 0 || correctIndex > 3) {
            throw new IllegalArgumentException("câu hỏi phải có 4 đáp án và 1 đáp án đúng");
        }
        this.kind = kind;
        this.big = big;
        this.bigSize = bigSize;
        this.hint = hint;
        this.options = options;
        this.pictures = pictures;
        this.optSize = optSize;
        this.correctIndex = correctIndex;
        this.answerText = answerText;
        this.key = kind + ":" + big + ":" + options[correctIndex];
    }

    String answer() {
        return options[correctIndex];
    }

    String kindName() {
        return NAMES[kind];
    }

    static Quiz fromMath(MathGame.Problem p) {
        String[] o = new String[4];
        for (int i = 0; i < 4; i++) {
            o[i] = String.valueOf(p.options[i]);
        }
        return new Quiz(MATH, p.text, 68f, "", o, false, 36f, p.correctIndex, "Đáp án là " + p.answer + ".");
    }
}
