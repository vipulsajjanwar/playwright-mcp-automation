/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Data-driven login test reading credential sets from Excel to validate multiple user scenarios in one suite.
 * Created: 2026-08-15
 */
package tests.integration;

import framework.base.BaseTest;
import framework.constants.FileConstants;
import framework.excel.ExcelReader;
import framework.excel.ExcelUtils;
import framework.pages.LoginPage;
import framework.reporting.TestExecutionLogger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ExcelDataDrivenLoginTest extends BaseTest {

    @BeforeClass(alwaysRun = true)
    public void ensureExcelDataFileExists() {
        if (!framework.utils.FileUtils.exists(FileConstants.EXCEL_TEST_DATA_FILE)) {
            String[][] sampleData = {
                    {"Username", "Password", "ExpectedRole"},
                    {"admin", "admin123", "ADMIN"},
                    {"qa_tester", "qa_pass!", "TESTER"},
                    {"guest_user", "guest99", "GUEST"}
            };
            ExcelUtils.createDummyExcelFile(FileConstants.EXCEL_TEST_DATA_FILE, "Login", sampleData);
        }
    }

    @DataProvider(name = "excelLoginData")
    public Object[][] getLoginDataFromExcel() {
        return ExcelReader.readSheet("Login");
    }

    @Test(dataProvider = "excelLoginData", groups = {"integration", "regression"}, description = "Data-driven login test using Apache POI Excel")
    public void testLoginWithExcelData(String username, String password, String expectedRole) {
        TestExecutionLogger.logStep(String.format("Testing login with username [%s], expected role [%s]", username, expectedRole));
        LoginPage loginPage = new LoginPage(page());
        loginPage.login(username, password);
        Assert.assertNotNull(loginPage.getTitle());
    }
}
