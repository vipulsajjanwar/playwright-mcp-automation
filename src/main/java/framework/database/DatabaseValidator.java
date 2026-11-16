/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Provides assertion-style methods for verifying database state against expected values after test actions.
 * Created: 2026-08-15
 */
package framework.database;

import framework.exceptions.DatabaseException;
import org.testng.Assert;

import java.util.List;
import java.util.Map;

public class DatabaseValidator {

    private DatabaseValidator() {}

    public static String getValue(String sql, Object... params) {
        List<Map<String, Object>> results = QueryExecutor.executeQuery(sql, params);
        if (results.isEmpty()) {
            throw new DatabaseException("No results returned for query: " + sql);
        }
        Map<String, Object> firstRow = results.get(0);
        Object firstVal = firstRow.values().iterator().next();
        return firstVal != null ? firstVal.toString() : null;
    }

    public static void validateRowCount(String sql, int expectedCount, Object... params) {
        List<Map<String, Object>> results = QueryExecutor.executeQuery(sql, params);
        Assert.assertEquals(results.size(), expectedCount, "Database row count mismatch for query: " + sql);
    }
}
