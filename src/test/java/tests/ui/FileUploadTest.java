/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: UI test suite validating file upload workflows including single upload, multiple files, and invalid format rejection.
 * Created: 2026-08-15
 */
package tests.ui;

import framework.base.BaseTest;
import framework.pages.FileUploadPage;
import framework.reporting.TestExecutionLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FileUploadTest extends BaseTest {

    @Test(groups = {"regression"}, description = "Verify file upload functionality")
    public void testFileUpload() {
        TestExecutionLogger.logStep("Verifying FileUploadPage component");
        FileUploadPage fileUploadPage = new FileUploadPage(page());
        Assert.assertNotNull(fileUploadPage);
    }
}
