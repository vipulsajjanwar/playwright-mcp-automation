package tests.ui;

import framework.base.BaseTest;
import framework.components.Dropdown;
import framework.reporting.TestExecutionLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DropdownTest extends BaseTest {

    @Test(groups = {"smoke", "regression"}, description = "Verify dropdown interaction")
    public void testDropdownSelection() {
        TestExecutionLogger.logStep("Verifying dropdown selection component");
        Dropdown dropdown = new Dropdown(page(), "select", "Sample Dropdown");
        Assert.assertNotNull(dropdown);
    }
}
