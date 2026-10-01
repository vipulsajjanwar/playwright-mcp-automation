package framework.reporting.database;

/**
 * Repository interface for persisting test execution results.
 */
public interface TestResultRepository {

    /**
     * Initializes the underlying database table or collection if not present.
     */
    void initSchema();

    /**
     * Persists a single test execution record.
     */
    void save(TestResult result);
}
