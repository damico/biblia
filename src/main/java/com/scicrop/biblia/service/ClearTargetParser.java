package com.scicrop.biblia.service;

import com.scicrop.biblia.model.ParsedPassage;
import com.scicrop.biblia.model.ParsedToken;
import com.scicrop.biblia.model.ParsedVerse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a translation token file of Clear Bible Alignments (data/LANG/targets/ID/*.tsv).
 * Token ids are BBCCCVVVWWW (book number in the 66-book order, chapter, verse, word); the
 * verse text is rebuilt from the tokens, with a space unless skip_space_after is set.
 */
public final class ClearTargetParser {

    private ClearTargetParser() {
    }

    /** @param usfmByBookNumber USFM code of each book, index 0 being book number 1 */
    public static List<ParsedVerse> parse(List<String> lines, List<String> usfmByBookNumber) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Empty token file");
        }
        Map<String, Integer> columns = new HashMap<>();
        String[] header = lines.getFirst().split("\t", -1);
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i], i);
        }
        int idColumn = column(columns, "id");
        int textColumn = column(columns, "text");
        int skipColumn = column(columns, "skip_space_after");

        List<ParsedVerse> verses = new ArrayList<>();
        String currentVerse = null;
        List<ParsedToken> tokens = new ArrayList<>();
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (line.isBlank()) {
                continue;
            }
            String[] cells = line.split("\t", -1);
            String id = cells[idColumn].strip();
            if (!id.matches("\\d{11}")) {
                throw new IllegalArgumentException("Line " + (lineNumber + 1) + ": invalid token id " + id);
            }
            String verseKey = id.substring(0, 8);
            if (!verseKey.equals(currentVerse)) {
                if (currentVerse != null) {
                    verses.add(buildVerse(currentVerse, tokens, usfmByBookNumber));
                }
                currentVerse = verseKey;
                tokens = new ArrayList<>();
            }
            String text = cells[textColumn];
            String trailing = "y".equals(cells[skipColumn].strip()) ? "" : " ";
            tokens.add(new ParsedToken(id, tokens.size() + 1, text, trailing, text, List.of()));
        }
        if (currentVerse != null) {
            verses.add(buildVerse(currentVerse, tokens, usfmByBookNumber));
        }
        return verses;
    }

    private static ParsedVerse buildVerse(String verseKey, List<ParsedToken> tokens, List<String> usfmByBookNumber) {
        int bookNumber = Integer.parseInt(verseKey.substring(0, 2));
        if (bookNumber < 1 || bookNumber > usfmByBookNumber.size()) {
            throw new IllegalArgumentException("Unknown book number " + bookNumber);
        }
        int chapter = Integer.parseInt(verseKey.substring(2, 5));
        int verse = Integer.parseInt(verseKey.substring(5, 8));
        StringBuilder content = new StringBuilder();
        tokens.forEach(t -> content.append(t.surface()).append(t.trailing()));
        ParsedPassage passage = new ParsedPassage(usfmByBookNumber.get(bookNumber - 1), chapter, verse, null,
                content.toString().strip(), List.of());
        return new ParsedVerse(passage, List.copyOf(tokens));
    }

    private static int column(Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (index == null) {
            throw new IllegalArgumentException("Missing column: " + name);
        }
        return index;
    }
}
