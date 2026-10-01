package tests.icedq.admin;

import com.icedq.admin.AppLoginPage;
import framework.annotations.CustomTest;
import framework.base.BaseTest;
import framework.pages.LoginPage;
import framework.reporting.FrameworkLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

@CustomTest(
        module = "Administration",
        feature = "Login",
        owner = "Vipul",
        category = "Regression",
        requirementId = "REQ-USER-102"
)
public class LoginTest extends BaseTest {

    @CustomTest(
            testId = "TC_LOGIN_001",
            name = "Verify Valid Login Credentials",
            description = "Verify that a user can successfully log in with valid credentials and navigate to the home dashboard",
            subFeature = "valid user login",
            severity = "BLOCKER",
            priority = "HIGH",
            jiraId = "ICEDQ-12345",
            storyId = "STORY-101",
            tags = {"login", "valid-credentials", "smoke"}
    )
    @Test(groups = {"smoke", "regression"})
    public void testLoginPageNavigation() {
        FrameworkLogger.step("Step 1: Initializing login page");
        AppLoginPage loginPage = new AppLoginPage(page());

        FrameworkLogger.step("Step 2: Submitting user credentials");
        loginPage.loginWithUserDetails("", "");

        FrameworkLogger.step("Step 3: Validating home page title");
        loginPage.validateLoginPageTitle("icedq - home");
    }

    @CustomTest(
            testId = "TC_LOGIN_002",
            name = "Verify Login Input Fields Interaction",
            description = "Verify login input fields interaction and form entry",
            severity = "NORMAL",
            priority = "MEDIUM",
            tags = {"login", "ui"}
    )
    @Test(groups = {"regression"})
    public void testLoginInputInteraction() {
        FrameworkLogger.step("Step 1: Interacting with login input fields");
        LoginPage loginPage = new LoginPage(page());
        loginPage.login("admin_user", "SecretPass123!");
        Assert.assertNotNull(loginPage.getTitle());
    }
}
