package com.scicrop.biblia.repository;

import com.scicrop.biblia.model.ParsedPassage;
import com.scicrop.biblia.model.Passage;
import com.scicrop.biblia.model.PassageAnnotation;
import com.scicrop.biblia.model.Token;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PassageRepository {

    private static final String SQL_COUNT_BY_EDITION = """
            SELECT count(*) FROM passages WHERE edition_id = :editionId
            """;

    private static final String SQL_INSERT_PASSAGE = """
            INSERT INTO passages (edition_id, book_id, chapter, verse, sort_order, heading, content)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_INSERT_ANNOTATION = """
            INSERT INTO passage_annotations (passage_id, seq, kind, start_offset, end_offset, key_text, body)
            SELECT p.id, ?, ?, ?, ?, ?, ?
              FROM passages p
             WHERE p.edition_id = ? AND p.book_id = ? AND p.chapter = ? AND p.verse = ? AND p.subdivision = ''
            """;

    // Reading queries share one filter on passages p joined to books b, filled in by find().
    private static final String CHAPTER_FILTER = "p.edition_id = :editionId AND b.code = :bookCode AND p.chapter = :chapter";
    private static final String IDS_FILTER = "p.id IN (:passageIds)";

    private static final String SQL_PASSAGES = """
            SELECT p.id, p.chapter, p.verse, p.heading, p.content
              FROM passages p
              JOIN books b ON b.id = p.book_id
             WHERE %s
             ORDER BY p.sort_order
            """;

    private static final String SQL_ANNOTATIONS = """
            SELECT a.passage_id, a.kind, a.start_offset, a.end_offset, a.key_text, a.body
              FROM passage_annotations a
              JOIN passages p ON p.id = a.passage_id
              JOIN books b ON b.id = p.book_id
             WHERE %s
             ORDER BY a.passage_id, a.seq
            """;

    private static final String SQL_TOKENS = """
            SELECT t.passage_id, t.id, t.position, t.surface, t.trailing_text,
                   array_remove(array_agg(ta.original_token_id ORDER BY ta.original_token_id), NULL) AS aligned_to,
                   (SELECT string_agg(a.gloss, ' ' ORDER BY a.segment)
                      FROM token_analyses a
                     WHERE a.token_id = t.id AND a.gloss IS NOT NULL) AS gloss
              FROM tokens t
              JOIN passages p ON p.id = t.passage_id
              JOIN books b ON b.id = p.book_id
              LEFT JOIN token_alignments ta ON ta.translation_token_id = t.id
             WHERE %s
             GROUP BY t.id
             ORDER BY t.passage_id, t.position
            """;

    private static final String SQL_ID_BY_REFERENCE = """
            SELECT p.id
              FROM passages p
              JOIN books b ON b.id = p.book_id
             WHERE p.edition_id = :editionId AND b.code = :bookCode AND p.chapter = :chapter AND p.verse = :verse
               AND p.subdivision = ''
            """;

    private static final String SQL_BY_REFERENCES = """
            SELECT p.id, b.code, cb.name, p.chapter, p.verse, p.content
              FROM passages p
              JOIN books b ON b.id = p.book_id
              JOIN canon_books cb ON cb.book_id = b.id
              JOIN canons c ON c.id = cb.canon_id AND c.code = :canonCode
             WHERE p.edition_id = :editionId AND p.subdivision = '' AND (%s)
             ORDER BY p.sort_order
            """;

    private static final String SQL_DELETE_BY_EDITION = """
            DELETE FROM passages WHERE edition_id = :editionId
            """;

    private static final int BATCH_SIZE = 1000;

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public PassageRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countByEdition(int editionId) {
        return jdbcClient.sql(SQL_COUNT_BY_EDITION)
                .param("editionId", editionId)
                .query(Long.class)
                .single();
    }

    /** Removes every passage of an edition; tokens, analyses, annotations and alignments go with them. */
    public int deleteByEdition(int editionId) {
        return jdbcClient.sql(SQL_DELETE_BY_EDITION)
                .param("editionId", editionId)
                .update();
    }

    public Optional<Long> findId(int editionId, String bookCode, int chapter, int verse) {
        return jdbcClient.sql(SQL_ID_BY_REFERENCE)
                .params(Map.of("editionId", editionId, "bookCode", bookCode, "chapter", chapter, "verse", verse))
                .query(Long.class)
                .optional();
    }

    public record PassageRef(long id, String bookCode, String bookName, int chapter, int verse, String content) {
    }

    /** Passages of an edition by reference (book code, chapter, verse); refs not found are left out. */
    public List<PassageRef> findByReferences(int editionId, List<String> bookCodes, List<Integer> chapters,
                                             List<Integer> verses, String canonCode) {
        if (bookCodes.isEmpty()) {
            return List.of();
        }
        StringBuilder filter = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("editionId", editionId);
        params.put("canonCode", canonCode);
        for (int i = 0; i < bookCodes.size(); i++) {
            filter.append(i == 0 ? "" : " OR ")
                    .append("(b.code = :b").append(i).append(" AND p.chapter = :c").append(i)
                    .append(" AND p.verse = :v").append(i).append(')');
            params.put("b" + i, bookCodes.get(i));
            params.put("c" + i, chapters.get(i));
            params.put("v" + i, verses.get(i));
        }
        return jdbcClient.sql(String.format(SQL_BY_REFERENCES, filter))
                .params(params)
                .query((rs, rowNum) -> new PassageRef(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        rs.getInt("chapter"), rs.getInt("verse"), rs.getString("content")))
                .list();
    }

    public List<Passage> findChapter(int editionId, String bookCode, int chapter) {
        return find(CHAPTER_FILTER, Map.of("editionId", editionId, "bookCode", bookCode, "chapter", chapter));
    }

    /** Passages by id, in edition order, with annotations and tokens. */
    public List<Passage> findByIds(List<Long> passageIds) {
        if (passageIds.isEmpty()) {
            return List.of();
        }
        return find(IDS_FILTER, Map.of("passageIds", passageIds));
    }

    private List<Passage> find(String filter, Map<String, Object> params) {
        Map<Long, List<PassageAnnotation>> annotations = new HashMap<>();
        jdbcClient.sql(String.format(SQL_ANNOTATIONS, filter))
                .params(params)
                .query(rs -> {
                    annotations.computeIfAbsent(rs.getLong("passage_id"), id -> new ArrayList<>())
                            .add(new PassageAnnotation(
                                    rs.getString("kind"),
                                    rs.getInt("start_offset"),
                                    rs.getInt("end_offset"),
                                    rs.getString("key_text"),
                                    rs.getString("body")));
                });

        Map<Long, List<Token>> tokens = new HashMap<>();
        jdbcClient.sql(String.format(SQL_TOKENS, filter))
                .params(params)
                .query(rs -> {
                    tokens.computeIfAbsent(rs.getLong("passage_id"), id -> new ArrayList<>())
                            .add(new Token(
                                    rs.getLong("id"),
                                    rs.getInt("position"),
                                    rs.getString("surface"),
                                    rs.getString("trailing_text"),
                                    List.of((Long[]) rs.getArray("aligned_to").getArray()),
                                    rs.getString("gloss")));
                });

        return jdbcClient.sql(String.format(SQL_PASSAGES, filter))
                .params(params)
                .query((rs, rowNum) -> new Passage(
                        rs.getInt("chapter"),
                        rs.getInt("verse"),
                        rs.getString("heading"),
                        rs.getString("content"),
                        annotations.getOrDefault(rs.getLong("id"), List.of()),
                        tokens.getOrDefault(rs.getLong("id"), List.of())))
                .list();
    }

    /** Inserts passages in the given order; sort_order is the list position starting at 1. */
    public void insertAll(int editionId, List<ParsedPassage> passages, Map<String, Integer> bookIdsByUsfm) {
        List<Object[]> passageRows = new ArrayList<>(passages.size());
        List<Object[]> annotationRows = new ArrayList<>();
        int sortOrder = 0;
        for (ParsedPassage p : passages) {
            int bookId = bookIdsByUsfm.get(p.usfmCode());
            passageRows.add(new Object[] {
                    editionId, bookId, p.chapter(), p.verse(), ++sortOrder, p.heading(), p.content()});
            int seq = 0;
            for (PassageAnnotation a : p.annotations()) {
                annotationRows.add(new Object[] {
                        ++seq, a.kind(), a.startOffset(), a.endOffset(), a.keyText(), a.body(),
                        editionId, bookId, p.chapter(), p.verse()});
            }
        }
        batch(SQL_INSERT_PASSAGE, passageRows);
        batch(SQL_INSERT_ANNOTATION, annotationRows);
    }

    private void batch(String sql, List<Object[]> rows) {
        for (int from = 0; from < rows.size(); from += BATCH_SIZE) {
            jdbcTemplate.batchUpdate(sql, rows.subList(from, Math.min(from + BATCH_SIZE, rows.size())));
        }
    }
}
