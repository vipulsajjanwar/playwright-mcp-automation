/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: UI test suite for the SecureBank login screen covering valid login, invalid credentials, and session timeout.
 * Created: 2026-10-23
 */
package tests.securebank;

import com.securebank.pages.SecureBankLoginPage;
import framework.annotations.CustomTest;
import framework.base.BaseTest;
import org.testng.annotations.Test;

@CustomTest(
        module = "Administration",
        feature = "Login",
        owner = "Vipul",
        category = "Regression",
        requirementId = "REQ-USER-101"
)
public class SecureBankLoginTest extends BaseTest {

    @CustomTest(
            testId = "TC-DASH-001",
            name = "Verify Dashboard Load",
            description = "Verify that a user can successfully log in with valid credentials and navigate to the home dashboard",
            subFeature = "valid user login",
            severity = "High",
            priority = "P1",
            jiraId = "SB_12345",
            storyId = "SB_STORY-001",
            tags = {"login", "valid-credentials", "smoke"}
    )
    @Test(groups = {"smoke", "regression"})
    public void verifyDashboardLoad() {
        try {

            new SecureBankLoginPage(page()).loginWithRole("admin");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            afterTest();
        }
    }

}
