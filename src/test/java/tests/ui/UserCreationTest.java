package tests.ui;

import framework.base.BaseTest;
import framework.pages.UserManagementPage;
import framework.reporting.TestExecutionLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class UserCreationTest extends BaseTest {

    @Test(groups = {"regression"}, description = "Verify creation of new user record")
    public void testCreateUser() {
        TestExecutionLogger.logStep("Filling out user creation form");
        UserManagementPage userPage = new UserManagementPage(page());
        userPage.createUser("John", "Doe", "john.doe@example.com", "+1555123456", "ADMIN");
        Assert.assertNotNull(userPage.getUserTable());
    }
}
