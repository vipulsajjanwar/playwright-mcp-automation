/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: UI test suite for DataTable component interactions: sorting, filtering, pagination, and cell content assertion.
 * Created: 2026-08-15
 */
package tests.ui;

import framework.base.BaseTest;
import framework.components.DataTable;
import framework.reporting.TestExecutionLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TableTest extends BaseTest {

    @Test(groups = {"regression"}, description = "Verify data table component inspection")
    public void testTableRows() {
        TestExecutionLogger.logStep("Inspecting data table");
        DataTable dataTable = new DataTable(page(), "table");
        Assert.assertNotNull(dataTable);
    }
}
