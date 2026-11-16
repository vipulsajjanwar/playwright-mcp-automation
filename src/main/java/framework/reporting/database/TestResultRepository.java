/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Repository interface defining CRUD operations for persisting and querying test results in the database.
 * Created: 2026-10-01
 */
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
