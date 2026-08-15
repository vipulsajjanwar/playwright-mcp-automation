package tests.database;

import framework.database.DatabaseValidator;
import framework.database.QueryExecutor;
import framework.reporting.TestExecutionLogger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UserDatabaseValidationTest {

    @BeforeClass(alwaysRun = true)
    public void setupTestDatabase() {
        TestExecutionLogger.logStep("Creating in-memory USERS table for JDBC validation");
        QueryExecutor.executeUpdate("CREATE TABLE IF NOT EXISTS USERS (ID INT PRIMARY KEY, USERNAME VARCHAR(255), STATUS VARCHAR(50))");
        QueryExecutor.executeUpdate("DELETE FROM USERS");
        QueryExecutor.executeUpdate("INSERT INTO USERS (ID, USERNAME, STATUS) VALUES (1, 'admin', 'ACTIVE')");
        QueryExecutor.executeUpdate("INSERT INTO USERS (ID, USERNAME, STATUS) VALUES (2, 'john_doe', 'INACTIVE')");
    }

    @Test(groups = {"database", "smoke"}, description = "Validate user status in DB via JDBC")
    public void testUserStatusInDatabase() {
        TestExecutionLogger.logStep("Executing SQL validation query");
        String actualStatus = DatabaseValidator.getValue("SELECT STATUS FROM USERS WHERE USERNAME=?", "admin");
        Assert.assertEquals(actualStatus, "ACTIVE", "User status in database should be ACTIVE");
    }

    @Test(groups = {"database", "regression"}, description = "Validate active user row count")
    public void testActiveUserRowCount() {
        TestExecutionLogger.logStep("Validating database row count for ACTIVE users");
        DatabaseValidator.validateRowCount("SELECT * FROM USERS WHERE STATUS=?", 1, "ACTIVE");
    }
}
