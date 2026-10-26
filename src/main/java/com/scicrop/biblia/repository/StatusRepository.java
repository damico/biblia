package com.scicrop.biblia.repository;

import com.scicrop.biblia.model.DatabaseStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public class StatusRepository {

    private static final String SQL_STATUS = """
            SELECT current_setting('server_version') AS server_version,
                   m.version AS last_migration,
                   m.applied_at AS last_migration_applied_at
              FROM (SELECT 1) AS one
              LEFT JOIN LATERAL (
                    SELECT version, applied_at
                      FROM schema_migration
                     ORDER BY version DESC
                     LIMIT 1
                   ) AS m ON TRUE
            """;

    private final JdbcClient jdbcClient;

    public StatusRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public DatabaseStatus findStatus() {
        return jdbcClient.sql(SQL_STATUS)
                .query((rs, rowNum) -> new DatabaseStatus(
                        rs.getString("server_version"),
                        rs.getString("last_migration"),
                        rs.getObject("last_migration_applied_at", OffsetDateTime.class)))
                .single();
    }
}
