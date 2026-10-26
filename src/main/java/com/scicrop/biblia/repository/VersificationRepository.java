package com.scicrop.biblia.repository;

import com.scicrop.biblia.model.VerseStat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class VersificationRepository {

    /** One standard reference of a passage, ready to insert. sourceRecordId is null for manual corrections. */
    public record Row(long passageId, int seq, String usfmCode, int chapter, int verse, String part,
                      String relation, String ruleAction, Integer sourceRecordId) {
    }

    /** A passage of another edition sharing a standard reference with the given passage. */
    public record Match(long passageId, int chapter, int verse, String sourceRelation, String sourcePart,
                        int sourceRefCount, String targetRelation, String targetPart, int targetRefCount) {
    }

    private static final String SQL_STATS = """
            SELECT p.id, b.usfm_code, p.chapter, p.verse,
                   coalesce(array_length(regexp_split_to_array(btrim(p.content), '\\s+'), 1), 0) AS words,
                   p.heading IS NOT NULL AS has_heading
              FROM passages p
              JOIN books b ON b.id = p.book_id
             WHERE p.edition_id = :editionId
             ORDER BY p.sort_order
            """;

    private static final String SQL_DELETE = """
            DELETE FROM passage_standard_refs
             WHERE passage_id IN (SELECT id FROM passages WHERE edition_id = :editionId)
            """;

    private static final String SQL_INSERT = """
            INSERT INTO passage_standard_refs (passage_id, seq, standard_book_id, standard_chapter, standard_verse,
                                               standard_part, relation, rule_action, source_record_id)
            SELECT ?, ?, b.id, ?, ?, ?, ?, ?, ?
              FROM books b
             WHERE b.usfm_code = ?
            """;

    private static final String SQL_HAS_REFS = """
            SELECT EXISTS (SELECT 1 FROM passage_standard_refs WHERE passage_id = :passageId)
            """;

    private static final String SQL_MATCHES = """
            SELECT DISTINCT pt.id, pt.sort_order, pt.chapter, pt.verse,
                   sa.relation AS source_relation, sa.standard_part AS source_part,
                   (SELECT count(*) FROM passage_standard_refs x
                     WHERE x.passage_id = sa.passage_id AND x.relation <> 'title') AS source_refs,
                   sb.relation AS target_relation, sb.standard_part AS target_part,
                   (SELECT count(*) FROM passage_standard_refs x
                     WHERE x.passage_id = sb.passage_id AND x.relation <> 'title') AS target_refs
              FROM passage_standard_refs sa
              JOIN passage_standard_refs sb ON sb.standard_book_id = sa.standard_book_id
                                           AND sb.standard_chapter = sa.standard_chapter
                                           AND sb.standard_verse = sa.standard_verse
              JOIN passages pt ON pt.id = sb.passage_id
             WHERE sa.passage_id = :passageId AND pt.edition_id = :targetEditionId
               -- a heading printed before verse 1 does not make the verse match the other edition's title
               AND (sa.standard_verse > 0 OR NOT EXISTS (
                        SELECT 1 FROM passage_standard_refs x WHERE x.passage_id = sa.passage_id AND x.standard_verse > 0))
             ORDER BY pt.sort_order
            """;

    private static final int BATCH_SIZE = 2000;

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public VersificationRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<VerseStat> findVerseStats(int editionId) {
        return jdbcClient.sql(SQL_STATS)
                .param("editionId", editionId)
                .query((rs, rowNum) -> new VerseStat(
                        rs.getLong("id"),
                        rs.getString("usfm_code"),
                        rs.getInt("chapter"),
                        rs.getInt("verse"),
                        rs.getInt("words"),
                        rs.getBoolean("has_heading")))
                .list();
    }

    /** Replaces the standard references of every passage of an edition. */
    public void replaceForEdition(int editionId, List<Row> rows) {
        jdbcClient.sql(SQL_DELETE).param("editionId", editionId).update();
        List<Object[]> values = new ArrayList<>(rows.size());
        for (Row r : rows) {
            values.add(new Object[] {r.passageId(), r.seq(), r.chapter(), r.verse(), r.part(), r.relation(),
                    r.ruleAction(), r.sourceRecordId(), r.usfmCode()});
        }
        for (int from = 0; from < values.size(); from += BATCH_SIZE) {
            jdbcTemplate.batchUpdate(SQL_INSERT, values.subList(from, Math.min(from + BATCH_SIZE, values.size())));
        }
    }

    public boolean hasStandardRefs(long passageId) {
        return jdbcClient.sql(SQL_HAS_REFS).param("passageId", passageId).query(Boolean.class).single();
    }

    public List<Match> findMatches(long passageId, int targetEditionId) {
        return jdbcClient.sql(SQL_MATCHES)
                .params(Map.of("passageId", passageId, "targetEditionId", targetEditionId))
                .query((rs, rowNum) -> new Match(
                        rs.getLong("id"),
                        rs.getInt("chapter"),
                        rs.getInt("verse"),
                        rs.getString("source_relation"),
                        rs.getString("source_part"),
                        rs.getInt("source_refs"),
                        rs.getString("target_relation"),
                        rs.getString("target_part"),
                        rs.getInt("target_refs")))
                .list();
    }
}
