package com.scicrop.biblia.repository;

import com.scicrop.biblia.model.SourceRecord;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SourceRecordRepository {

    private static final String SQL_ALL = """
            SELECT name, maintainer, url, version_ref, license, attribution
              FROM source_records
             ORDER BY id
            """;

    private final JdbcClient jdbcClient;

    public SourceRecordRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<SourceRecord> findAll() {
        return jdbcClient.sql(SQL_ALL)
                .query((rs, rowNum) -> new SourceRecord(
                        rs.getString("name"),
                        rs.getString("maintainer"),
                        rs.getString("url"),
                        rs.getString("version_ref"),
                        rs.getString("license"),
                        rs.getString("attribution")))
                .list();
    }
}
