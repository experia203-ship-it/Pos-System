package com.connectors.pos.settings;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
@RequiredArgsConstructor
@Service
public class DemoDataReset {
    private static final Logger log = LoggerFactory.getLogger(DemoDataReset.class);

    private final DataSource dataSource;

    @Value("${app.demo.auto-reset:false}")
    private boolean isAutoResetEnabled;

    // Cron expression: Seconds Minutes Hours Day-of-month Month Day-of-week
    // "0 0 4 * * ?" = Every day at 04:00:00 AM
    @Scheduled(cron = "0 0 4 * * ?",zone = "Africa/Cairo")
    public void executeDatabaseReset() {
        if (!isAutoResetEnabled) {
            log.info("Demo DB Auto-Reset skipped: Feature disabled on this environment.");
            return;
        }

        log.info("Starting scheduled demo database reset...");

        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/demo/sqlite_demo_reset.sql"));
            populator.setContinueOnError(false);

            populator.execute(dataSource);

            log.info("Demo database successfully reset to seed state.");
        } catch (Exception e) {
            log.error("Failed to execute demo database reset", e);
        }
    }


}
