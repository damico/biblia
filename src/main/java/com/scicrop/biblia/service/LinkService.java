package com.scicrop.biblia.service;

import com.scicrop.biblia.dto.LinkQuery;
import com.scicrop.biblia.model.BookOption;
import com.scicrop.biblia.model.Edition;
import com.scicrop.biblia.model.EditionBook;
import com.scicrop.biblia.model.LinkGraph;
import com.scicrop.biblia.model.LinkPoint;
import com.scicrop.biblia.repository.BookRepository;
import com.scicrop.biblia.repository.EditionRepository;
import com.scicrop.biblia.repository.LinkRepository;
import com.scicrop.biblia.repository.LinkRepository.KeyColumn;
import com.scicrop.biblia.repository.LinkRepository.Origin;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LinkService {

    private static final String CANON = "protestant";

    /** Edition on the other side of the Septuagint mode, by language of the lemma. */
    private static final String HEBREW_COUNTERPART = "n1904";
    private static final String GREEK_COUNTERPART = "wlc";

    private static final String ALL = "ALL";

    private final EditionRepository editionRepository;
    private final BookRepository bookRepository;
    private final LinkRepository linkRepository;

    public LinkService(EditionRepository editionRepository, BookRepository bookRepository,
                       LinkRepository linkRepository) {
        this.editionRepository = editionRepository;
        this.bookRepository = bookRepository;
        this.linkRepository = linkRepository;
    }

    /** One side of the graph: its edition, books and points. */
    private record Side(Edition edition, int editionId, List<EditionBook> books) {
    }

    @Transactional(readOnly = true)
    public LinkGraph build(String editionCode, LinkQuery query) {
        if (query.lexeme() == null) {
            throw badRequest("Missing lexeme");
        }
        String language = linkRepository.findLexemeLanguage(query.lexeme())
                .orElseThrow(() -> notFound("Unknown lexeme: " + query.lexeme()));
        Side side = side(editionCode);
        Origin origin = null;
        if (query.from() != null) {
            origin = linkRepository.findOrigin(query.from())
                    .filter(o -> o.editionId() == side.editionId())
                    .orElseThrow(() -> badRequest("Token " + query.from() + " is not in " + editionCode));
        }
        String mode = query.mode() != null ? query.mode() : (origin != null ? "fan" : "pairs");
        return switch (mode) {
            case "fan" -> fan(side, query, origin);
            case "pairs" -> pairs(side, query, origin);
            case "lxx" -> lxx(side, query, origin, language);
            default -> throw badRequest("Unknown mode: " + mode);
        };
    }

    private LinkGraph fan(Side side, LinkQuery query, Origin origin) {
        if (origin == null) {
            throw badRequest("The fan mode needs the token the reader started from");
        }
        List<LinkPoint> all = linkRepository.findLexemePoints(side.editionId(), query.lexeme(), codes(side.books()),
                null);
        String rightRange = query.right() != null ? query.right() : ALL;
        List<String> rightBooks = range(side.books(), rightRange);
        LinkPoint originPoint = new LinkPoint(origin.bookCode(), origin.chapter(), origin.verse(), 1, null);
        List<LinkPoint> rightPoints = all.stream()
                .filter(p -> rightBooks.contains(p.bookCode()))
                .filter(p -> !sameVerse(p, originPoint))
                .toList();
        long pairCount = rightPoints.stream().mapToLong(LinkPoint::count).sum();
        return graph("fan", side, side, origin.bookCode(), rightRange, originPoint, List.of(),
                List.of(originPoint), rightPoints, pairCount, options(side.books(), all), options(side.books(), all));
    }

    private LinkGraph pairs(Side side, LinkQuery query, Origin origin) {
        List<LinkPoint> all = linkRepository.findLexemePoints(side.editionId(), query.lexeme(), codes(side.books()),
                null);
        String leftBook = leftBook(query, origin, all);
        String rightRange = query.right() != null ? query.right() : ALL;
        List<String> rightBooks = range(side.books(), rightRange);
        List<LinkPoint> leftPoints = all.stream().filter(p -> p.bookCode().equals(leftBook)).toList();
        List<LinkPoint> rightPoints = all.stream().filter(p -> rightBooks.contains(p.bookCode())).toList();
        // every left occurrence with every right one, except a verse paired with itself
        long pairCount = total(leftPoints) * total(rightPoints);
        Map<String, Long> rightCounts = new HashMap<>();
        rightPoints.forEach(p -> rightCounts.put(verseKey(p), p.count()));
        for (LinkPoint p : leftPoints) {
            pairCount -= p.count() * rightCounts.getOrDefault(verseKey(p), 0L);
        }
        return graph("pairs", side, side, leftBook, rightRange, origin(origin), List.of(),
                leftPoints, rightPoints, pairCount, options(side.books(), all), options(side.books(), all));
    }

    private LinkGraph lxx(Side side, LinkQuery query, Origin origin, String language) {
        boolean greek = "grc".equals(language);
        Side other = side(greek ? GREEK_COUNTERPART : HEBREW_COUNTERPART);
        // Greek lemma: its own Strong numbers. Hebrew or Aramaic lemma: the Septuagint equivalents of its occurrences.
        KeyColumn ownKey = greek ? KeyColumn.STRONG : KeyColumn.LXX_STRONG;
        KeyColumn otherKey = greek ? KeyColumn.LXX_STRONG : KeyColumn.STRONG;

        List<LinkPoint> all = linkRepository.findLexemePoints(side.editionId(), query.lexeme(), codes(side.books()),
                ownKey);
        List<String> strongs = all.stream().map(LinkPoint::key).distinct().sorted().toList();
        String leftBook = leftBook(query, origin, all);
        String rightRange = query.right() != null ? query.right() : ALL;
        List<String> rightBooks = range(other.books(), rightRange);

        List<LinkPoint> otherAll = linkRepository.findStrongPoints(other.editionId(), strongs, codes(other.books()),
                otherKey);
        List<LinkPoint> leftPoints = all.stream().filter(p -> p.bookCode().equals(leftBook)).toList();
        List<LinkPoint> rightPoints = otherAll.stream().filter(p -> rightBooks.contains(p.bookCode())).toList();
        // pairs only between occurrences with the same Strong number
        Map<String, Long> rightByKey = new HashMap<>();
        rightPoints.forEach(p -> rightByKey.merge(p.key(), p.count(), Long::sum));
        long pairCount = leftPoints.stream().mapToLong(p -> p.count() * rightByKey.getOrDefault(p.key(), 0L)).sum();
        return graph("lxx", side, other, leftBook, rightRange, origin(origin), strongs,
                leftPoints, rightPoints, pairCount, options(side.books(), all), options(other.books(), otherAll));
    }

    private LinkGraph graph(String mode, Side left, Side right, String leftRange, String rightRange, LinkPoint origin,
                            List<String> strongs, List<LinkPoint> leftPoints, List<LinkPoint> rightPoints,
                            long pairCount, List<BookOption> leftBooks, List<BookOption> rightBooks) {
        return new LinkGraph(mode,
                left.edition().code(), left.edition().name(), right.edition().code(), right.edition().name(),
                leftRange, rightRange, origin, strongs,
                linkRepository.findChapters(left.editionId(), List.of(leftRange), CANON),
                linkRepository.findChapters(right.editionId(), range(right.books(), rightRange), CANON),
                leftPoints, rightPoints, pairCount, leftBooks, rightBooks);
    }

    private static LinkPoint origin(Origin origin) {
        return origin == null ? null : new LinkPoint(origin.bookCode(), origin.chapter(), origin.verse(), 1, null);
    }

    private static long total(List<LinkPoint> points) {
        return points.stream().mapToLong(LinkPoint::count).sum();
    }

    private static String verseKey(LinkPoint p) {
        return p.bookCode() + " " + p.chapter() + ":" + p.verse();
    }

    /** Book of the left axis: the one asked for, else the book of the origin, else the one with most occurrences. */
    private static String leftBook(LinkQuery query, Origin origin, List<LinkPoint> all) {
        if (query.left() != null && !query.left().isBlank()) {
            return query.left();
        }
        if (origin != null) {
            return origin.bookCode();
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        all.forEach(p -> counts.merge(p.bookCode(), p.count(), Long::sum));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> notFound("No occurrences to show"));
    }

    /** Book codes of a range: ALL, OT, NT or one book code. */
    private static List<String> range(List<EditionBook> books, String range) {
        return books.stream()
                .filter(b -> ALL.equals(range) || b.testament().equals(range) || b.code().equals(range))
                .map(EditionBook::code)
                .toList();
    }

    private static List<BookOption> options(List<EditionBook> books, List<LinkPoint> points) {
        Map<String, Long> counts = new HashMap<>();
        points.forEach(p -> counts.merge(p.bookCode(), p.count(), Long::sum));
        return books.stream()
                .map(b -> new BookOption(b.code(), b.name(), b.testament(), counts.getOrDefault(b.code(), 0L)))
                .toList();
    }

    private static List<String> codes(List<EditionBook> books) {
        return books.stream().map(EditionBook::code).toList();
    }

    private static boolean sameVerse(LinkPoint a, LinkPoint b) {
        return a.bookCode().equals(b.bookCode()) && a.chapter() == b.chapter() && a.verse() == b.verse();
    }

    private Side side(String editionCode) {
        Edition edition = editionRepository.findByCode(editionCode)
                .orElseThrow(() -> notFound("Unknown edition: " + editionCode));
        int editionId = editionRepository.findIdByCode(editionCode).orElseThrow();
        return new Side(edition, editionId, bookRepository.findByEdition(editionId, CANON));
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
