package com.scicrop.biblia.service;

import com.scicrop.biblia.model.ParsedAnalysis;
import com.scicrop.biblia.model.ParsedPassage;
import com.scicrop.biblia.model.ParsedToken;
import com.scicrop.biblia.model.ParsedVerse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the MACULA word level TSV (tab separated, no quoting) of MACULA Greek or MACULA Hebrew.
 * Greek files have one line per word. Hebrew files have one line per segment (prefix, stem or
 * suffix), and the segments of a word share its ref; the "lang" column tells Hebrew from Aramaic.
 * The verse text is rebuilt from each word followed by its "after" column.
 */
public final class MaculaParser {

    private static final Pattern REF = Pattern.compile("^([1-3A-Z]{3}) (\\d+):(\\d+)!(\\d+)$");

    private static final char MAQAF = '־';
    private static final char PASEQ = '׀';

    /** Hebrew cantillation marks and meteg, ignored by the normalized form. */
    private static final Pattern CANTILLATION = Pattern.compile("[\\u0591-\\u05AF\\u05BD]");

    private static final Map<String, String> HEBREW_LANGUAGES = Map.of("H", "hbo", "A", "arc");

    private MaculaParser() {
    }

    public static List<ParsedVerse> parse(List<String> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Empty MACULA file");
        }
        Map<String, Integer> columns = new HashMap<>();
        String[] header = lines.getFirst().split("\t", -1);
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i], i);
        }
        boolean hebrew = columns.containsKey("lang");

        List<ParsedVerse> verses = new ArrayList<>();
        String currentVerse = null;
        String usfmCode = null;
        int chapter = 0;
        int verse = 0;
        List<ParsedToken> tokens = new ArrayList<>();
        String currentWord = null;
        int wordPosition = 0;
        List<Row> wordRows = new ArrayList<>();

        for (int lineNumber = 1; lineNumber <= lines.size(); lineNumber++) {
            Row row = null;
            String wordKey = null;
            String verseKey = null;
            Matcher ref = null;
            if (lineNumber < lines.size()) {
                String line = lines.get(lineNumber);
                if (line.isBlank()) {
                    continue;
                }
                String[] cells = line.split("\t", -1);
                if (cells.length != header.length) {
                    throw new IllegalArgumentException("Line " + (lineNumber + 1) + ": expected " + header.length
                            + " columns, found " + cells.length);
                }
                row = new Row(columns, cells);
                wordKey = row.raw("ref").strip();
                ref = REF.matcher(wordKey);
                if (!ref.matches()) {
                    throw new IllegalArgumentException("Line " + (lineNumber + 1) + ": invalid ref " + wordKey);
                }
                verseKey = ref.group(1) + " " + ref.group(2) + ":" + ref.group(3);
            }

            // A word ends when the ref changes (or at the end of the file).
            if (currentWord != null && !currentWord.equals(wordKey)) {
                tokens.add(hebrew ? hebrewToken(wordRows, wordPosition) : greekToken(wordRows.getFirst(), wordPosition));
                wordRows = new ArrayList<>();
            }
            if (currentVerse != null && !currentVerse.equals(verseKey)) {
                verses.add(buildVerse(usfmCode, chapter, verse, tokens));
                tokens = new ArrayList<>();
            }
            if (row == null) {
                break;
            }
            if (!verseKey.equals(currentVerse)) {
                currentVerse = verseKey;
                usfmCode = ref.group(1);
                chapter = Integer.parseInt(ref.group(2));
                verse = Integer.parseInt(ref.group(3));
            }
            // Word numbers come from the source and may have gaps (a Ketiv left out in favour of its Qere).
            if (!wordKey.equals(currentWord)) {
                int number = Integer.parseInt(ref.group(4));
                int previous = tokens.isEmpty() ? 0 : tokens.getLast().position();
                if (number <= previous) {
                    throw new IllegalArgumentException("Line " + (lineNumber + 1) + ": word out of order in "
                            + verseKey);
                }
                wordPosition = number;
            }
            currentWord = wordKey;
            wordRows.add(row);
        }
        return verses;
    }

    private static ParsedToken greekToken(Row row, int position) {
        ParsedAnalysis analysis = new ParsedAnalysis(
                1, null, "grc",
                row.get("lemma"),
                row.get("class"),
                row.optional("morph"),
                row.optional("person"),
                row.optional("number"),
                row.optional("gender"),
                row.optional("case"),
                row.optional("tense"),
                row.optional("voice"),
                row.optional("mood"),
                row.optional("degree"),
                null, null, null,
                row.optional("strong"),
                row.optional("gloss"),
                null, null, null);
        return new ParsedToken(row.get("xml:id"), position, row.get("text"), row.raw("after"),
                row.get("normalized"), List.of(analysis));
    }

    private static ParsedToken hebrewToken(List<Row> rows, int position) {
        StringBuilder surface = new StringBuilder();
        List<ParsedAnalysis> analyses = new ArrayList<>();
        for (Row row : rows) {
            String text = row.raw("text");
            surface.append(text);
            String language = HEBREW_LANGUAGES.get(row.get("lang"));
            if (language == null) {
                throw new IllegalArgumentException("Unknown lang " + row.raw("lang") + " in " + row.raw("ref"));
            }
            analyses.add(new ParsedAnalysis(
                    analyses.size() + 1, text, language,
                    row.get("lemma"),
                    row.get("pos"),
                    row.optional("morph"),
                    known(row.optional("person")),
                    known(row.optional("number")),
                    known(row.optional("gender")),
                    null, null, null, null, null,
                    known(row.optional("stem")),
                    "verb".equals(row.raw("pos")) ? row.optional("type") : null,
                    row.optional("state"),
                    row.optional("strongnumberx"),
                    row.optional("english"),
                    row.optional("transliteration"),
                    row.optional("greek"),
                    row.optional("greekstrong")));
        }
        String after = rows.getLast().raw("after");
        String trailing = after.equals(String.valueOf(PASEQ)) ? " " + PASEQ + " " : after;
        String word = surface.toString();
        // No Unicode normalization: canonical reordering would change the order of Hebrew points.
        String normalized = CANTILLATION.matcher(word).replaceAll("");
        return new ParsedToken(rows.getFirst().get("xml:id").substring(0, 12), position, word, trailing,
                normalized, List.copyOf(analyses));
    }

    // Word id: "o" + book, chapter, verse and word (12 characters); the 13th is the segment.
    /** MACULA Hebrew marks undetermined values as "unknown: x"; they are stored as null. */
    private static String known(String value) {
        return value == null || value.startsWith("unknown") ? null : value;
    }

    private static ParsedVerse buildVerse(String usfmCode, int chapter, int verse, List<ParsedToken> tokens) {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            ParsedToken token = tokens.get(i);
            content.append(token.surface()).append(token.trailing());
            boolean last = i == tokens.size() - 1;
            boolean joined = token.trailing().endsWith(" ") || token.trailing().endsWith(String.valueOf(MAQAF));
            if (!last && !joined) {
                content.append(' ');
            }
        }
        ParsedPassage passage = new ParsedPassage(usfmCode, chapter, verse, null, content.toString().strip(), List.of());
        return new ParsedVerse(passage, List.copyOf(tokens));
    }

    private record Row(Map<String, Integer> columns, String[] cells) {

        String raw(String name) {
            Integer index = columns.get(name);
            if (index == null) {
                throw new IllegalArgumentException("Missing column: " + name);
            }
            return cells[index];
        }

        String get(String name) {
            String value = raw(name).strip();
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Empty " + name + " in " + raw("ref"));
            }
            return value;
        }

        String optional(String name) {
            String value = raw(name).strip();
            return value.isEmpty() ? null : value;
        }
    }
}
