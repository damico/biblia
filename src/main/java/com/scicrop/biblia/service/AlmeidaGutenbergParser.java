package com.scicrop.biblia.service;

import com.scicrop.biblia.model.ParsedPassage;
import com.scicrop.biblia.model.PassageAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Old Testament of the Project Gutenberg transcription of the Almeida 1911 (eBook #62383).
 *
 * The text is a sequence of paragraphs separated by blank lines. A book starts with an all capitals title
 * ("O LIVRO DE JOSUÉ."); three or more blank lines end a chapter. The first verse of a chapter carries
 * the chapter number instead of 1 (a drop cap). After the verses of a chapter come its cross references
 * ("[1] Pro. 8.23."), numbered from 1 in each chapter. Lettered markers ("[A]") point to the alternatives
 * listed once at the end of the file under NOTAS, lettered across the whole Bible. Words in italics
 * (_..._) were supplied by the translators. Psalm titles are unnumbered paragraphs before verse 1;
 * Lamentations prefixes some verses with the Hebrew letter of the acrostic ("ALEPH. 1 ...").
 * Chapter summaries and section headings (whole paragraphs in italics) and the chronology
 * ("[Antes de Christo 4004]") are editorial and are skipped.
 */
public final class AlmeidaGutenbergParser {

    private static final Logger log = LoggerFactory.getLogger(AlmeidaGutenbergParser.class);

    private static final String START = "*** START OF THE PROJECT GUTENBERG EBOOK";
    private static final String OLD_TESTAMENT_END = "FIM DO VELHO TESTAMENTO.";
    private static final String NOTES_TITLE = "NOTAS";

    private static final Pattern BOOK_TITLE = Pattern.compile("^(?:\\[([A-Z]+)\\] )?\\p{Lu}[\\p{Lu} ,.'\u2019-]{2,}\\.$");
    private static final Pattern VERSE = Pattern.compile("^(?:(\\p{Lu}+)\\. )?((?:\\[\\d+\\] )?)(\\d+) (.*)$");
    private static final Pattern CROSS_REFERENCE = Pattern.compile("^\\[(\\d+)\\] (.*)$");
    private static final Pattern NOTE = Pattern.compile("^\\[([A-Z]+)\\] (.*)$");
    private static final Pattern CHRONOLOGY = Pattern.compile("^\\[[^\\]]*(?:Christo|Domini)[^\\]]*\\]$");
    private static final Pattern SECTION_HEADING = Pattern.compile("^_.*_$");
    private static final Pattern MARKER = Pattern.compile("\\[(\\d+|[A-Z]+)\\]");
    // A query of the transcriber about an unclear letter, as in "o[_o_?]" (Num 5:3).
    private static final Pattern TRANSCRIBER_QUERY = Pattern.compile("\\[_[^\\]]*_\\?\\]");
    private static final Pattern CLOSING_PUNCTUATION = Pattern.compile("[,.;:!?)]");

    /** A verse as written in the source, before italics and markers are turned into annotations. */
    private static final class RawVerse {
        final int number;
        String text;
        String heading;
        final List<PassageAnnotation> titleNotes = new ArrayList<>();

        RawVerse(int number, String text) {
            this.number = number;
            this.text = text;
        }
    }

    private static final class RawChapter {
        final List<RawVerse> verses = new ArrayList<>();
        final Map<Integer, String> crossReferences = new HashMap<>();
    }

    private static final class RawBook {
        final String title;
        final String titleNote;
        final List<RawChapter> chapters = new ArrayList<>();

        RawBook(String title, String titleNote) {
            this.title = title;
            this.titleNote = titleNote;
        }
    }

    public record Result(List<ParsedPassage> passages, int repairs) {
    }

    private final List<RawBook> books = new ArrayList<>();
    private final Map<String, String> notes;
    private int repairs;

    private AlmeidaGutenbergParser(Map<String, String> notes) {
        this.notes = notes;
    }

    /** Parses the Old Testament; bookCodes are the USFM codes of its books in canonical order. */
    public static Result parse(String text, List<String> bookCodes) {
        String normalized = text.replace("\r", "");
        int start = normalized.indexOf(START);
        int end = normalized.indexOf(OLD_TESTAMENT_END);
        int notesStart = normalized.lastIndexOf("\n" + NOTES_TITLE + "\n");
        if (start < 0 || end < 0 || notesStart < end) {
            throw new IllegalArgumentException("Not the Gutenberg Almeida 1911 file: markers not found");
        }
        AlmeidaGutenbergParser parser = new AlmeidaGutenbergParser(notes(normalized.substring(notesStart)));
        String body = normalized.substring(start, end);
        // the book index before Genesis is not text
        int genesis = body.indexOf("\nO PRIMEIRO LIVRO DE MOYS");
        parser.readStructure(body.substring(genesis + 1));
        if (parser.books.size() != bookCodes.size()) {
            throw new IllegalArgumentException("Expected " + bookCodes.size() + " books, found " + parser.books.size());
        }
        List<ParsedPassage> passages = new ArrayList<>();
        for (int b = 0; b < bookCodes.size(); b++) {
            passages.addAll(parser.toPassages(bookCodes.get(b), parser.books.get(b)));
        }
        return new Result(passages, parser.repairs);
    }

    /** The lettered alternatives at the end of the file, by letter. */
    private static Map<String, String> notes(String section) {
        Map<String, String> notes = new HashMap<>();
        for (Paragraph paragraph : paragraphs(section)) {
            Matcher m = NOTE.matcher(paragraph.text());
            if (m.matches()) {
                notes.put(m.group(1), m.group(2));
            }
        }
        return notes;
    }

    /** A paragraph, its lines joined by single spaces, and the number of blank lines before it. */
    private record Paragraph(String text, int blanksBefore) {
    }

    private static List<Paragraph> paragraphs(String text) {
        List<Paragraph> result = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        int blanks = 0;
        for (String line : (text + "\n\n").split("\n", -1)) {
            if (line.isBlank()) {
                if (!paragraph.isEmpty()) {
                    result.add(new Paragraph(paragraph.toString(), blanks));
                    paragraph.setLength(0);
                    blanks = 0;
                }
                blanks++;
            } else {
                paragraph.append(paragraph.isEmpty() ? "" : " ").append(line.strip());
            }
        }
        return result;
    }

    private void readStructure(String text) {
        RawBook book = null;
        boolean chapterBreak = false;
        String pendingHeading = null;
        for (Paragraph p : paragraphs(text)) {
            String paragraph = p.text();
            if (p.blanksBefore() >= 3) {
                chapterBreak = true;
            }
            Matcher title = BOOK_TITLE.matcher(paragraph);
            Matcher verse = VERSE.matcher(paragraph);
            Matcher crossReference = CROSS_REFERENCE.matcher(paragraph);
            if (title.matches()) {
                book = new RawBook(paragraph, title.group(1));
                books.add(book);
                chapterBreak = true;
            } else if (book == null) {
                throw new IllegalArgumentException("Text before the first book: " + paragraph);
            } else if (verse.matches()) {
                int number = Integer.parseInt(verse.group(3));
                if (chapterBreak || book.chapters.isEmpty()) {
                    book.chapters.add(new RawChapter());
                    chapterBreak = false;
                    number = checkChapterNumber(book, number, paragraph);
                }
                RawVerse raw = addVerse(book, number, verse.group(2) + verse.group(4));
                String acrostic = verse.group(1);
                raw.heading = join(pendingHeading, acrostic == null ? null
                        : acrostic.charAt(0) + acrostic.substring(1).toLowerCase());
                pendingHeading = null;
            } else if (crossReference.matches()) {
                if (book.chapters.isEmpty()) {
                    throw new IllegalArgumentException("Cross reference before any verse: " + paragraph);
                }
                book.chapters.getLast().crossReferences.put(Integer.parseInt(crossReference.group(1)),
                        crossReference.group(2));
            } else if (CHRONOLOGY.matcher(paragraph).matches() || SECTION_HEADING.matcher(paragraph).matches()) {
                // editorial, not imported
            } else if (book.title.contains("PSALMOS")) {
                pendingHeading = join(pendingHeading, paragraph);
            } else {
                throw new IllegalArgumentException("Unexpected paragraph in " + book.title + ": " + paragraph);
            }
        }
    }

    private static String join(String first, String second) {
        if (first == null) {
            return second;
        }
        return second == null ? first : first + " " + second;
    }

    /** The first verse of a chapter is numbered with the chapter number; it is verse 1. */
    private static int checkChapterNumber(RawBook book, int number, String paragraph) {
        if (number != book.chapters.size()) {
            throw new IllegalArgumentException(book.title + ": chapter " + book.chapters.size()
                    + " starts with number " + number + ": " + paragraph);
        }
        return 1;
    }

    /**
     * Adds a verse to the current chapter. When numbers are skipped, the missing verses were typeset
     * inside the previous paragraph ("... mantimento. 5 Não voltará ...", Hos 11:4-5) and are split out.
     */
    private RawVerse addVerse(RawBook book, int number, String text) {
        List<RawVerse> verses = book.chapters.getLast().verses;
        int expected = verses.size() + 1;
        while (number > expected && !verses.isEmpty()) {
            RawVerse previous = verses.getLast();
            Matcher inner = Pattern.compile("(?<=[.;:!?)] )" + expected + " (?=[\\p{Lu}_\\[])").matcher(previous.text);
            if (!inner.find()) {
                break;
            }
            String rest = previous.text.substring(inner.end());
            previous.text = previous.text.substring(0, inner.start()).stripTrailing();
            verses.add(new RawVerse(expected, rest));
            repairs++;
            log.warn("Split {} {}:{} out of verse {}", book.title, book.chapters.size(), expected, previous.number);
            expected++;
        }
        if (number != expected) {
            throw new IllegalArgumentException(book.title + " " + book.chapters.size() + ": verse " + number
                    + " where " + expected + " was expected");
        }
        RawVerse verse = new RawVerse(number, text);
        verses.add(verse);
        return verse;
    }

    private List<ParsedPassage> toPassages(String usfmCode, RawBook book) {
        List<ParsedPassage> passages = new ArrayList<>();
        for (int c = 0; c < book.chapters.size(); c++) {
            RawChapter chapter = book.chapters.get(c);
            String where = usfmCode + " " + (c + 1);
            for (RawVerse verse : chapter.verses) {
                List<PassageAnnotation> annotations = new ArrayList<>();
                if (c == 0 && verse.number == 1 && book.titleNote != null) {
                    annotations.add(new PassageAnnotation("footnote", 0, 0, "Título do livro",
                            note(book.titleNote, where)));
                }
                String heading = verse.heading;
                if (heading != null) {
                    // markers in a psalm title become notes of its first verse
                    Matcher m = MARKER.matcher(heading);
                    while (m.find()) {
                        annotations.add(markerAnnotation(m.group(1), 0, "Título", chapter, where));
                    }
                    heading = m.replaceAll("").replaceAll("\\s+", " ").replace(" .", ".").strip();
                }
                String content = convert(verse.text, annotations, chapter, where + ":" + verse.number);
                passages.add(new ParsedPassage(usfmCode, c + 1, verse.number, heading, content, annotations));
            }
        }
        return passages;
    }

    /** Removes italics and note markers from a verse, recording them as annotations by code point offset. */
    private String convert(String text, List<PassageAnnotation> annotations, RawChapter chapter, String where) {
        Matcher query = TRANSCRIBER_QUERY.matcher(text);
        if (query.find()) {
            text = query.replaceAll("");
            repairs++;
            log.warn("Removed a transcriber query at {}", where);
        }
        StringBuilder content = new StringBuilder();
        int addedStart = -1;
        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (ch == '_') {
                int offset = content.codePointCount(0, content.length());
                if (addedStart < 0) {
                    addedStart = offset;
                } else {
                    if (offset > addedStart) {
                        annotations.add(new PassageAnnotation("added", addedStart, offset, null, null));
                    }
                    addedStart = -1;
                }
                i++;
                continue;
            }
            Matcher marker = MARKER.matcher(text).region(i, text.length());
            if (ch == '[' && marker.lookingAt()) {
                i = marker.end();
                boolean atWordStart = content.isEmpty() || content.charAt(content.length() - 1) == ' '
                        || content.charAt(content.length() - 1) == '(';
                if (atWordStart && i < text.length() && text.charAt(i) == ' ') {
                    i++;
                }
                if (i < text.length() && CLOSING_PUNCTUATION.matcher(String.valueOf(text.charAt(i))).matches()
                        && !content.isEmpty() && content.charAt(content.length() - 1) == ' ') {
                    content.setLength(content.length() - 1);
                }
                int offset = content.codePointCount(0, content.length());
                annotations.add(markerAnnotation(marker.group(1), offset, null, chapter, where));
                continue;
            }
            if (ch == '[') {
                throw new IllegalArgumentException("Unknown bracket at " + where + ": " + text);
            }
            content.append(ch);
            i++;
        }
        if (addedStart >= 0) {
            throw new IllegalArgumentException("Unclosed italics at " + where + ": " + text);
        }
        String result = content.toString();
        if (!result.equals(result.strip()) || result.contains("  ")) {
            throw new IllegalArgumentException("Spacing left by markers at " + where + ": " + result);
        }
        return result;
    }

    private PassageAnnotation markerAnnotation(String label, int offset, String keyText, RawChapter chapter,
                                               String where) {
        if (Character.isDigit(label.charAt(0))) {
            String body = chapter.crossReferences.get(Integer.parseInt(label));
            if (body == null) {
                throw new IllegalArgumentException("Cross reference [" + label + "] not listed in " + where);
            }
            return new PassageAnnotation("crossref", offset, offset, keyText, body);
        }
        return new PassageAnnotation("footnote", offset, offset, keyText, note(label, where));
    }

    private String note(String label, String where) {
        String body = notes.get(label);
        if (body == null) {
            throw new IllegalArgumentException("Note [" + label + "] of " + where + " not found under NOTAS");
        }
        return body;
    }
}
