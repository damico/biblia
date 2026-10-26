package com.scicrop.biblia.repository;

import com.scicrop.biblia.model.Edition;
import com.scicrop.biblia.model.EditionRef;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EditionRepository {

    private static final String SQL_ID_BY_CODE = """
            SELECT id FROM editions WHERE code = :code
            """;

    private static final String SQL_SOURCE_RECORD = """
            SELECT id FROM source_records WHERE url = :url AND version_ref = :versionRef
            """;

    private static final String SQL_REF_BY_CODE = """
            SELECT id, language_code, source_record_id FROM editions WHERE code = :code
            """;

    private static final String SQL_SELECT_EDITION = """
            SELECT e.code, e.name, e.version, l.code AS language_code, l.name AS language_name,
                   l.script_direction, s.name AS source_name, s.url AS source_url,
                   s.version_ref AS source_version_ref, s.license, s.attribution
              FROM editions e
              JOIN languages l ON l.code = e.language_code
              JOIN source_records s ON s.id = e.source_record_id
            """;

    private static final String SQL_ALL = SQL_SELECT_EDITION + " ORDER BY e.name";

    private static final String SQL_BY_CODE = SQL_SELECT_EDITION + " WHERE e.code = :code";

    private static final RowMapper<Edition> EDITION_MAPPER = (rs, rowNum) -> new Edition(
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("version"),
            rs.getString("language_code"),
            rs.getString("language_name"),
            rs.getString("script_direction"),
            rs.getString("source_name"),
            rs.getString("source_url"),
            rs.getString("source_version_ref"),
            rs.getString("license"),
            rs.getString("attribution"));

    private final JdbcClient jdbcClient;

    public EditionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<Integer> findIdByCode(String code) {
        return jdbcClient.sql(SQL_ID_BY_CODE)
                .param("code", code)
                .query(Integer.class)
                .optional();
    }

    public Optional<Integer> findSourceRecordId(String url, String versionRef) {
        return jdbcClient.sql(SQL_SOURCE_RECORD)
                .param("url", url)
                .param("versionRef", versionRef)
                .query(Integer.class)
                .optional();
    }

    public Optional<EditionRef> findRefByCode(String code) {
        return jdbcClient.sql(SQL_REF_BY_CODE)
                .param("code", code)
                .query((rs, rowNum) -> new EditionRef(
                        rs.getInt("id"),
                        rs.getString("language_code"),
                        rs.getInt("source_record_id")))
                .optional();
    }

    public List<Edition> findAll() {
        return jdbcClient.sql(SQL_ALL)
                .query(EDITION_MAPPER)
                .list();
    }

    public Optional<Edition> findByCode(String code) {
        return jdbcClient.sql(SQL_BY_CODE)
                .param("code", code)
                .query(EDITION_MAPPER)
                .optional();
    }
}
