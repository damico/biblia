package com.scicrop.biblia.repository;

import com.scicrop.biblia.model.CanonBook;
import com.scicrop.biblia.model.EditionBook;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;

@Repository
public class BookRepository {

    private static final String SQL_CANON_EXISTS = """
            SELECT EXISTS (SELECT 1 FROM canons WHERE code = :canonCode)
            """;

    private static final String SQL_BOOKS_BY_CANON = """
            SELECT b.code, b.testament, cb.position, cb.name, cb.abbreviation
              FROM canon_books cb
              JOIN canons c ON c.id = cb.canon_id
              JOIN books b ON b.id = cb.book_id
             WHERE c.code = :canonCode
             ORDER BY cb.position
            """;

    private static final String SQL_USFM_CODES_BY_CANON = """
            SELECT b.usfm_code, b.id
              FROM canon_books cb
              JOIN canons c ON c.id = cb.canon_id
              JOIN books b ON b.id = cb.book_id
             WHERE c.code = :canonCode
             ORDER BY cb.position
            """;

    private static final String SQL_BOOKS_BY_EDITION = """
            SELECT b.code, b.testament, cb.name, cb.abbreviation, ch.chapter_count
              FROM (SELECT book_id, max(chapter) AS chapter_count
                      FROM passages
                     WHERE edition_id = :editionId
                     GROUP BY book_id) AS ch
              JOIN books b ON b.id = ch.book_id
              JOIN canon_books cb ON cb.book_id = b.id
              JOIN canons c ON c.id = cb.canon_id
             WHERE c.code = :canonCode
             ORDER BY cb.position
            """;

    private final JdbcClient jdbcClient;

    public BookRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean canonExists(String canonCode) {
        return jdbcClient.sql(SQL_CANON_EXISTS)
                .param("canonCode", canonCode)
                .query(Boolean.class)
                .single();
    }

    public List<CanonBook> findByCanon(String canonCode) {
        return jdbcClient.sql(SQL_BOOKS_BY_CANON)
                .param("canonCode", canonCode)
                .query((rs, rowNum) -> new CanonBook(
                        rs.getString("code"),
                        rs.getString("testament"),
                        rs.getInt("position"),
                        rs.getString("name"),
                        rs.getString("abbreviation")))
                .list();
    }

    /** Books that have passages in the edition, named and ordered by the canon. */
    public List<EditionBook> findByEdition(int editionId, String canonCode) {
        return jdbcClient.sql(SQL_BOOKS_BY_EDITION)
                .param("editionId", editionId)
                .param("canonCode", canonCode)
                .query((rs, rowNum) -> new EditionBook(
                        rs.getString("code"),
                        rs.getString("testament"),
                        rs.getString("name"),
                        rs.getString("abbreviation"),
                        rs.getInt("chapter_count")))
                .list();
    }

    /** Book ids keyed by USFM code, in canon order. */
    public LinkedHashMap<String, Integer> findIdsByUsfmCode(String canonCode) {
        LinkedHashMap<String, Integer> ids = new LinkedHashMap<>();
        jdbcClient.sql(SQL_USFM_CODES_BY_CANON)
                .param("canonCode", canonCode)
                .query(rs -> {
                    ids.put(rs.getString("usfm_code"), rs.getInt("id"));
                });
        return ids;
    }
}
