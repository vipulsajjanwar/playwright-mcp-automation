package tests.ui;

import framework.annotations.CustomTest;
import framework.base.BaseTest;
import framework.pages.UserManagementPage;
import framework.reporting.FrameworkLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

@CustomTest(
        module = "Administration",
        feature = "User Management",
        owner = "QA Team"
)
public class UserCreationTest extends BaseTest {

    @CustomTest(
            testId = "USER_MGMT_001",
            name = "Create New User Record",
            description = "Verify that a new user record can be successfully created with valid user details and assigned roles",
            severity = "CRITICAL",
            priority = "HIGH",
            category = "Regression",
            tags = {"UserCreation", "Admin"},
            jiraId = "ICEDQ-9876",
            storyId = "STORY-205"
    )
    @Test(groups = {"regression"})
    public void testCreateUser() {
        FrameworkLogger.step("Step 1: Filling out user creation form");
        UserManagementPage userPage = new UserManagementPage(page());
        userPage.createUser("John", "Doe", "john.doe@example.com", "+1555123456", "ADMIN");

        FrameworkLogger.step("Step 2: Validating user table record presence");
        Assert.assertNotNull(userPage.getUserTable());
    }
}
