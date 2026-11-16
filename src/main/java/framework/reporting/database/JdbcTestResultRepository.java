/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: JDBC-backed implementation of TestResultRepository for persisting test results to a relational database.
 * Created: 2026-10-01
 */
package framework.reporting.database;

import framework.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JDBC implementation of {@link TestResultRepository}.
 * Uses {@link DatabaseManager} for connection pooling and executes standard SQL.
 */
public class JdbcTestResultRepository implements TestResultRepository {

    private static final Logger logger = LoggerFactory.getLogger(JdbcTestResultRepository.class);
    private static final AtomicBoolean SCHEMA_INITIALIZED = new AtomicBoolean(false);

    @Override
    public void initSchema() {
        if (SCHEMA_INITIALIZED.compareAndSet(false, true)) {
            String createTableSql = "CREATE TABLE IF NOT EXISTS test_results (" +
                    "execution_id VARCHAR(100), " +
                    "test_id VARCHAR(100), " +
                    "test_name VARCHAR(255), " +
                    "module VARCHAR(100), " +
                    "feature VARCHAR(100), " +
                    "status VARCHAR(50), " +
                    "start_time TIMESTAMP, " +
                    "end_time TIMESTAMP, " +
                    "duration BIGINT, " +
                    "environment VARCHAR(50), " +
                    "browser VARCHAR(50), " +
                    "retry_count INT, " +
                    "error_message VARCHAR(2000), " +
                    "stack_trace CLOB, " +
                    "build_number VARCHAR(100), " +
                    "branch VARCHAR(100), " +
                    "commit_hash VARCHAR(100), " +
                    "created_at TIMESTAMP" +
                    ")";

            try (Connection conn = DatabaseManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSql);
                logger.info("Successfully initialized 'test_results' database table.");
            } catch (Exception e) {
                logger.warn("Could not auto-initialize 'test_results' table: {}", e.getMessage());
            }
        }
    }

    @Override
    public void save(TestResult result) {
        if (result == null) {
            return;
        }

        initSchema();

        String insertSql = "INSERT INTO test_results (" +
                "execution_id, test_id, test_name, module, feature, status, " +
                "start_time, end_time, duration, environment, browser, retry_count, " +
                "error_message, stack_trace, build_number, branch, commit_hash, created_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            ps.setString(1, result.getExecutionId());
            ps.setString(2, result.getTestId());
            ps.setString(3, result.getTestName());
            ps.setString(4, result.getModule());
            ps.setString(5, result.getFeature());
            ps.setString(6, result.getStatus());
            ps.setTimestamp(7, result.getStartTime() != null ? Timestamp.from(result.getStartTime()) : null);
            ps.setTimestamp(8, result.getEndTime() != null ? Timestamp.from(result.getEndTime()) : null);
            ps.setLong(9, result.getDuration());
            ps.setString(10, result.getEnvironment());
            ps.setString(11, result.getBrowser());
            ps.setInt(12, result.getRetryCount());
            ps.setString(13, truncate(result.getErrorMessage(), 1900));
            ps.setString(14, result.getStackTrace() != null ? result.getStackTrace() : "");
            ps.setString(15, result.getBuildNumber());
            ps.setString(16, result.getBranch());
            ps.setString(17, result.getCommit());
            ps.setTimestamp(18, result.getCreatedAt() != null ? Timestamp.from(result.getCreatedAt()) : Timestamp.from(java.time.Instant.now()));

            ps.executeUpdate();
            logger.info("Saved test result for {} to database successfully.", result.getTestId());
        } catch (Exception e) {
            // DB reporting failures must never fail the test
            logger.warn("Failed to persist test result for {} to database: {}",
                    result.getTestId(), e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
