package com.scicrop.biblia.repository;

import com.scicrop.biblia.model.GlossMatch;
import com.scicrop.biblia.model.LemmaAssociation;
import com.scicrop.biblia.model.SearchVerse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Word search queries. The searched word is matched as a whole word, with an optional plural ending
 * (s or es), ignoring case and, in translations, accents. The service only lets letters, spaces,
 * hyphens and apostrophes through, so the word can be placed inside the regular expression.
 */
@Repository
public class SearchRepository {

    public record Counted<T>(long total, List<T> items) {
    }

    private static final String WORD_REGEX = "('\\m' || %s || '(s|es)?\\M')";

    private static final String SQL_VERSES = """
            SELECT b.code, cb.name, b.testament, p.chapter, p.verse, p.content, count(*) OVER () AS total
              FROM passages p
              JOIN books b ON b.id = p.book_id
              JOIN canon_books cb ON cb.book_id = b.id
              JOIN canons c ON c.id = cb.canon_id AND c.code = :canonCode
             WHERE p.edition_id = :editionId
               AND unaccent(lower(p.content)) ~ %s
             ORDER BY p.sort_order
             LIMIT :limit
            """.formatted(WORD_REGEX.formatted("unaccent(lower(:word))"));

    private static final String SQL_ASSOCIATIONS = """
            WITH word_verses AS (
                SELECT p.id
                  FROM passages p
                 WHERE p.edition_id = :translationId
                   AND unaccent(lower(p.content)) ~ %s
            ), original_verses AS (
                SELECT DISTINCT pb.id
                  FROM word_verses w
                  JOIN passage_standard_refs sa ON sa.passage_id = w.id AND sa.standard_verse > 0
                  JOIN passage_standard_refs sb ON sb.standard_book_id = sa.standard_book_id
                                               AND sb.standard_chapter = sa.standard_chapter
                                               AND sb.standard_verse = sa.standard_verse
                  JOIN passages pb ON pb.id = sb.passage_id AND pb.edition_id = :originalId
            ), word_total AS (
                SELECT count(*) AS verses FROM original_verses
            ), together AS (
                SELECT a.lexeme_id, count(DISTINCT t.passage_id) AS verses
                  FROM original_verses o
                  JOIN tokens t ON t.passage_id = o.id
                  JOIN token_analyses a ON a.token_id = t.id
                 GROUP BY a.lexeme_id
                HAVING count(DISTINCT t.passage_id) >= 2
            )
            SELECT l.id, l.lemma, l.language_code, tg.verses AS together, wt.verses AS word_verses,
                   vc.verses AS lemma_verses, 2.0 * tg.verses / (wt.verses + vc.verses) AS dice
              FROM together tg
              JOIN lexeme_verse_counts vc ON vc.lexeme_id = tg.lexeme_id AND vc.edition_id = :originalId
              JOIN lexemes l ON l.id = tg.lexeme_id
             CROSS JOIN word_total wt
             ORDER BY dice DESC
             LIMIT :limit
            """.formatted(WORD_REGEX.formatted("unaccent(lower(:word))"));

    private static final String SQL_GLOSS_MATCHES = """
            SELECT g.lexeme_id, l.lemma, l.language_code,
                   array_agg(g.gloss ORDER BY g.occurrences DESC) AS glosses,
                   sum(g.occurrences) AS occurrences, vc.verses AS lemma_verses
              FROM lexeme_glosses g
              JOIN lexemes l ON l.id = g.lexeme_id
              JOIN lexeme_verse_counts vc ON vc.lexeme_id = g.lexeme_id AND vc.edition_id = g.edition_id
             WHERE g.edition_id = :editionId
               AND lower(g.gloss) ~ %s
             GROUP BY g.lexeme_id, l.lemma, l.language_code, vc.verses
             ORDER BY occurrences DESC
             LIMIT :limit
            """.formatted(WORD_REGEX.formatted("lower(:word)"));

    private static final String SQL_TOP_GLOSSES = """
            SELECT lexeme_id, gloss
              FROM (SELECT g.lexeme_id, g.gloss,
                           row_number() OVER (PARTITION BY g.lexeme_id ORDER BY g.occurrences DESC) AS rank
                      FROM lexeme_glosses g
                     WHERE g.edition_id = :editionId AND g.lexeme_id IN (:lexemeIds)) ranked
             WHERE rank <= :perLexeme
             ORDER BY lexeme_id, rank
            """;

    private final JdbcClient jdbcClient;

    public SearchRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Counted<SearchVerse> findVerses(int editionId, String word, String canonCode, int limit) {
        long[] total = {0};
        List<SearchVerse> verses = jdbcClient.sql(SQL_VERSES)
                .params(Map.of("editionId", editionId, "word", word, "canonCode", canonCode, "limit", limit))
                .query((rs, rowNum) -> {
                    total[0] = rs.getLong("total");
                    return new SearchVerse(rs.getString("code"), rs.getString("name"), rs.getString("testament"),
                            rs.getInt("chapter"), rs.getInt("verse"), rs.getString("content"));
                })
                .list();
        return new Counted<>(total[0], verses);
    }

    /** Lemmas of the original edition ranked by co-occurrence with the word in corresponding verses. */
    public List<LemmaAssociation> findAssociations(int translationId, int originalId, String word, int limit) {
        List<LemmaAssociation> rows = jdbcClient.sql(SQL_ASSOCIATIONS)
                .params(Map.of("translationId", translationId, "originalId", originalId, "word", word,
                        "limit", limit))
                .query((rs, rowNum) -> new LemmaAssociation(
                        rs.getInt("id"), rs.getString("lemma"), rs.getString("language_code"),
                        rs.getLong("together"), rs.getLong("word_verses"), rs.getLong("lemma_verses"),
                        rs.getDouble("dice"), List.of()))
                .list();
        Map<Integer, List<String>> glosses = findTopGlosses(originalId,
                rows.stream().map(LemmaAssociation::lexemeId).toList(), 6);
        return rows.stream()
                .map(a -> new LemmaAssociation(a.lexemeId(), a.lemma(), a.languageCode(), a.together(),
                        a.wordVerses(), a.lemmaVerses(), a.dice(), glosses.getOrDefault(a.lexemeId(), List.of())))
                .toList();
    }

    public List<GlossMatch> findGlossMatches(int editionId, String word, int limit) {
        return jdbcClient.sql(SQL_GLOSS_MATCHES)
                .params(Map.of("editionId", editionId, "word", word, "limit", limit))
                .query((rs, rowNum) -> new GlossMatch(
                        rs.getInt("lexeme_id"), rs.getString("lemma"), rs.getString("language_code"),
                        List.of((String[]) rs.getArray("glosses").getArray()),
                        rs.getLong("occurrences"), rs.getLong("lemma_verses")))
                .list();
    }

    /** The most common English glosses of each lexeme in the edition, most frequent first. */
    public Map<Integer, List<String>> findTopGlosses(int editionId, List<Integer> lexemeIds, int perLexeme) {
        Map<Integer, List<String>> glosses = new HashMap<>();
        if (lexemeIds.isEmpty()) {
            return glosses;
        }
        jdbcClient.sql(SQL_TOP_GLOSSES)
                .params(Map.of("editionId", editionId, "lexemeIds", lexemeIds, "perLexeme", perLexeme))
                .query(rs -> {
                    glosses.computeIfAbsent(rs.getInt("lexeme_id"), id -> new ArrayList<>()).add(rs.getString("gloss"));
                });
        return glosses;
    }
}
