package framework.reporting.reporters;

import framework.config.ReportingConfig;
import framework.context.TestContext;
import framework.reporting.TestReporter;
import framework.reporting.database.JdbcTestResultRepository;
import framework.reporting.database.TestResult;
import framework.reporting.database.TestResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Database implementation of {@link TestReporter}.
 * Persists test execution records into a relational database via {@link TestResultRepository}.
 */
public class DatabaseTestReporter implements TestReporter {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTestReporter.class);
    private final TestResultRepository repository;

    private static final ExecutorService DB_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "database-reporter-worker");
        t.setDaemon(true);
        return t;
    });

    public DatabaseTestReporter() {
        this(new JdbcTestResultRepository());
    }

    public DatabaseTestReporter(TestResultRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getReporterName() {
        return "Database";
    }

    @Override
    public boolean isEnabled() {
        return ReportingConfig.isDatabaseEnabled();
    }

    @Override
    public void onSuiteStart(ISuite suite) {
        if (!isEnabled()) return;
        try {
            repository.initSchema();
        } catch (Exception e) {
            logger.warn("Failed to init database schema onSuiteStart: {}", e.getMessage());
        }
    }

    @Override
    public void onTestStart(TestContext context) {
        // No-op
    }

    @Override
    public void onStep(TestContext context, String message) {
        // No-op
    }

    @Override
    public void onTestPass(TestContext context) {
        saveRecord(context);
    }

    @Override
    public void onTestFail(TestContext context, Throwable throwable) {
        saveRecord(context);
    }

    @Override
    public void onTestSkip(TestContext context, String reason) {
        saveRecord(context);
    }

    @Override
    public void onTestFinish(TestContext context) {
        // Handled in pass/fail/skip
    }

    @Override
    public void onSuiteFinish(ISuite suite) {
        if (!isEnabled()) return;
        try {
            DB_EXECUTOR.shutdown();
            if (!DB_EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                DB_EXECUTOR.shutdownNow();
            }
        } catch (Exception ignored) {
            DB_EXECUTOR.shutdownNow();
        }
    }

    private void saveRecord(TestContext context) {
        if (!isEnabled() || context == null) return;
        try {
            TestResult entity = TestResult.fromContext(context);
            if (ReportingConfig.isAsyncDatabaseEnabled()) {
                DB_EXECUTOR.submit(() -> repository.save(entity));
            } else {
                repository.save(entity);
            }
        } catch (Exception e) {
            logger.warn("Could not submit test result to database: {}", e.getMessage());
        }
    }
}
