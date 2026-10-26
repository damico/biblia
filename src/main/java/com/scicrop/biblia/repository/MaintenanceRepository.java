package com.scicrop.biblia.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MaintenanceRepository {

    /** Planner statistics are stale right after a bulk import; without them some queries take minutes. */
    private static final String SQL_ANALYZE = """
            ANALYZE passages, passage_annotations, tokens, lexemes, token_analyses, token_alignments
            """;

    private static final String SQL_REFRESH_GLOSSES = "REFRESH MATERIALIZED VIEW lexeme_glosses";
    private static final String SQL_REFRESH_VERSE_COUNTS = "REFRESH MATERIALIZED VIEW lexeme_verse_counts";

    private final JdbcClient jdbcClient;

    public MaintenanceRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Refreshes the word search views and the planner statistics after an import. */
    public void analyzeTextTables() {
        jdbcClient.sql(SQL_REFRESH_GLOSSES).update();
        jdbcClient.sql(SQL_REFRESH_VERSE_COUNTS).update();
        jdbcClient.sql(SQL_ANALYZE).update();
    }
}
