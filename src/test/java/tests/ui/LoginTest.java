package tests.ui;

import framework.base.BaseTest;
import framework.pages.AppLoginPage;
import framework.pages.LoginPage;
import framework.reporting.ReportUtils;
import framework.reporting.TestExecutionLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test(groups = {"smoke", "regression"}, description = "Verify successful navigation to Todomvc demo application")
    public void testLoginPageNavigation() {
        String testcaseId = "TC001";
        String testcaseName = "Verify valid login credentials of demo application";

        try {
            TestExecutionLogger.logStep(testcaseName);

            AppLoginPage loginPage = new AppLoginPage(page());

            loginPage.loginWithUserDetails("", "");

            loginPage.validateLoginPageTitle("icedq - home");
        } catch (Exception e) {
            ReportUtils.logFail("Error during test execution: " , e);

        }finally{
            TestExecutionLogger.finalizeTest(true,  null, null, testcaseId, testcaseName);
        }

    }

    @Test(groups = {"regression"}, description = "Verify login input fields interaction")
    public void testLoginInputInteraction() {
        TestExecutionLogger.logStep("Interacting with login fields");
        LoginPage loginPage = new LoginPage(page());
        loginPage.login("admin_user", "SecretPass123!");
        Assert.assertNotNull(loginPage.getTitle());
    }
}
