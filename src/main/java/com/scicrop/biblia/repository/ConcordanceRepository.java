package com.scicrop.biblia.repository;

import com.scicrop.biblia.dto.ConcordanceQuery;
import com.scicrop.biblia.model.BookCount;
import com.scicrop.biblia.model.OccurrenceCounts;
import com.scicrop.biblia.model.ParallelVerse;
import com.scicrop.biblia.model.Token;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Concordance searches over tokens of one edition, by lexeme (lemma) or by normalized form,
 * optionally narrowed by book and morphological attributes.
 */
@Repository
public class ConcordanceRepository {

    /** Matching tokens. Conditions are appended by matchSql. */
    private static final String SQL_MATCH_FROM = """
              FROM tokens t
              JOIN token_analyses a ON a.token_id = t.id
              JOIN passages p ON p.id = t.passage_id
              JOIN books b ON b.id = p.book_id
            """;

    private static final String SQL_BOOK_NAMES = """
              JOIN canon_books cb ON cb.book_id = b.id
              JOIN canons c ON c.id = cb.canon_id AND c.code = :canonCode
            """;

    private static final String SQL_PASSAGE_TOKENS = """
            SELECT t.passage_id, t.id, t.position, t.surface, t.trailing_text
              FROM tokens t
             WHERE t.passage_id IN (:passageIds)
             ORDER BY t.passage_id, t.position
            """;

    /**
     * Verses of the parallel edition sharing a standard reference with each hit passage; for passages
     * without standard references, the verse with the same reference.
     */
    private static final String SQL_PARALLEL = """
            SELECT DISTINCT sa.passage_id AS id, pt.sort_order, pt.chapter, pt.verse, pt.content
              FROM passage_standard_refs sa
              JOIN passage_standard_refs sb ON sb.standard_book_id = sa.standard_book_id
                                           AND sb.standard_chapter = sa.standard_chapter
                                           AND sb.standard_verse = sa.standard_verse
              JOIN passages pt ON pt.id = sb.passage_id
              JOIN editions e ON e.id = pt.edition_id AND e.code = :parallelEdition
             WHERE sa.passage_id IN (:passageIds)
               AND (sa.standard_verse > 0 OR NOT EXISTS (
                        SELECT 1 FROM passage_standard_refs x WHERE x.passage_id = sa.passage_id AND x.standard_verse > 0))
            UNION
            SELECT g.id, pt.sort_order, pt.chapter, pt.verse, pt.content
              FROM passages g
              JOIN passages pt ON pt.book_id = g.book_id AND pt.chapter = g.chapter
                              AND pt.verse = g.verse AND pt.subdivision = g.subdivision
              JOIN editions e ON e.id = pt.edition_id AND e.code = :parallelEdition
             WHERE g.id IN (:passageIds)
               AND NOT EXISTS (SELECT 1 FROM passage_standard_refs x WHERE x.passage_id = g.id)
             ORDER BY 1, 2
            """;

    /** Columns of token_analyses for each morphological filter of ConcordanceQuery. */
    private static final Map<String, String> FILTER_COLUMNS = Map.of(
            "person", "person",
            "number", "number",
            "gender", "gender",
            "grammaticalCase", "grammatical_case",
            "tense", "tense",
            "voice", "voice",
            "mood", "mood",
            "stem", "stem",
            "verbForm", "verb_form",
            "state", "state");

    public record PassageRef(long passageId, String bookCode, String bookName, int chapter, int verse) {
    }

    private final JdbcClient jdbcClient;

    public ConcordanceRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public OccurrenceCounts count(int editionId, ConcordanceQuery query) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT " + occurrencesSql(query) + " AS occurrences, count(DISTINCT p.id) AS verses, "
                + "count(DISTINCT p.book_id) AS books " + matchSql(editionId, query, params, false);
        return jdbcClient.sql(sql)
                .params(params)
                .query((rs, rowNum) -> new OccurrenceCounts(
                        rs.getLong("occurrences"), rs.getLong("verses"), rs.getLong("books")))
                .single();
    }

    public List<BookCount> countByBook(int editionId, ConcordanceQuery query, String canonCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("canonCode", canonCode);
        String sql = "SELECT b.code, cb.name, " + occurrencesSql(query) + " AS occurrences, count(DISTINCT p.id) AS verses "
                + matchSql(editionId, query, params, true)
                + " GROUP BY b.code, cb.name, cb.position ORDER BY cb.position";
        return jdbcClient.sql(sql)
                .params(params)
                .query((rs, rowNum) -> new BookCount(
                        rs.getString("code"), rs.getString("name"), rs.getLong("occurrences"), rs.getLong("verses")))
                .list();
    }

    /** One page of distinct matching passages in edition order. */
    public List<PassageRef> findPassages(int editionId, ConcordanceQuery query, String canonCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("canonCode", canonCode);
        params.put("offset", query.effectiveOffset());
        params.put("limit", query.effectiveLimit());
        String sql = "SELECT DISTINCT p.id, p.sort_order, b.code, cb.name, p.chapter, p.verse "
                + matchSql(editionId, query, params, true)
                + " ORDER BY p.sort_order OFFSET :offset LIMIT :limit";
        return jdbcClient.sql(sql)
                .params(params)
                .query((rs, rowNum) -> new PassageRef(
                        rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        rs.getInt("chapter"), rs.getInt("verse")))
                .list();
    }

    /** Positions of the matching tokens, keyed by passage id. */
    public Map<Long, List<Integer>> findMatchedPositions(int editionId, ConcordanceQuery query, List<Long> passageIds) {
        Map<Long, List<Integer>> positions = new HashMap<>();
        if (passageIds.isEmpty()) {
            return positions;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("passageIds", passageIds);
        String sql = "SELECT DISTINCT t.passage_id, t.position " + matchSql(editionId, query, params, false)
                + " AND t.passage_id IN (:passageIds) ORDER BY t.passage_id, t.position";
        jdbcClient.sql(sql)
                .params(params)
                .query(rs -> {
                    positions.computeIfAbsent(rs.getLong("passage_id"), id -> new ArrayList<>())
                            .add(rs.getInt("position"));
                });
        return positions;
    }

    public Map<Long, List<Token>> findTokens(List<Long> passageIds) {
        Map<Long, List<Token>> tokens = new HashMap<>();
        if (passageIds.isEmpty()) {
            return tokens;
        }
        jdbcClient.sql(SQL_PASSAGE_TOKENS)
                .param("passageIds", passageIds)
                .query(rs -> {
                    tokens.computeIfAbsent(rs.getLong("passage_id"), id -> new ArrayList<>())
                            .add(new Token(rs.getLong("id"), rs.getInt("position"),
                                    rs.getString("surface"), rs.getString("trailing_text"), List.of(), null));
                });
        return tokens;
    }

    /** Corresponding verses of another edition, keyed by hit passage id, in that edition's order. */
    public Map<Long, List<ParallelVerse>> findParallel(List<Long> passageIds, String parallelEdition) {
        Map<Long, List<ParallelVerse>> parallel = new HashMap<>();
        if (passageIds.isEmpty()) {
            return parallel;
        }
        jdbcClient.sql(SQL_PARALLEL)
                .param("passageIds", passageIds)
                .param("parallelEdition", parallelEdition)
                .query(rs -> {
                    parallel.computeIfAbsent(rs.getLong("id"), id -> new ArrayList<>())
                            .add(new ParallelVerse(rs.getInt("chapter"), rs.getInt("verse"), rs.getString("content")));
                });
        return parallel;
    }

    /**
     * A lemma search counts analysed segments (a Hebrew word may hold a prefix and a stem of different
     * lemmas, or the same lemma twice); a form search counts written tokens.
     */
    private static String occurrencesSql(ConcordanceQuery query) {
        return query.lexeme() != null ? "count(DISTINCT (t.id, a.segment))" : "count(DISTINCT t.id)";
    }

    /** Distinct values of each morphological filter in an edition, keyed by query parameter name. */
    public Map<String, List<String>> findFilterValues(int editionId) {
        Map<String, List<String>> values = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> filter : FILTER_COLUMNS.entrySet()) {
            List<String> distinct = jdbcClient.sql("SELECT DISTINCT a." + filter.getValue() + " AS value"
                            + " FROM token_analyses a JOIN tokens t ON t.id = a.token_id"
                            + " WHERE t.edition_id = :editionId AND a." + filter.getValue() + " IS NOT NULL"
                            + " ORDER BY 1")
                    .param("editionId", editionId)
                    .query(String.class)
                    .list();
            if (!distinct.isEmpty()) {
                values.put(filter.getKey(), distinct);
            }
        }
        return values;
    }

    /**
     * FROM and WHERE of the matching tokens, optionally joined to the canon book names (needs :canonCode).
     * Filter columns come from FILTER_COLUMNS, never from user input. Form search ignores case.
     */
    private static String matchSql(int editionId, ConcordanceQuery query, Map<String, Object> params,
                                   boolean withBookNames) {
        StringBuilder sql = new StringBuilder(SQL_MATCH_FROM);
        if (withBookNames) {
            sql.append(SQL_BOOK_NAMES);
        }
        sql.append(" WHERE t.edition_id = :editionId");
        params.put("editionId", editionId);
        if (query.lexeme() != null) {
            sql.append(" AND a.lexeme_id = :lexemeId");
            params.put("lexemeId", query.lexeme());
        } else {
            sql.append(" AND lower(t.normalized) = lower(:form)");
            params.put("form", query.form());
        }
        if (query.book() != null && !query.book().isBlank()) {
            sql.append(" AND b.code = :bookCode");
            params.put("bookCode", query.book());
        }
        for (Map.Entry<String, String> filter : query.morphFilters().entrySet()) {
            String column = FILTER_COLUMNS.get(filter.getKey());
            sql.append(" AND a.").append(column).append(" = :").append(filter.getKey());
            params.put(filter.getKey(), filter.getValue());
        }
        return sql.toString();
    }
}
