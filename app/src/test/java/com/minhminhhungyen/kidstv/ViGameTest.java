package com.minhminhhungyen.kidstv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

/** Bộ bài tiếng Việt cho bé dưới 6 tuổi: dữ liệu sạch, mỗi câu có đúng 1 đáp án đúng, không lộ đáp án, không lặp. */
public class ViGameTest {
    private static final ViGame.Glyphs ALL = e -> true;
    private static final String[] DIGRAPHS = {"ch", "kh", "nh", "th", "ng", "tr", "ph", "gi", "gh"};

    private static boolean isNfc(String s) {
        return Normalizer.isNormalized(s, Normalizer.Form.NFC);
    }

    private static boolean inAlphabet(String s) {
        return Arrays.asList(ViGame.ALPHABET).contains(s);
    }

    private static ViGame.Item itemByEmoji(String emoji) {
        for (ViGame.Item it : ViGame.ITEMS) {
            if (it.emoji.equals(emoji)) {
                return it;
            }
        }
        return null;
    }

    private static ViGame.Item itemByWord(String word) {
        for (ViGame.Item it : ViGame.ITEMS) {
            if (it.word.equals(word)) {
                return it;
            }
        }
        return null;
    }

    private static ViGame.Animal animalBySound(String sound) {
        for (ViGame.Animal a : ViGame.ANIMALS) {
            if (a.sound.equals(sound)) {
                return a;
            }
        }
        return null;
    }

    private static Set<String> allAnimalEmoji() {
        Set<String> s = new HashSet<>();
        for (ViGame.Animal a : ViGame.ANIMALS) {
            s.add(a.emoji);
        }
        for (ViGame.Animal a : ViGame.SILENT) {
            s.add(a.emoji);
        }
        return s;
    }

    // ---- Dữ liệu ----------------------------------------------------------------------------------

    @Test
    public void alphabetHas29DistinctLetters() {
        assertEquals(29, ViGame.ALPHABET.length);
        Set<String> seen = new HashSet<>();
        for (String l : ViGame.ALPHABET) {
            assertTrue(l, isNfc(l));
            assertEquals(l, 1, l.length());
            assertTrue(l, seen.add(l));
            String up = ViGame.upper(l);
            assertEquals(l, 1, up.length());
            assertFalse(l + " chữ hoa khác chữ thường", up.equals(l));
            assertEquals(l, l, up.toLowerCase(Locale.ROOT));
        }
        assertEquals("Ă", ViGame.upper("ă"));
        assertEquals("Đ", ViGame.upper("đ"));
        assertEquals("Ơ", ViGame.upper("ơ"));
    }

    @Test
    public void wordBankIsClean() {
        Set<String> emojis = new HashSet<>();
        Set<String> words = new HashSet<>();
        for (ViGame.Item it : ViGame.ITEMS) {
            assertTrue(it.word, isNfc(it.word));
            assertEquals("chữ thường, không dấu cách thừa: " + it.word, it.word, it.word.trim().toLowerCase(Locale.ROOT));
            assertTrue("hình không trùng: " + it.word, emojis.add(it.emoji));
            assertTrue("từ không trùng: " + it.word, words.add(it.word));
            assertTrue("chữ đầu của '" + it.word + "' phải là chữ cái trơn (không có dấu thanh)", inAlphabet(it.letter()));
            boolean digraph = false;
            for (String d : DIGRAPHS) {
                digraph |= it.word.startsWith(d);
            }
            assertEquals("'" + it.word + "': đánh dấu single phải khớp với việc có bắt đầu bằng ch/kh/nh/th/ng/tr/ph/gi/gh",
                    !digraph, it.single);
            int[] cps = it.emoji.codePoints().toArray();
            assertTrue(it.word + " hình lạ", cps.length >= 1 && cps.length <= 2);
            for (int cp : cps) {
                assertTrue(it.word + " không dùng emoji mới hơn Unicode 11 (TV Android 12 có thể không vẽ được)", cp < 0x1FA70);
                assertTrue(it.word + " hình không được là chữ/số thường", cp > 0xFF);
            }
            if (cps.length == 2) {
                assertEquals(it.word + ": ký tự thứ hai chỉ có thể là FE0F", 0xFE0F, cps[1]);
            }
        }
        assertTrue("đủ từ để ra bài", ViGame.ITEMS.length >= 60);
    }

    @Test
    public void animalBankIsClean() {
        Set<String> emojis = new HashSet<>();
        Set<String> sounds = new HashSet<>();
        for (ViGame.Animal a : ViGame.ANIMALS) {
            assertTrue(a.name, isNfc(a.name) && isNfc(a.sound));
            assertFalse(a.name, a.sound.isEmpty());
            assertTrue(a.name, emojis.add(a.emoji));
            assertTrue("tiếng kêu không trùng: " + a.sound, sounds.add(a.sound));
        }
        for (ViGame.Animal a : ViGame.SILENT) {
            assertTrue(a.name, a.sound.isEmpty());
            assertTrue("hình không trùng con vật có tiếng kêu: " + a.name, emojis.add(a.emoji));
        }
    }

    @Test
    public void syllableRulesFollowSpelling() {
        for (String c : ViGame.SYL_CONS) {
            assertTrue(c, isNfc(c));
            for (String v : ViGame.vowelsFor(c)) {
                assertTrue(v, isNfc(v));
                if (c.equals("c")) {
                    assertFalse("c không đi với e ê i: " + c + v, "eêi".contains(v));
                }
                if (c.equals("k")) {
                    assertTrue("k chỉ đi với e ê i: " + c + v, "eêi".contains(v));
                }
            }
        }
        assertFalse("không ghép vần 'cu' (từ trẻ con hay nói bậy)", Arrays.asList(ViGame.vowelsFor("c")).contains("u"));
    }

    // ---- Câu hỏi ------------------------------------------------------------------------------------

    private static void checkCommon(Quiz q, String what) {
        assertEquals(what, 4, q.options.length);
        Set<String> seen = new HashSet<>();
        for (String o : q.options) {
            assertNotNull(what, o);
            assertFalse(what + " đáp án rỗng", o.isEmpty());
            assertTrue(what + " đáp án trùng: " + o, seen.add(o));
            assertTrue(what, isNfc(o));
        }
        assertTrue(what, q.correctIndex >= 0 && q.correctIndex < 4);
        assertEquals(what, q.options[q.correctIndex], q.answer());
        assertFalse(what, q.big.isEmpty());
        assertFalse(what, q.answerText.isEmpty());
        assertTrue(what, isNfc(q.big) && isNfc(q.hint) && isNfc(q.answerText));
        assertEquals(what, q.kind + ":" + q.big + ":" + q.answer(), q.key);
    }

    private static void checkSemantics(Quiz q) {
        String what = q.kindName() + " " + q.big + " → " + q.answer() + " " + Arrays.toString(q.options);
        checkCommon(q, what);
        switch (q.kind) {
            case Quiz.LETTER_UP: {
                String lower = q.big.toLowerCase(Locale.ROOT);
                assertTrue(what, inAlphabet(lower));
                assertEquals(what, ViGame.upper(lower), q.big);
                assertEquals(what, lower, q.answer());
                for (String o : q.options) {
                    assertTrue(what, inAlphabet(o));
                    assertTrue(what, o.equals(lower) || !ViGame.sameGroup(o, lower));
                }
                assertFalse(what, q.pictures);
                break;
            }
            case Quiz.LETTER_LOW: {
                assertTrue(what, inAlphabet(q.big));
                assertEquals(what, ViGame.upper(q.big), q.answer());
                for (String o : q.options) {
                    String low = o.toLowerCase(Locale.ROOT);
                    assertTrue(what, inAlphabet(low));
                    assertEquals(what + " đáp án phải là chữ hoa", ViGame.upper(low), o);
                    assertTrue(what, low.equals(q.big) || !ViGame.sameGroup(low, q.big));
                }
                assertFalse(what, q.pictures);
                break;
            }
            case Quiz.FIRST_LETTER: {
                ViGame.Item it = itemByEmoji(q.big);
                assertNotNull(what, it);
                assertTrue(what + " chỉ hỏi chữ cái đầu với từ bắt đầu bằng một chữ cái", it.single);
                assertEquals(what, it.letter(), q.answer());
                assertTrue(what, q.hint.contains(it.word));
                for (String o : q.options) {
                    assertTrue(what, inAlphabet(o));
                    assertTrue(what, o.equals(it.letter()) || !ViGame.sameGroup(o, it.letter()));
                }
                assertFalse(what, q.pictures);
                break;
            }
            case Quiz.WORD_PICTURE: {
                ViGame.Item it = itemByWord(q.big);
                assertNotNull(what, it);
                assertEquals(what, it.emoji, q.answer());
                Set<String> firsts = new HashSet<>();
                for (String o : q.options) {
                    ViGame.Item x = itemByEmoji(o);
                    assertNotNull(what, x);
                    assertTrue(what + " các hình đáp án có chữ đầu khác nhau", firsts.add(x.letter()));
                }
                assertTrue(what, q.pictures);
                break;
            }
            case Quiz.ANIMAL_SOUND: {
                String sound = q.big.substring(1, q.big.length() - 1);
                ViGame.Animal a = animalBySound(sound);
                assertNotNull(what, a);
                assertEquals(what, a.emoji, q.answer());
                Set<String> known = allAnimalEmoji();
                for (String o : q.options) {
                    assertTrue(what, known.contains(o));
                }
                assertTrue(what, q.pictures);
                break;
            }
            case Quiz.SYLLABLE: {
                String[] parts = q.big.split(" ");
                assertEquals(what, 5, parts.length);
                String c = parts[0];
                String v = parts[2];
                assertEquals(what, "+", parts[1]);
                assertEquals(what, "=", parts[3]);
                assertEquals(what, "?", parts[4]);
                assertTrue(what, Arrays.asList(ViGame.vowelsFor(c)).contains(v));
                assertEquals(what, c + v, q.answer());
                Set<String> valid = new HashSet<>();
                for (String cc : ViGame.SYL_CONS) {
                    for (String vv : ViGame.vowelsFor(cc)) {
                        valid.add(cc + vv);
                    }
                }
                for (String o : q.options) {
                    assertTrue(what + " tiếng không hợp lệ: " + o, valid.contains(o));
                }
                assertFalse(what, q.pictures);
                break;
            }
            default:
                throw new AssertionError("dạng lạ: " + q.kind);
        }
    }

    @Test
    public void everyKindGivesOneRightAnswerAndNoGiveaway() {
        ViGame g = new ViGame(new Random(11), ALL);
        for (int kind = Quiz.LETTER_UP; kind <= Quiz.SYLLABLE; kind++) {
            assertTrue("dạng " + kind + " dùng được", g.available(kind));
            for (int i = 0; i < 3000; i++) {
                Quiz q = g.next(kind);
                assertEquals("ép dạng " + kind, kind, q.kind);
                checkSemantics(q);
            }
        }
    }

    @Test
    public void answerPositionIsSpreadOut() {
        ViGame g = new ViGame(new Random(5), ALL);
        for (int kind = Quiz.LETTER_UP; kind <= Quiz.SYLLABLE; kind++) {
            int[] pos = new int[4];
            for (int i = 0; i < 2000; i++) {
                pos[g.next(kind).correctIndex]++;
            }
            for (int p = 0; p < 4; p++) {
                assertTrue("dạng " + kind + " đáp án đúng ở ô " + p + " có " + pos[p] + "/2000", pos[p] > 300);
            }
        }
    }

    @Test
    public void randomMixCoversEveryKindWithoutLongRuns() {
        ViGame g = new ViGame(new Random(3), ALL);
        int[] count = new int[Quiz.KIND_COUNT];
        int run = 0;
        int last = -1;
        for (int i = 0; i < 6000; i++) {
            Quiz q = g.next(0);
            checkSemantics(q);
            count[q.kind]++;
            run = q.kind == last ? run + 1 : 1;
            last = q.kind;
            assertTrue("không ra 3 câu liền cùng dạng", run <= 2);
        }
        assertEquals("tiếng Việt không có câu toán", 0, count[Quiz.MATH]);
        for (int k = Quiz.LETTER_UP; k <= Quiz.SYLLABLE; k++) {
            assertTrue("dạng " + k + " ra " + count[k] + " lần", count[k] > 300);
        }
    }

    @Test
    public void noRecentRepeats() {
        ViGame g = new ViGame(new Random(7), ALL);
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            String k = g.next(0).key;
            for (int j = Math.max(0, keys.size() - 5); j < keys.size(); j++) {
                assertFalse("lặp câu trong 5 câu gần nhất: " + k, keys.get(j).equals(k));
            }
            keys.add(k);
        }
        // Dạng chỉ có 10 con vật vẫn không lặp trong 8 câu liền
        ViGame a = new ViGame(new Random(9), ALL);
        List<String> ak = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            String k = a.next(Quiz.ANIMAL_SOUND).key;
            for (int j = Math.max(0, ak.size() - 8); j < ak.size(); j++) {
                assertFalse("lặp con vật trong 8 câu gần nhất: " + k, ak.get(j).equals(k));
            }
            ak.add(k);
        }
    }

    @Test
    public void everyBankEntryGetsAsked() {
        ViGame g = new ViGame(new Random(21), ALL);
        Set<String> letters = new HashSet<>();
        Set<String> firstWords = new HashSet<>();
        Set<String> pictureWords = new HashSet<>();
        Set<String> sounds = new HashSet<>();
        Set<String> syllables = new HashSet<>();
        for (int i = 0; i < 6000; i++) {
            letters.add(g.next(Quiz.LETTER_UP).answer());
            firstWords.add(itemByEmoji(g.next(Quiz.FIRST_LETTER).big).word);
            pictureWords.add(g.next(Quiz.WORD_PICTURE).big);
            sounds.add(g.next(Quiz.ANIMAL_SOUND).big);
            syllables.add(g.next(Quiz.SYLLABLE).answer());
        }
        assertEquals("đủ 29 chữ cái", 29, letters.size());
        int singles = 0;
        for (ViGame.Item it : ViGame.ITEMS) {
            if (it.single) {
                singles++;
            }
        }
        assertEquals("mọi từ chữ-đầu-đơn đều được hỏi chữ cái đầu", singles, firstWords.size());
        assertEquals("mọi từ đều được hỏi tìm hình", ViGame.ITEMS.length, pictureWords.size());
        assertEquals("mọi con vật đều được hỏi tiếng kêu", ViGame.ANIMALS.length, sounds.size());
        int valid = 0;
        for (String c : ViGame.SYL_CONS) {
            valid += ViGame.vowelsFor(c).length;
        }
        assertEquals("mọi tiếng ghép vần đều được hỏi", valid, syllables.size());
    }

    // ---- TV không vẽ được hình ----------------------------------------------------------------------

    @Test
    public void withoutAnyPictureOnlyLetterAndSyllableQuestions() {
        ViGame g = new ViGame(new Random(2), e -> false);
        assertFalse(g.available(Quiz.FIRST_LETTER));
        assertFalse(g.available(Quiz.WORD_PICTURE));
        assertFalse(g.available(Quiz.ANIMAL_SOUND));
        assertTrue(g.available(Quiz.LETTER_UP) && g.available(Quiz.LETTER_LOW) && g.available(Quiz.SYLLABLE));
        Set<Integer> kinds = new HashSet<>();
        for (int i = 0; i < 3000; i++) {
            Quiz q = g.next(i % 3 == 0 ? Quiz.WORD_PICTURE : 0); // ép dạng có hình thì bị bỏ qua
            checkSemantics(q);
            kinds.add(q.kind);
            assertFalse("không ra câu có hình", q.pictures);
        }
        assertEquals(new HashSet<>(Arrays.asList(Quiz.LETTER_UP, Quiz.LETTER_LOW, Quiz.SYLLABLE)), kinds);
    }

    @Test
    public void someMissingPicturesAreLeftOut() {
        // Thiếu khoảng 60% hình (chọn cố định theo hình): câu hỏi vẫn đúng và không bao giờ dùng hình đã bị loại
        ViGame.Glyphs partial = e -> (e.hashCode() & 0xFF) % 5 < 2;
        ViGame g = new ViGame(new Random(4), partial);
        for (int i = 0; i < 4000; i++) {
            Quiz q = g.next(0);
            checkSemantics(q);
            if (q.kind == Quiz.FIRST_LETTER) {
                assertTrue(q.big, partial.canShow(q.big));
            }
            if (q.pictures) {
                for (String o : q.options) {
                    assertTrue(q.kindName() + " dùng hình bị loại: " + o, partial.canShow(o));
                }
            }
        }
    }

    @Test
    public void statsReportsWhatTheTvCanDraw() {
        assertEquals("viPics=" + ViGame.ITEMS.length + "/" + ViGame.ITEMS.length + " viAnimals=" + ViGame.ANIMALS.length
                + "/" + ViGame.ANIMALS.length, ViGame.stats(null));
        assertEquals("viPics=0/" + ViGame.ITEMS.length + " viAnimals=0/" + ViGame.ANIMALS.length, ViGame.stats(e -> false));
    }

    // ---- Chọn môn ------------------------------------------------------------------------------------

    @Test
    public void subjectDecidesWhichQuestions() {
        QuizGame onlyMath = new QuizGame(new Random(1), ALL);
        for (int i = 0; i < 500; i++) {
            Quiz q = onlyMath.next(QuizGame.SUBJECT_MATH, 0);
            assertEquals(Quiz.MATH, q.kind);
            assertTrue(q.big, q.big.matches("\\d+ [+−] \\d+ = \\?"));
            checkCommon(q, q.big);
            assertTrue(q.answerText, q.answerText.startsWith("Đáp án là "));
        }
        QuizGame onlyVi = new QuizGame(new Random(1), ALL);
        for (int i = 0; i < 500; i++) {
            assertTrue(onlyVi.next(QuizGame.SUBJECT_VI, 0).kind != Quiz.MATH);
        }
        QuizGame mix = new QuizGame(new Random(1), ALL);
        int math = 0;
        int vi = 0;
        int run = 0;
        boolean lastMath = false;
        for (int i = 0; i < 4000; i++) {
            Quiz q = mix.next(QuizGame.SUBJECT_MIX, 0);
            boolean m = q.kind == Quiz.MATH;
            if (m) {
                math++;
            } else {
                vi++;
            }
            run = i > 0 && m == lastMath ? run + 1 : 1;
            lastMath = m;
            assertTrue("không quá 2 câu liền cùng một môn", run <= 2);
        }
        assertTrue("xen kẽ có cả toán (" + math + ") và tiếng Việt (" + vi + ")", math > 1200 && vi > 1200);
    }

    @Test
    public void forcedKindOnlyAffectsVietnamese() {
        QuizGame g = new QuizGame(new Random(8), ALL);
        for (int i = 0; i < 200; i++) {
            assertEquals(Quiz.SYLLABLE, g.next(QuizGame.SUBJECT_VI, Quiz.SYLLABLE).kind);
            assertEquals(Quiz.MATH, g.next(QuizGame.SUBJECT_MATH, Quiz.SYLLABLE).kind);
        }
    }

    @Test
    public void quizRejectsBrokenInput() {
        try {
            new Quiz(Quiz.SYLLABLE, "b + a = ?", 64f, "", new String[]{"ba", "bo", "be"}, false, 40f, 0, "x");
            throw new AssertionError("phải từ chối câu thiếu đáp án");
        } catch (IllegalArgumentException expected) {
            // đúng
        }
        Map<String, Integer> ignore = new HashMap<>();
        assertTrue(ignore.isEmpty());
    }
}
