package tests.icedq.admin;

import framework.base.BaseTest;
import com.icedq.admin.AppLoginPage;

import framework.pages.LoginPage;
import framework.reporting.ReportUtils;
import framework.reporting.TestExecutionLogger;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
@Epic("Administration")
@Feature("Login")
public class LoginTest extends BaseTest {

    @Story("Verify that a user can successfully log in with valid credentials")
    @Severity(SeverityLevel.BLOCKER)
    @Issue("ICEDQ-12345")
    @Owner("Your Name")
    @Description("This test verifies that a user can successfully log in with valid credentials.")
    @Test(groups = {"smoke", "regression"}, description = "Verify successful navigation to demo application")
    public void testLoginPageNavigation() {
        String testcaseId = "TC001";
        String testcaseName = "Verify valid login credentials of demo application";
        TestExecutionLogger.OnStartTest(testcaseId, testcaseName);
        try {

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
