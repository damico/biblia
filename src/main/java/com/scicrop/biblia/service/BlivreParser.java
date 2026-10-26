package com.scicrop.biblia.service;

import com.scicrop.biblia.model.ParsedPassage;
import com.scicrop.biblia.model.PassageAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses one book file of the Biblia Livre "f4" source format.
 *
 * Markers sit at the start of a line: "\v Gn.1.1" opens a verse, "\name" opens
 * and "\*name" closes a span. Line breaks are whitespace. The source has a few
 * malformed spans (missing backslash, "\fn" used as closer, unclosed spans);
 * those are repaired and each repair is logged and counted.
 */
public class BlivreParser {

    private static final Logger log = LoggerFactory.getLogger(BlivreParser.class);

    private static final Pattern MARKER = Pattern.compile(
            "^\\s*(\\\\\\*|\\*|\\\\)(v|added|fn|key|ref|psalm-title|name-long|name-short|abbreviation|ubs-code)(?![a-z-])(.*)$");
    private static final Pattern VERSE_REF = Pattern.compile("^\\s*\\S+\\.(\\d+)\\.(\\d+)\\s*$");
    private static final Pattern LEADING_PUNCTUATION = Pattern.compile("^[,.;:!?)\\]\u201D\u2019]");

    private enum Mode { TEXT, ADDED, FOOTNOTE, KEY, REF, PSALM_TITLE, HEADER_FIELD }

    private final String fileName;
    private final List<ParsedPassage> passages = new ArrayList<>();
    private int repairs;

    private String usfmCode;
    private String headerField;
    private final StringBuilder headerValue = new StringBuilder();

    private int chapter = -1;
    private int verse = -1;
    private String heading;
    private String pendingHeading;
    private final StringBuilder content = new StringBuilder();
    private final List<PassageAnnotation> annotations = new ArrayList<>();

    private Mode mode = Mode.TEXT;
    private int addedStart = -1;
    private int anchor;
    private final StringBuilder noteBody = new StringBuilder();
    private final StringBuilder keyText = new StringBuilder();
    private final StringBuilder psalmTitle = new StringBuilder();

    private BlivreParser(String fileName) {
        this.fileName = fileName;
    }

    public record Result(String usfmCode, List<ParsedPassage> passages, int repairs) {
    }

    public static Result parse(String fileName, String text) {
        BlivreParser parser = new BlivreParser(fileName);
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        for (String line : text.split("\r?\n", -1)) {
            parser.line(line);
        }
        parser.finishVerse();
        if (parser.usfmCode == null) {
            throw new IllegalArgumentException(fileName + ": missing \\ubs-code header");
        }
        return new Result(parser.usfmCode, parser.passages, parser.repairs);
    }

    private void line(String line) {
        Matcher m = MARKER.matcher(line);
        if (!m.matches()) {
            text(line);
            return;
        }
        String prefix = m.group(1);
        String name = m.group(2);
        String rest = m.group(3);
        boolean closing = prefix.contains("*");
        if (prefix.equals("*")) {
            repair("marker without backslash: " + line.strip());
        }

        if (name.equals("v")) {
            startVerse(rest);
            return;
        }
        switch (name) {
            case "added" -> added(closing);
            case "fn" -> footnote(closing);
            case "key" -> key(closing);
            case "ref" -> reference(closing);
            case "psalm-title" -> psalmTitle(closing);
            default -> headerField(name, closing);
        }
        if (!rest.isBlank()) {
            text(rest);
        }
    }

    private void text(String line) {
        switch (mode) {
            case TEXT -> appendContent(line);
            case ADDED -> {
                int start = appendContent(line);
                if (addedStart < 0 && start >= 0) {
                    addedStart = start;
                }
            }
            case FOOTNOTE, REF -> appendWords(noteBody, line);
            case KEY -> appendWords(keyText, line);
            case PSALM_TITLE -> appendWords(psalmTitle, line);
            case HEADER_FIELD -> appendWords(headerValue, line);
        }
    }

    private void added(boolean closing) {
        if (closing) {
            if (mode == Mode.ADDED) {
                closeAdded();
            } else {
                repair("stray \\*added");
            }
            return;
        }
        if (mode == Mode.ADDED) {
            repair("\\added inside \\added, previous span closed");
            closeAdded();
        } else if (mode == Mode.FOOTNOTE) {
            repair("\\added inside \\fn, footnote text moved back to the verse");
            mode = Mode.TEXT;
            appendContent(noteBody.toString());
            noteBody.setLength(0);
            keyText.setLength(0);
        } else if (mode != Mode.TEXT) {
            closeOpenSpan("\\added");
        }
        mode = Mode.ADDED;
        addedStart = -1;
    }

    private void closeAdded() {
        if (addedStart >= 0) {
            annotations.add(new PassageAnnotation("added", addedStart, length(), null, null));
        }
        mode = Mode.TEXT;
        addedStart = -1;
    }

    private void footnote(boolean closing) {
        if (closing) {
            if (mode == Mode.FOOTNOTE || mode == Mode.KEY) {
                closeFootnote();
            } else {
                repair("stray \\*fn");
            }
            return;
        }
        if (mode == Mode.FOOTNOTE || mode == Mode.KEY) {
            repair("\\fn used to close a footnote");
            closeFootnote();
            return;
        }
        if (mode != Mode.TEXT) {
            closeOpenSpan("\\fn");
        }
        mode = Mode.FOOTNOTE;
        anchor = length();
    }

    private void closeFootnote() {
        String key = keyText.isEmpty() ? null : keyText.toString();
        annotations.add(new PassageAnnotation("footnote", anchor, anchor, key, noteBody.toString()));
        noteBody.setLength(0);
        keyText.setLength(0);
        mode = Mode.TEXT;
    }

    private void key(boolean closing) {
        if (closing) {
            if (mode == Mode.KEY) {
                mode = Mode.FOOTNOTE;
            } else {
                repair("stray \\*key");
            }
        } else if (mode == Mode.FOOTNOTE) {
            mode = Mode.KEY;
        } else {
            repair("\\key outside \\fn ignored");
        }
    }

    private void reference(boolean closing) {
        if (closing) {
            if (mode == Mode.REF) {
                annotations.add(new PassageAnnotation("reference", anchor, anchor, null, noteBody.toString()));
                noteBody.setLength(0);
                mode = Mode.TEXT;
            } else {
                repair("stray \\*ref");
            }
            return;
        }
        if (mode != Mode.TEXT) {
            closeOpenSpan("\\ref");
        }
        mode = Mode.REF;
        anchor = length();
    }

    private void psalmTitle(boolean closing) {
        if (closing) {
            if (mode == Mode.PSALM_TITLE) {
                pendingHeading = psalmTitle.toString();
                psalmTitle.setLength(0);
                mode = Mode.TEXT;
            } else {
                repair("stray \\*psalm-title");
            }
            return;
        }
        if (mode != Mode.TEXT) {
            closeOpenSpan("\\psalm-title");
        }
        mode = Mode.PSALM_TITLE;
    }

    private void headerField(String name, boolean closing) {
        if (!closing) {
            headerField = name;
            headerValue.setLength(0);
            mode = Mode.HEADER_FIELD;
            return;
        }
        if (name.equals(headerField) && name.equals("ubs-code")) {
            usfmCode = headerValue.toString().strip();
        }
        headerField = null;
        mode = Mode.TEXT;
    }

    private void startVerse(String reference) {
        finishVerse();
        Matcher m = VERSE_REF.matcher(reference);
        if (!m.matches()) {
            throw new IllegalArgumentException(fileName + ": invalid verse reference '" + reference + "'");
        }
        chapter = Integer.parseInt(m.group(1));
        verse = Integer.parseInt(m.group(2));
        heading = pendingHeading;
        pendingHeading = null;
    }

    private void finishVerse() {
        if (mode != Mode.TEXT) {
            closeOpenSpan("end of verse");
        }
        if (chapter >= 0) {
            passages.add(new ParsedPassage(usfmCode, chapter, verse, heading, content.toString(),
                    List.copyOf(annotations)));
        }
        content.setLength(0);
        annotations.clear();
        chapter = -1;
        heading = null;
    }

    private void closeOpenSpan(String at) {
        repair("unclosed " + mode + " closed at " + at);
        switch (mode) {
            case ADDED -> closeAdded();
            case FOOTNOTE, KEY -> closeFootnote();
            case REF -> reference(true);
            case PSALM_TITLE -> psalmTitle(true);
            default -> mode = Mode.TEXT;
        }
    }

    /** Appends a line to the verse content and returns the offset where its text starts, or -1 if blank. */
    private int appendContent(String line) {
        String words = line.strip().replaceAll("\\s+", " ");
        if (words.isEmpty()) {
            return -1;
        }
        if (!content.isEmpty() && !LEADING_PUNCTUATION.matcher(words).find()) {
            content.append(' ');
        }
        int start = length();
        content.append(words);
        return start;
    }

    private static void appendWords(StringBuilder target, String line) {
        String words = line.strip().replaceAll("\\s+", " ");
        if (words.isEmpty()) {
            return;
        }
        if (!target.isEmpty() && !LEADING_PUNCTUATION.matcher(words).find()) {
            target.append(' ');
        }
        target.append(words);
    }

    private int length() {
        return content.codePointCount(0, content.length());
    }

    private void repair(String message) {
        repairs++;
        String where = chapter >= 0 ? usfmCode + " " + chapter + ":" + verse : fileName;
        log.warn("Repaired markup at {}: {}", where, message);
    }
}
