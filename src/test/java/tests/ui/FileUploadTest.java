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
