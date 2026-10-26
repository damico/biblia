package com.scicrop.biblia.repository;

import com.scicrop.biblia.model.AxisChapter;
import com.scicrop.biblia.model.LinkPoint;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Queries behind the occurrence link visualization (axes, points and Strong keys). */
@Repository
public class LinkRepository {

    /** Strong columns of token_analyses: the word's own Greek Strong, or its Septuagint equivalent. */
    public enum KeyColumn {
        STRONG("a.strong"),
        LXX_STRONG("a.lxx_strong");

        private final String sql;

        KeyColumn(String sql) {
            this.sql = sql;
        }
    }

    public record Origin(int editionId, String bookCode, int chapter, int verse) {
    }

    private static final String SQL_CHAPTERS = """
            SELECT b.code, cb.name, p.chapter, max(p.verse) AS verses
              FROM passages p
              JOIN books b ON b.id = p.book_id
              JOIN canon_books cb ON cb.book_id = b.id
              JOIN canons c ON c.id = cb.canon_id AND c.code = :canonCode
             WHERE p.edition_id = :editionId AND b.code IN (:bookCodes)
             GROUP BY b.code, cb.name, cb.position, p.chapter
             ORDER BY cb.position, p.chapter
            """;

    private static final String SQL_ORIGIN = """
            SELECT t.edition_id, b.code, p.chapter, p.verse
              FROM tokens t
              JOIN passages p ON p.id = t.passage_id
              JOIN books b ON b.id = p.book_id
             WHERE t.id = :tokenId
            """;

    private static final String SQL_LEXEME_LANGUAGE = """
            SELECT language_code FROM lexemes WHERE id = :lexemeId
            """;

    private static final String POINTS_SELECT = """
            SELECT b.code, p.chapter, p.verse, count(*) AS occurrences, %s AS link_key
              FROM tokens t
              JOIN token_analyses a ON a.token_id = t.id
              JOIN passages p ON p.id = t.passage_id
              JOIN books b ON b.id = p.book_id
             WHERE t.edition_id = :editionId AND b.code IN (:bookCodes) AND %s
             GROUP BY b.code, p.chapter, p.verse, p.sort_order, link_key
             ORDER BY p.sort_order, link_key
            """;

    private final JdbcClient jdbcClient;

    public LinkRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<AxisChapter> findChapters(int editionId, List<String> bookCodes, String canonCode) {
        if (bookCodes.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql(SQL_CHAPTERS)
                .param("editionId", editionId)
                .param("bookCodes", bookCodes)
                .param("canonCode", canonCode)
                .query((rs, rowNum) -> new AxisChapter(
                        rs.getString("code"), rs.getString("name"), rs.getInt("chapter"), rs.getInt("verses")))
                .list();
    }

    public Optional<Origin> findOrigin(long tokenId) {
        return jdbcClient.sql(SQL_ORIGIN)
                .param("tokenId", tokenId)
                .query((rs, rowNum) -> new Origin(
                        rs.getInt("edition_id"), rs.getString("code"), rs.getInt("chapter"), rs.getInt("verse")))
                .optional();
    }

    public Optional<String> findLexemeLanguage(int lexemeId) {
        return jdbcClient.sql(SQL_LEXEME_LANGUAGE)
                .param("lexemeId", lexemeId)
                .query(String.class)
                .optional();
    }

    /**
     * Verses with occurrences of a lexeme, in edition order. With a key column, occurrences are split by
     * that Strong number and those without one are left out.
     */
    public List<LinkPoint> findLexemePoints(int editionId, int lexemeId, List<String> bookCodes, KeyColumn key) {
        String keySql = key == null ? "NULL" : key.sql;
        String condition = "a.lexeme_id = :lexemeId" + (key == null ? "" : " AND " + key.sql + " IS NOT NULL");
        return points(String.format(POINTS_SELECT, keySql, condition), editionId, bookCodes,
                Map.of("lexemeId", lexemeId));
    }

    /** Verses whose words have one of the given Greek Strong numbers in the key column, split by number. */
    public List<LinkPoint> findStrongPoints(int editionId, List<String> strongs, List<String> bookCodes,
                                            KeyColumn key) {
        if (strongs.isEmpty()) {
            return List.of();
        }
        return points(String.format(POINTS_SELECT, key.sql, key.sql + " IN (:strongs)"), editionId, bookCodes,
                Map.of("strongs", strongs));
    }

    private List<LinkPoint> points(String sql, int editionId, List<String> bookCodes, Map<String, Object> params) {
        if (bookCodes.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql(sql)
                .param("editionId", editionId)
                .param("bookCodes", bookCodes)
                .params(params)
                .query((rs, rowNum) -> new LinkPoint(
                        rs.getString("code"), rs.getInt("chapter"), rs.getInt("verse"),
                        rs.getLong("occurrences"), rs.getString("link_key")))
                .list();
    }
}
