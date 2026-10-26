package com.scicrop.biblia.service;

import com.scicrop.biblia.model.StandardRef;
import com.scicrop.biblia.model.VerseStat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rules of the "Expanded" section of STEPBible TVTMS. Each rule says: if its tests are true for an
 * edition, the edition's SourceRef holds the text of StandardRef, with an action (Keep verse,
 * Renumber verse, Concatenation, ...). Tests look only at the edition itself: whether verses exist,
 * which verse is the last of a chapter, word counts and whether a psalm title precedes verse 1.
 */
public final class TvtmsRules {

    public record Rule(String type, String source, String standard, String action, String tests) {
    }

    /** A rule test that cannot be read (a few lines of the source have typos); the rule then does not apply. */
    public static class UnreadableTest extends RuntimeException {
        UnreadableTest(String test) {
            super(test);
        }
    }

    private static final Pattern REF = Pattern.compile(
            "^([1-4]?[A-Za-z]{2,3})\\.([A-Z]|\\d+):(\\d+|TextBeforeV1|Title)(?:[.!](\\d+|[a-z]))?$");
    private static final Pattern CONDITION = Pattern.compile("^(.+)=(Exist|NotExist|Last)$");
    private static final Pattern COMPARISON = Pattern.compile("^(.+?)([<>])(.+)$");
    private static final Pattern MULTIPLIED = Pattern.compile("^(.+)\\*(\\d+)$");
    private static final Pattern STANDARD_PART = Pattern.compile(
            "^(?:([1-4]?[A-Za-z]{2,3})\\.)?(\\d+):(\\d+|Title)(?:!([a-z]))?(?:-(\\d+))?$");

    private TvtmsRules() {
    }

    /** Reads the rules between #DataStart(Expanded) and #DataEnd(Expanded). */
    public static List<Rule> parse(List<String> lines) {
        List<Rule> rules = new ArrayList<>();
        boolean inside = false;
        boolean header = false;
        for (String line : lines) {
            if (line.startsWith("#DataStart(Expanded)")) {
                inside = true;
                header = true;
                continue;
            }
            if (line.startsWith("#DataEnd(Expanded)")) {
                break;
            }
            if (!inside || line.isBlank() || "'#$".indexOf(line.charAt(0)) >= 0) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }
            String[] cells = line.split("\t", -1);
            if (cells.length < 9) {
                continue;
            }
            rules.add(new Rule(cells[0].strip(), cells[1].strip(), cells[2].strip(), cells[3].strip(),
                    cells[8].strip()));
        }
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("No TVTMS expanded rules found");
        }
        return rules;
    }

    /** TVTMS book code (Gen, 1Sa, Sng) for a database USFM code (GEN, 1SA, SNG): same letters, other case. */
    public static String tvtmsBook(String usfmCode) {
        int firstLetter = Character.isDigit(usfmCode.charAt(0)) ? 1 : 0;
        return usfmCode.substring(0, firstLetter + 1) + usfmCode.substring(firstLetter + 1).toLowerCase(Locale.ROOT);
    }

    /** Verse data of one edition, keyed for the tests. */
    public static final class Edition {

        private final Map<String, VerseStat> verses = new HashMap<>();
        private final Map<String, Integer> lastVerse = new HashMap<>();

        public Edition(List<VerseStat> stats) {
            for (VerseStat v : stats) {
                verses.put(key(v.usfmCode(), v.chapter(), v.verse()), v);
                lastVerse.merge(v.usfmCode() + " " + v.chapter(), v.verse(), Math::max);
            }
        }

        private static String key(String usfm, int chapter, int verse) {
            return usfm + " " + chapter + ":" + verse;
        }

        boolean exists(String usfm, int chapter, int verse) {
            VerseStat v = verses.get(key(usfm, chapter, verse));
            return v != null && v.words() > 0;
        }

        boolean isLast(String usfm, int chapter, int verse) {
            return exists(usfm, chapter, verse) && Integer.valueOf(verse).equals(lastVerse.get(usfm + " " + chapter));
        }

        boolean hasTitle(String usfm, int chapter) {
            VerseStat v = verses.get(key(usfm, chapter, 1));
            return v != null && v.hasHeading();
        }

        int words(String usfm, int chapter, int verse) {
            VerseStat v = verses.get(key(usfm, chapter, verse));
            return v == null ? 0 : v.words();
        }
    }

    /** True when every test of the rule holds for the edition (a rule without tests always applies). */
    public static boolean applies(Rule rule, Edition edition) {
        if (rule.tests().isEmpty()) {
            return true;
        }
        for (String test : rule.tests().split("\\s*&\\s*")) {
            if (!test.isBlank() && !test(test.strip(), edition)) {
                return false;
            }
        }
        return true;
    }

    private static boolean test(String test, Edition edition) {
        Matcher condition = CONDITION.matcher(test);
        if (condition.matches()) {
            String[] ref = ref(condition.group(1));
            String kind = condition.group(2);
            String usfm = ref[0].toUpperCase(Locale.ROOT);
            if ("TextBeforeV1".equals(ref[2])) {
                boolean title = isNumber(ref[1]) && edition.hasTitle(usfm, Integer.parseInt(ref[1]));
                return "Exist".equals(kind) == title;
            }
            if (!isNumber(ref[1]) || !isNumber(ref[2])) {
                // lettered chapters (Greek Esther) and other forms do not occur in the imported editions
                return "NotExist".equals(kind);
            }
            int chapter = Integer.parseInt(ref[1]);
            int verse = Integer.parseInt(ref[2]);
            if (ref[3] != null && !"0".equals(ref[3])) {
                // the imported editions have no subverses
                return "NotExist".equals(kind);
            }
            return switch (kind) {
                case "Exist" -> edition.exists(usfm, chapter, verse);
                case "Last" -> edition.isLast(usfm, chapter, verse);
                default -> !edition.exists(usfm, chapter, verse);
            };
        }
        Matcher comparison = COMPARISON.matcher(test);
        if (comparison.matches()) {
            int left = words(comparison.group(1), edition);
            int right = words(comparison.group(3), edition);
            return "<".equals(comparison.group(2)) ? left < right : left > right;
        }
        throw new UnreadableTest(test);
    }

    /** Word count of an expression like Mal.4:4+Mal.4:6 or Mal.4:5*2. */
    private static int words(String expression, Edition edition) {
        int total = 0;
        for (String term : expression.split("\\+")) {
            term = term.strip();
            int factor = 1;
            Matcher multiplied = MULTIPLIED.matcher(term);
            if (multiplied.matches()) {
                term = multiplied.group(1);
                factor = Integer.parseInt(multiplied.group(2));
            }
            String[] ref = ref(term);
            if (!isNumber(ref[1]) || !isNumber(ref[2]) || (ref[3] != null && !"0".equals(ref[3]))) {
                continue;
            }
            total += factor * edition.words(ref[0].toUpperCase(Locale.ROOT), Integer.parseInt(ref[1]),
                    Integer.parseInt(ref[2]));
        }
        return total;
    }

    /** Splits Book.Chapter:Verse[.sub|!sub] into its four parts. */
    private static String[] ref(String text) {
        Matcher m = REF.matcher(text.strip());
        if (!m.matches()) {
            throw new UnreadableTest(text);
        }
        return new String[] {m.group(1), m.group(2), m.group(3), m.group(4)};
    }

    private static boolean isNumber(String text) {
        return text != null && !text.isEmpty() && text.chars().allMatch(Character::isDigit);
    }

    /**
     * Reads a StandardRef such as "Psa.3:1", "Psa.3:Title", "3Jn.1:14!b", "Act.19:40-41" or
     * "Isa.63:19; 64:1" (a part without book keeps the previous book). Returns an empty list for
     * "Absent ..." and other forms that name no standard verse.
     */
    public static List<StandardRef> standardRefs(String text) {
        List<StandardRef> refs = new ArrayList<>();
        String book = null;
        for (String part : text.split(";")) {
            Matcher m = STANDARD_PART.matcher(part.strip());
            if (!m.matches()) {
                return List.of();
            }
            if (m.group(1) != null) {
                book = m.group(1).toUpperCase(Locale.ROOT);
            }
            if (book == null) {
                return List.of();
            }
            int chapter = Integer.parseInt(m.group(2));
            String subverse = m.group(4) == null ? "" : m.group(4);
            if ("Title".equals(m.group(3))) {
                refs.add(new StandardRef(book, chapter, 0, ""));
                continue;
            }
            int from = Integer.parseInt(m.group(3));
            int to = m.group(5) == null ? from : Integer.parseInt(m.group(5));
            for (int verse = from; verse <= to; verse++) {
                refs.add(new StandardRef(book, chapter, verse, from == to ? subverse : ""));
            }
        }
        return refs;
    }
}
