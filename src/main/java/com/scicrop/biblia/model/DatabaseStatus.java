package com.scicrop.biblia.model;

import java.time.OffsetDateTime;

public record DatabaseStatus(
        String serverVersion,
        String lastMigration,
        OffsetDateTime lastMigrationAppliedAt) {
}
