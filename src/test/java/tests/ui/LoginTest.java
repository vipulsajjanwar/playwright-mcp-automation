package tests.ui;

import framework.base.BaseTest;
import framework.pages.LoginPage;
import framework.reporting.TestExecutionLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test(groups = {"smoke", "regression"}, description = "Verify successful navigation to Todomvc demo application")
    public void testPageNavigation() {
        TestExecutionLogger.logStep("Navigating to TodoMVC Demo");
        LoginPage loginPage = new LoginPage(page());
        Assert.assertTrue(loginPage.getTitle().toLowerCase().contains("todo"), "Title should contain todo");
    }

    @Test(groups = {"regression"}, description = "Verify login input fields interaction")
    public void testLoginInputInteraction() {
        TestExecutionLogger.logStep("Interacting with login fields");
        LoginPage loginPage = new LoginPage(page());
        loginPage.login("admin_user", "SecretPass123!");
        Assert.assertNotNull(loginPage.getTitle());
    }
}
