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
