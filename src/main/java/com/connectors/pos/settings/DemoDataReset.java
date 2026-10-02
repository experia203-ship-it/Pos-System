package com.connectors.pos.settings;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
@RequiredArgsConstructor
@Service
@Profile("demo-reset")
public class DemoDataReset {
    private static final Logger log = LoggerFactory.getLogger(DemoDataReset.class);

    private final DataSource dataSource;

    @Value("${app.demo.auto-reset:false}")
    private boolean isAutoResetEnabled;

    @Value("${RENDER:false}")
    private boolean isRenderEnvironment;

    /**
     * Seeds the demo data right after the app finishes starting, but only if the
     * database looks empty (e.g. a brand-new deploy/disk). Without this, a fresh
     * deployment would boot with no products/customers/orders and sit empty until
     * the next scheduled 4 AM reset below.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartupIfEmpty() {
        if (!isAutoResetEnabled || !isRenderEnvironment) {
            log.info("Demo DB startup seed skipped: the Render demo reset is not enabled here.");
            return;
        }

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            // NOTE: don't check the "settings" table here — spring.sql.init.mode=always
            // runs src/main/resources/data.sql on every boot, which always INSERT OR
            // IGNOREs a default settings row before this listener fires. That made
            // this count always >= 1, so the startup seed never actually ran. The
            // "products" table is only ever populated by the demo reset script
            // below, so it is a reliable empty/non-empty signal for demo data.
            Integer productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
            if (productCount != null && productCount > 0) {
                log.info("Demo database already contains data; skipping startup seed.");
                return;
            }
        } catch (Exception e) {
            log.warn("Could not inspect demo database state before deciding whether to seed on startup", e);
            return;
        }

        log.info("Demo database is empty on startup; seeding immediately instead of waiting for the next scheduled reset.");
        runReset();
    }

    // Cron expression: Seconds Minutes Hours Day-of-month Month Day-of-week
    // "0 0 4 * * ?" = Every day at 04:00:00 AM
    @Scheduled(cron = "0 0 4 * * ?",zone = "Africa/Cairo")
    public void executeDatabaseReset() {
        if (!isAutoResetEnabled || !isRenderEnvironment) {
            log.info("Demo DB Auto-Reset skipped: the Render demo reset is not enabled here.");
            return;
        }

        log.info("Starting scheduled demo database reset...");
        runReset();
    }

    private void runReset() {
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
