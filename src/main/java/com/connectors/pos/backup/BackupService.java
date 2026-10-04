package com.connectors.pos.backup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Creates point-in-time snapshots of the production SQLite database.
 * <p>
 * Backups are only meaningful for the SQLite (desktop/production) deployment;
 * the Postgres-backed dev/demo environments are excluded automatically by
 * checking the active JDBC URL.
 * <p>
 * {@code VACUUM INTO} is used instead of a raw file copy because the live
 * database runs in WAL mode: a plain file copy could capture the main file
 * mid-write while the WAL file holds uncommitted pages, producing a corrupt
 * snapshot. {@code VACUUM INTO} asks SQLite itself to write a consistent,
 * compacted copy of the current database state to a new file.
 */
@Slf4j
@Service
public class BackupService {

    private final DataSource dataSource;
    private final String datasourceUrl;
    private final String backupDirName;
    private final int retentionCount;

    public BackupService(DataSource dataSource,
                          @Value("${spring.datasource.url}") String datasourceUrl,
                          @Value("${app.backup.dir:${app.data.dir:.}/backups}") String backupDirName,
                          @Value("${app.backup.retention:7}") int retentionCount) {
        this.dataSource = dataSource;
        this.datasourceUrl = datasourceUrl;
        this.backupDirName = backupDirName;
        this.retentionCount = retentionCount;
    }

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public boolean isBackupSupported() {
        return datasourceUrl != null && datasourceUrl.startsWith("jdbc:sqlite");
    }

    /**
     * Creates a new backup file and prunes old ones down to the retention count.
     * Used by both the manual "download backup" action and the scheduled job.
     */
    public Path createBackup() {
        if (!isBackupSupported()) {
            throw new UnsupportedOperationException("Backups are only supported for the SQLite production database.");
        }

        try {
            Path dir = resolveBackupDir();
            String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
            Path target = dir.resolve("backup_" + timestamp + ".sqlite");
            String escapedPath = target.toAbsolutePath().toString().replace("'", "''");

            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute("VACUUM INTO '" + escapedPath + "'");
            }

            applyRetentionPolicy(dir);
            return target;
        } catch (Exception e) {
            log.error("Failed to create database backup", e);
            throw new RuntimeException("Failed to create database backup: " + e.getMessage(), e);
        }
    }

    private Path resolveBackupDir() throws IOException {
        Path dir = Path.of(backupDirName);
        Files.createDirectories(dir);
        return dir;
    }

    private void applyRetentionPolicy(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> backups = files
                    .filter(p -> p.getFileName().toString().startsWith("backup_") && p.getFileName().toString().endsWith(".sqlite"))
                    .sorted(Comparator.comparing(this::lastModifiedMillis).reversed())
                    .toList();

            for (int i = retentionCount; i < backups.size(); i++) {
                Path old = backups.get(i);
                if (Files.deleteIfExists(old)) {
                    log.info("Deleted old backup beyond retention limit: {}", old);
                }
            }
        } catch (IOException e) {
            log.error("Failed to apply backup retention policy", e);
        }
    }

    private long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    // Cron: Seconds Minutes Hours Day-of-month Month Day-of-week
    // "0 0 3 * * ?" = every day at 03:00 AM
    @Scheduled(cron = "${app.backup.cron:0 0 3 * * ?}", zone = "Africa/Cairo")
    public void scheduledBackup() {
        if (!isBackupSupported()) {
            return;
        }
        try {
            Path backup = createBackup();
            log.info("Scheduled database backup created: {}", backup);
        } catch (Exception e) {
            log.error("Scheduled database backup failed", e);
        }
    }
}
