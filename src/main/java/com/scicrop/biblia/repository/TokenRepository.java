package com.scicrop.biblia.repository;

import com.scicrop.biblia.model.EditionRef;
import com.scicrop.biblia.model.ParsedAnalysis;
import com.scicrop.biblia.model.ParsedToken;
import com.scicrop.biblia.model.ParsedVerse;
import com.scicrop.biblia.model.SegmentDetail;
import com.scicrop.biblia.model.TokenDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class TokenRepository {

    private static final String SQL_INSERT_LEXEME = """
            INSERT INTO lexemes (language_code, source_record_id, lemma)
            VALUES (?, ?, ?)
            ON CONFLICT (language_code, source_record_id, lemma) DO NOTHING
            """;

    private static final String SQL_INSERT_TOKEN = """
            INSERT INTO tokens (edition_id, passage_id, position, source_ref, surface, trailing_text, normalized)
            SELECT p.edition_id, p.id, ?, ?, ?, ?, ?
              FROM passages p
             WHERE p.edition_id = ? AND p.book_id = ? AND p.chapter = ? AND p.verse = ? AND p.subdivision = ''
            """;

    private static final String SQL_INSERT_ANALYSIS = """
            INSERT INTO token_analyses (token_id, segment, source_record_id, lexeme_id, segment_text, language_code,
                                        part_of_speech, morph_code, person, number, gender, grammatical_case,
                                        tense, voice, mood, degree, stem, verb_form, state, strong, gloss,
                                        transliteration, lxx_greek, lxx_strong)
            SELECT t.id, ?, l.source_record_id, l.id, ?, l.language_code,
                   ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
              FROM tokens t, lexemes l
             WHERE t.edition_id = ? AND t.source_ref = ?
               AND l.language_code = ? AND l.source_record_id = ? AND l.lemma = ?
            """;

    private static final String SQL_INSERT_ALIGNMENT = """
            INSERT INTO token_alignments (source_record_id, original_token_id, translation_token_id)
            SELECT ?, o.id, t.id
              FROM tokens o, tokens t
             WHERE o.edition_id = ? AND o.source_ref = ?
               AND t.edition_id = ? AND t.source_ref = ?
            """;

    private static final String SQL_TOKEN = """
            SELECT t.id, e.code AS edition_code, e.name AS edition_name, e.language_code,
                   b.code AS book_code, cb.name AS book_name, p.chapter, p.verse, t.surface, t.normalized
              FROM tokens t
              JOIN editions e ON e.id = t.edition_id
              JOIN passages p ON p.id = t.passage_id
              JOIN books b ON b.id = p.book_id
              JOIN canon_books cb ON cb.book_id = b.id
              JOIN canons c ON c.id = cb.canon_id AND c.code = :canonCode
             WHERE t.id = :tokenId
            """;

    private static final String SQL_SEGMENTS = """
            SELECT a.segment, a.segment_text, a.language_code, l.id AS lexeme_id, l.lemma,
                   a.part_of_speech, a.morph_code, a.person, a.number, a.gender, a.grammatical_case,
                   a.tense, a.voice, a.mood, a.degree, a.stem, a.verb_form, a.state, a.strong, a.gloss,
                   a.transliteration
              FROM token_analyses a
              JOIN lexemes l ON l.id = a.lexeme_id
             WHERE a.token_id = :tokenId
             ORDER BY a.segment
            """;

    private static final String SQL_LEMMA = """
            SELECT lemma FROM lexemes WHERE id = :lexemeId
            """;

    private static final int BATCH_SIZE = 2000;

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public TokenRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Token with its segments; lemma and form counts are left null for the service to fill. */
    public Optional<TokenDetail> findDetail(long tokenId, String canonCode) {
        List<SegmentDetail> segments = jdbcClient.sql(SQL_SEGMENTS)
                .param("tokenId", tokenId)
                .query((rs, rowNum) -> new SegmentDetail(
                        rs.getInt("segment"),
                        rs.getString("segment_text"),
                        rs.getString("language_code"),
                        rs.getInt("lexeme_id"),
                        rs.getString("lemma"),
                        rs.getString("part_of_speech"),
                        rs.getString("morph_code"),
                        rs.getString("person"),
                        rs.getString("number"),
                        rs.getString("gender"),
                        rs.getString("grammatical_case"),
                        rs.getString("tense"),
                        rs.getString("voice"),
                        rs.getString("mood"),
                        rs.getString("degree"),
                        rs.getString("stem"),
                        rs.getString("verb_form"),
                        rs.getString("state"),
                        rs.getString("strong"),
                        rs.getString("gloss"),
                        rs.getString("transliteration"),
                        null))
                .list();
        return jdbcClient.sql(SQL_TOKEN)
                .param("tokenId", tokenId)
                .param("canonCode", canonCode)
                .query((rs, rowNum) -> new TokenDetail(
                        rs.getLong("id"),
                        rs.getString("edition_code"),
                        rs.getString("edition_name"),
                        rs.getString("language_code"),
                        rs.getString("book_code"),
                        rs.getString("book_name"),
                        rs.getInt("chapter"),
                        rs.getInt("verse"),
                        rs.getString("surface"),
                        rs.getString("normalized"),
                        segments,
                        null))
                .optional();
    }

    public Optional<String> findLemma(int lexemeId) {
        return jdbcClient.sql(SQL_LEMMA)
                .param("lexemeId", lexemeId)
                .query(String.class)
                .optional();
    }

    /** Inserts the distinct (language, lemma) pairs of the analyses as lexemes of the edition source. */
    public void insertLexemes(EditionRef edition, List<ParsedVerse> verses) {
        Set<List<String>> lexemes = new LinkedHashSet<>();
        for (ParsedVerse verse : verses) {
            for (ParsedToken token : verse.tokens()) {
                for (ParsedAnalysis a : token.analyses()) {
                    lexemes.add(List.of(a.languageCode(), a.lemma()));
                }
            }
        }
        List<Object[]> rows = new ArrayList<>(lexemes.size());
        for (List<String> lexeme : lexemes) {
            rows.add(new Object[] {lexeme.get(0), edition.sourceRecordId(), lexeme.get(1)});
        }
        batch(SQL_INSERT_LEXEME, rows);
    }

    /** Inserts tokens and their analyses (translations have none). Passages and lexemes must already exist. */
    public void insertTokens(EditionRef edition, List<ParsedVerse> verses, Map<String, Integer> bookIdsByUsfm) {
        List<Object[]> tokenRows = new ArrayList<>();
        List<Object[]> analysisRows = new ArrayList<>();
        for (ParsedVerse verse : verses) {
            int bookId = bookIdsByUsfm.get(verse.passage().usfmCode());
            for (ParsedToken t : verse.tokens()) {
                tokenRows.add(new Object[] {
                        t.position(), t.sourceRef(), t.surface(), t.trailing(), t.normalized(),
                        edition.id(), bookId, verse.passage().chapter(), verse.passage().verse()});
                for (ParsedAnalysis a : t.analyses()) {
                    analysisRows.add(new Object[] {
                            a.segment(), a.segmentText(),
                            a.partOfSpeech(), a.morphCode(), a.person(), a.number(), a.gender(), a.grammaticalCase(),
                            a.tense(), a.voice(), a.mood(), a.degree(), a.stem(), a.verbForm(), a.state(),
                            a.strong(), a.gloss(), a.transliteration(), a.lxxGreek(), a.lxxStrong(),
                            edition.id(), t.sourceRef(),
                            a.languageCode(), edition.sourceRecordId(), a.lemma()});
                }
            }
        }
        batch(SQL_INSERT_TOKEN, tokenRows);
        batch(SQL_INSERT_ANALYSIS, analysisRows);
    }

    /** Links tokens of two editions by their source references; returns the number of pairs stored. */
    public int insertAlignments(int sourceRecordId, EditionRef original, EditionRef translation,
                                List<String[]> pairs) {
        List<Object[]> rows = new ArrayList<>(pairs.size());
        for (String[] pair : pairs) {
            rows.add(new Object[] {sourceRecordId, original.id(), pair[0], translation.id(), pair[1]});
        }
        batch(SQL_INSERT_ALIGNMENT, rows);
        return rows.size();
    }

    private void batch(String sql, List<Object[]> rows) {
        for (int from = 0; from < rows.size(); from += BATCH_SIZE) {
            jdbcTemplate.batchUpdate(sql, rows.subList(from, Math.min(from + BATCH_SIZE, rows.size())));
        }
    }
}
