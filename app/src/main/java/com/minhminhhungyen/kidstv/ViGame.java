package com.minhminhhungyen.kidstv;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Bài tiếng Việt cho bé dưới 6 tuổi (làm quen chữ cái): bé chỉ cần nhìn và chọn, không cần đọc dài.
 * Dạng bài: chữ hoa ↔ chữ thường, chữ cái đầu của từ (có hình), từ ↔ hình, con gì kêu "gâu gâu", ghép vần.
 * Hình là emoji của hệ thống; hình nào TV không vẽ được thì bị loại khỏi bộ bài (xem {@link Glyphs}).
 * Java thuần, không phụ thuộc Android.
 */
final class ViGame {
    /** Hỏi hệ thống xem phông chữ có vẽ được hình (emoji) này không. */
    interface Glyphs {
        boolean canShow(String emoji);
    }

    /** Một từ có hình. */
    static final class Item {
        final String emoji;
        final String word;
        /** true: chữ đầu là một chữ cái đơn (m, b, đ...) nên dùng được cho câu "chữ cái đầu"; false: ch, kh, nh... */
        final boolean single;

        Item(String emoji, String word, boolean single) {
            this.emoji = emoji;
            this.word = word;
            this.single = single;
        }

        String letter() {
            return word.substring(0, 1);
        }
    }

    /** Một con vật; sound rỗng nếu con vật không có tiếng kêu quen thuộc (chỉ làm đáp án nhiễu). */
    static final class Animal {
        final String emoji;
        final String name;
        final String sound;

        Animal(String emoji, String name, String sound) {
            this.emoji = emoji;
            this.name = name;
            this.sound = sound;
        }
    }

    /** 29 chữ cái tiếng Việt. */
    static final String[] ALPHABET = {
        "a", "ă", "â", "b", "c", "d", "đ", "e", "ê", "g", "h", "i", "k", "l", "m", "n", "o", "ô", "ơ",
        "p", "q", "r", "s", "t", "u", "ư", "v", "x", "y"
    };

    /** Các chữ dễ nhầm với nhau: không đặt chung một câu hỏi (a/ă/â, o/ô/ơ, d/đ, I hoa và l thường...). */
    private static final String[][] SAME = {
        {"a", "ă", "â"}, {"e", "ê"}, {"o", "ô", "ơ"}, {"u", "ư"}, {"d", "đ"}, {"i", "l"}
    };

    // Chữ đầu là một chữ cái đơn. Hình lấy từ emoji phổ biến (Unicode ≤ 11) để TV Android 12 vẽ được.
    // Hình cần dấu FE0F (đổi kiểu chữ thành kiểu hình) thì viết bằng mã Unicode cho chắc.
    static final Item[] ITEMS = {
        new Item("🎈", "bóng bay", true),
        new Item("⚽", "bóng đá", true),
        new Item("🐮", "bò", true),
        new Item("✏️", "bút chì", true),
        new Item("🎒", "ba lô", true),
        new Item("🐟", "cá", true),
        new Item("🍊", "cam", true),
        new Item("🦀", "cua", true),
        new Item("🥕", "cà rốt", true),
        new Item("🌳", "cây", true),
        new Item("🌈", "cầu vồng", true),
        new Item("🐐", "dê", true),
        new Item("🍉", "dưa hấu", true),
        new Item("🥥", "dừa", true),
        new Item("🍓", "dâu tây", true),
        new Item("💡", "đèn", true),
        new Item("⏰", "đồng hồ", true),
        new Item("🍑", "đào", true),
        new Item("🥢", "đũa", true),
        new Item("👶", "em bé", true),
        new Item("🐓", "gà trống", true),
        new Item("🐻", "gấu", true),
        new Item("🌹", "hoa hồng", true),
        new Item("🐯", "hổ", true),
        new Item("🦒", "hươu cao cổ", true),
        new Item("🎁", "hộp quà", true),
        new Item("🍬", "kẹo", true),
        new Item("🍦", "kem", true),
        new Item("✂️", "kéo", true),
        new Item("🐜", "kiến", true),
        new Item("🍃", "lá", true),
        new Item("🐷", "lợn", true),
        new Item("🍐", "lê", true),
        new Item("🔥", "lửa", true),
        new Item("🐫", "lạc đà", true),
        new Item("🐱", "mèo", true),
        new Item("🌧️", "mưa", true),
        new Item("🧢", "mũ", true),
        new Item("🌙", "mặt trăng", true),
        new Item("☀️", "mặt trời", true),
        new Item("🦑", "mực", true),
        new Item("✈️", "máy bay", true),
        new Item("🍄", "nấm", true),
        new Item("🕯️", "nến", true),
        new Item("🎀", "nơ", true),
        new Item("🐝", "ong", true),
        new Item("🚗", "ô tô", true),
        new Item("🔋", "pin", true),
        new Item("👖", "quần", true),
        new Item("🐢", "rùa", true),
        new Item("🐍", "rắn", true),
        new Item("🐉", "rồng", true),
        new Item("🥬", "rau cải", true),
        new Item("⭐", "sao", true),
        new Item("🦁", "sư tử", true),
        new Item("🥛", "sữa", true),
        new Item("📚", "sách", true),
        new Item("✋", "tay", true),
        new Item("👂", "tai", true),
        new Item("🍎", "táo", true),
        new Item("📺", "ti vi", true),
        new Item("🚀", "tên lửa", true),
        new Item("🚢", "tàu thủy", true),
        new Item("🦆", "vịt", true),
        new Item("🐘", "voi", true),
        new Item("🧳", "va li", true),
        new Item("🚲", "xe đạp", true),
        new Item("🚌", "xe buýt", true),
        new Item("🥭", "xoài", true),
        // Chữ đầu là hai chữ cái (ch, kh, nh, th...): chỉ dùng cho câu "tìm hình", không hỏi chữ cái đầu.
        new Item("🐶", "chó", false),
        new Item("🐒", "khỉ", false),
        new Item("🐰", "thỏ", false),
        new Item("🏠", "nhà", false),
        new Item("🐭", "chuột", false),
        new Item("🐴", "ngựa", false),
    };

    static final Animal[] ANIMALS = {
        new Animal("🐶", "chó", "gâu gâu"),
        new Animal("🐱", "mèo", "meo meo"),
        new Animal("🐓", "gà trống", "ò ó o o"),
        new Animal("🦆", "vịt", "cạc cạc"),
        new Animal("🐷", "lợn", "ụt ịt"),
        new Animal("🐮", "bò", "ùm bò"),
        new Animal("🐐", "dê", "be be"),
        new Animal("🐸", "ếch", "ộp ộp"),
        new Animal("🐝", "ong", "vo ve"),
        new Animal("🐴", "ngựa", "hí hí"),
    };

    /** Con vật không có tiếng kêu quen thuộc: chỉ làm đáp án nhiễu cho câu "con gì kêu...". */
    static final Animal[] SILENT = {
        new Animal("🐟", "cá", ""),
        new Animal("🐢", "rùa", ""),
        new Animal("🦀", "cua", ""),
        new Animal("🐜", "kiến", ""),
    };

    /** Phụ âm dùng cho bài ghép vần. */
    static final String[] SYL_CONS = {"b", "c", "d", "đ", "h", "k", "l", "m", "n", "r", "s", "t", "v", "x"};

    /** Vần ghép được với phụ âm, đúng chính tả: c đi với a o ô ơ; k đi với e ê i; còn lại a e ê i o ô ơ u. */
    static String[] vowelsFor(String cons) {
        if ("c".equals(cons)) {
            return new String[]{"a", "o", "ô", "ơ"};
        }
        if ("k".equals(cons)) {
            return new String[]{"e", "ê", "i"};
        }
        return new String[]{"a", "e", "ê", "i", "o", "ô", "ơ", "u"};
    }

    static boolean sameGroup(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        for (String[] g : SAME) {
            boolean ha = false;
            boolean hb = false;
            for (String s : g) {
                ha |= s.equals(a);
                hb |= s.equals(b);
            }
            if (ha && hb) {
                return true;
            }
        }
        return false;
    }

    static String upper(String letter) {
        return letter.toUpperCase(Locale.ROOT);
    }

    /** Chuỗi "viPics=đủ/tổng viAnimals=đủ/tổng" cho STATUS: cho biết TV vẽ được bao nhiêu hình. */
    static String stats(Glyphs g) {
        int items = 0;
        for (Item it : ITEMS) {
            if (g == null || g.canShow(it.emoji)) {
                items++;
            }
        }
        int animals = 0;
        for (Animal a : ANIMALS) {
            if (g == null || g.canShow(a.emoji)) {
                animals++;
            }
        }
        return "viPics=" + items + "/" + ITEMS.length + " viAnimals=" + animals + "/" + ANIMALS.length;
    }

    // ------------------------------------------------------------------------------------------

    private static final int RECENT = 8;

    private final Random rnd;
    private final List<Item> okItems = new ArrayList<>();
    private final List<Item> okSingles = new ArrayList<>();
    private final List<Animal> okSounds = new ArrayList<>();
    private final List<Animal> okSilent = new ArrayList<>();
    private final ArrayDeque<String> recent = new ArrayDeque<>();
    private int lastKind = -1;
    private int run = 0;

    ViGame(Random rnd, Glyphs glyphs) {
        this.rnd = rnd;
        for (Item it : ITEMS) {
            if (glyphs == null || glyphs.canShow(it.emoji)) {
                okItems.add(it);
                if (it.single) {
                    okSingles.add(it);
                }
            }
        }
        for (Animal a : ANIMALS) {
            if (glyphs == null || glyphs.canShow(a.emoji)) {
                okSounds.add(a);
            }
        }
        for (Animal a : SILENT) {
            if (glyphs == null || glyphs.canShow(a.emoji)) {
                okSilent.add(a);
            }
        }
    }

    /** Dạng bài nào dùng được (dạng có hình cần đủ hình mà TV vẽ được). */
    boolean available(int kind) {
        switch (kind) {
            case Quiz.LETTER_UP:
            case Quiz.LETTER_LOW:
            case Quiz.SYLLABLE:
                return true;
            case Quiz.FIRST_LETTER:
                return okSingles.size() >= 6 && distinctFirsts(okSingles) >= 4;
            case Quiz.WORD_PICTURE:
                return okItems.size() >= 8 && distinctFirsts(okItems) >= 4;
            case Quiz.ANIMAL_SOUND:
                return okSounds.size() >= 3 && okSounds.size() + okSilent.size() >= 4;
            default:
                return false;
        }
    }

    private static int distinctFirsts(List<Item> items) {
        Set<String> s = new HashSet<>();
        for (Item it : items) {
            s.add(it.letter());
        }
        return s.size();
    }

    /**
     * Câu tiếp theo. forcedKind 1..6 ép dạng bài (để thử); 0 = ngẫu nhiên theo tỉ lệ.
     * Không ra lại câu vừa làm gần đây, và không ra cùng một dạng quá 2 lần liên tiếp.
     */
    Quiz next(int forcedKind) {
        Quiz q = null;
        for (int tries = 0; tries < 40; tries++) {
            int kind = pickKind(forcedKind);
            q = make(kind);
            if (!recent.contains(q.key)) {
                break;
            }
        }
        recent.addLast(q.key);
        while (recent.size() > RECENT) {
            recent.removeFirst();
        }
        run = q.kind == lastKind ? run + 1 : 1;
        lastKind = q.kind;
        return q;
    }

    private int pickKind(int forced) {
        if (forced >= Quiz.LETTER_UP && forced <= Quiz.SYLLABLE && available(forced)) {
            return forced;
        }
        int[] w = new int[Quiz.KIND_COUNT];
        w[Quiz.LETTER_UP] = 15;
        w[Quiz.LETTER_LOW] = 15;
        w[Quiz.SYLLABLE] = 10;
        w[Quiz.FIRST_LETTER] = available(Quiz.FIRST_LETTER) ? 25 : 0;
        w[Quiz.WORD_PICTURE] = available(Quiz.WORD_PICTURE) ? 20 : 0;
        w[Quiz.ANIMAL_SOUND] = available(Quiz.ANIMAL_SOUND) ? 15 : 0;
        if (run >= 2 && lastKind > 0) {
            w[lastKind] = 0;
        }
        int total = 0;
        for (int v : w) {
            total += v;
        }
        int r = rnd.nextInt(total);
        for (int k = 1; k < w.length; k++) {
            if (r < w[k]) {
                return k;
            }
            r -= w[k];
        }
        return Quiz.LETTER_UP;
    }

    private Quiz make(int kind) {
        switch (kind) {
            case Quiz.LETTER_UP:
                return letterCase(true);
            case Quiz.LETTER_LOW:
                return letterCase(false);
            case Quiz.FIRST_LETTER:
                return firstLetter();
            case Quiz.WORD_PICTURE:
                return wordPicture();
            case Quiz.ANIMAL_SOUND:
                return animalSound();
            default:
                return syllable();
        }
    }

    // ---- Chữ hoa ↔ chữ thường ----------------------------------------------------------------------

    private Quiz letterCase(boolean showUpper) {
        String letter = ALPHABET[rnd.nextInt(ALPHABET.length)];
        List<String> opts = letterOptions(letter);
        int idx = opts.indexOf(letter);
        String[] o = new String[4];
        for (int i = 0; i < 4; i++) {
            o[i] = showUpper ? opts.get(i) : upper(opts.get(i));
        }
        if (showUpper) {
            return new Quiz(Quiz.LETTER_UP, upper(letter), 84f, "Chữ thường của chữ này là chữ nào?", o, false, 46f,
                    idx, "Chữ thường của " + upper(letter) + " là " + letter + ".");
        }
        return new Quiz(Quiz.LETTER_LOW, letter, 84f, "Chữ hoa của chữ này là chữ nào?", o, false, 46f,
                idx, "Chữ hoa của " + letter + " là " + upper(letter) + ".");
    }

    /** Chữ đúng + 3 chữ khác nhóm với nó, xếp ngẫu nhiên (chữ thường). */
    private List<String> letterOptions(String answer) {
        List<String> pool = new ArrayList<>();
        for (String l : ALPHABET) {
            if (!sameGroup(l, answer)) {
                pool.add(l);
            }
        }
        Collections.shuffle(pool, rnd);
        List<String> out = new ArrayList<>();
        out.add(answer);
        out.addAll(pool.subList(0, 3));
        Collections.shuffle(out, rnd);
        return out;
    }

    // ---- Chữ cái đầu của từ --------------------------------------------------------------------------

    private Quiz firstLetter() {
        Item it = okSingles.get(rnd.nextInt(okSingles.size()));
        String letter = it.letter();
        List<String> opts = letterOptions(letter);
        return new Quiz(Quiz.FIRST_LETTER, it.emoji, 64f, "“" + it.word + "” bắt đầu bằng chữ gì?",
                opts.toArray(new String[0]), false, 46f, opts.indexOf(letter),
                "“" + it.word + "” bắt đầu bằng chữ " + letter + ".");
    }

    // ---- Chữ ↔ hình ----------------------------------------------------------------------------------

    private Quiz wordPicture() {
        Item it = okItems.get(rnd.nextInt(okItems.size()));
        List<Item> pool = new ArrayList<>(okItems);
        Collections.shuffle(pool, rnd);
        List<Item> chosen = new ArrayList<>();
        chosen.add(it);
        Set<String> firsts = new HashSet<>();
        firsts.add(it.letter());
        // Đáp án nhiễu có chữ đầu khác nhau: bé biết chữ đầu là loại được rồi
        for (Item x : pool) {
            if (chosen.size() == 4) {
                break;
            }
            if (x != it && firsts.add(x.letter())) {
                chosen.add(x);
            }
        }
        Collections.shuffle(chosen, rnd);
        String[] o = new String[4];
        for (int i = 0; i < 4; i++) {
            o[i] = chosen.get(i).emoji;
        }
        return new Quiz(Quiz.WORD_PICTURE, it.word, 58f, "Tìm hình đúng với chữ này", o, true, 46f,
                chosen.indexOf(it), "“" + it.word + "” là hình " + it.emoji + ".");
    }

    // ---- Tiếng kêu con vật ---------------------------------------------------------------------------

    private Quiz animalSound() {
        Animal a = okSounds.get(rnd.nextInt(okSounds.size()));
        List<Animal> pool = new ArrayList<>(okSounds);
        pool.addAll(okSilent);
        pool.remove(a);
        Collections.shuffle(pool, rnd);
        List<Animal> chosen = new ArrayList<>();
        chosen.add(a);
        chosen.addAll(pool.subList(0, 3));
        Collections.shuffle(chosen, rnd);
        String[] o = new String[4];
        for (int i = 0; i < 4; i++) {
            o[i] = chosen.get(i).emoji;
        }
        return new Quiz(Quiz.ANIMAL_SOUND, "“" + a.sound + "”", 54f, "Con gì kêu như vậy?", o, true, 46f,
                chosen.indexOf(a), "“" + a.sound + "” là tiếng kêu của " + a.name + " " + a.emoji + ".");
    }

    // ---- Ghép vần ------------------------------------------------------------------------------------

    private Quiz syllable() {
        String c = SYL_CONS[rnd.nextInt(SYL_CONS.length)];
        String[] vs = vowelsFor(c);
        String v = vs[rnd.nextInt(vs.length)];
        String ans = c + v;

        List<String> sameCons = new ArrayList<>();
        for (String x : vs) {
            if (!x.equals(v)) {
                sameCons.add(c + x);
            }
        }
        List<String> sameVowel = new ArrayList<>();
        List<String> all = new ArrayList<>();
        for (String cc : SYL_CONS) {
            for (String x : vowelsFor(cc)) {
                String s = cc + x;
                if (s.equals(ans)) {
                    continue;
                }
                all.add(s);
                if (x.equals(v)) {
                    sameVowel.add(s);
                }
            }
        }
        Collections.shuffle(sameCons, rnd);
        Collections.shuffle(sameVowel, rnd);
        Collections.shuffle(all, rnd);
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(ans);
        if (!sameCons.isEmpty()) {
            set.add(sameCons.get(0));
        }
        if (!sameVowel.isEmpty()) {
            set.add(sameVowel.get(0));
        }
        for (String s : all) {
            if (set.size() >= 4) {
                break;
            }
            set.add(s);
        }
        List<String> opts = new ArrayList<>(set);
        Collections.shuffle(opts, rnd);
        return new Quiz(Quiz.SYLLABLE, c + " + " + v + " = ?", 64f, "Ghép hai chữ thành tiếng nào?",
                opts.toArray(new String[0]), false, 42f, opts.indexOf(ans), c + " + " + v + " = " + ans + ".");
    }
}
