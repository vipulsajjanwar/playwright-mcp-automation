/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Page Object for the SecureBank Dashboard, encapsulating navigation, widget interactions, and dashboard-level assertions.
 * Created: 2026-10-23
 */
package com.securebank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.base.BasePage;
import framework.reporting.FrameworkLogger;
import framework.reporting.ReportUtils;
import framework.utils.DateUtils;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardPage extends BasePage {
    private static final Logger log = LoggerFactory.getLogger(DashboardPage.class);
    private final Page page;
    public DashboardPage(Page page) {
        super(page);
        this.page = page;
    }

    String recentTxnTableXpath ="//table[@data-testid='recent-transactions-table']";
    @Step("Click on module {moduleName} in the dashboard")
    public void clickOnModuleTab(String moduleName, String title){

        try {
            Locator moduleTab = getByXpath("//span[normalize-space(text())='"+moduleName+"']//parent::a");

            click(moduleTab, moduleName);

            Locator moduleTitle = getByXpath("//h1[contains(text(),'"+title+"')]");

            validateElementVisible(moduleTitle, "Module title is visible after clicking on module tab");
        } catch (Exception e) {
            log.error("Error clicking on module tab {}: {}", moduleName, e.getMessage());
        }

    }
    @Step("Get total net worth displayed on the dashboard")
    public String getTotalNetWorth(){
        try {
            Locator netWorthElement = getByXpath("//div[@data-metric='net-worth']//p[contains(@data-testid,'net-worth-value')]");

            String netWorthText = netWorthElement.innerText();
            ReportUtils.logInfo("Total Net Worth: " + netWorthText);
            return netWorthText;
        } catch (Exception e) {
            log.error("Error retrieving total net worth: {}", e.getMessage());
        }
        return null;
    }

    @Step("Comparing displayed net worth with expected total account balance")
    public void compareNetWorthWithExpected(double expectedNetWorth) {
        try {

            clickOnModuleTab("Dashboard", "Welcome back,");

            String netWorthText = getTotalNetWorth();
            if (netWorthText != null) {
                double actualNetWorth = Double.parseDouble(netWorthText.replace("$", "").replace(",", ""));
                if (actualNetWorth == expectedNetWorth) {
                    ReportUtils.logInfo("Actual Net Worth matches the expected value: $" + expectedNetWorth);
                } else {
                    ReportUtils.logFail("Actual Net Worth $" + actualNetWorth + " does not match the expected value: $" + expectedNetWorth, new Throwable("Net Worth Mismatch"));
                }
            } else {
                ReportUtils.logFail("Unable to retrieve actual Net Worth for comparison.", new Throwable("Net Worth Retrieval Error"));
            }
        } catch (Exception e) {
            log.error("Error parsing Net Worth value: {}", e.getMessage());
            ReportUtils.logError(e,"Error parsing Net Worth value for comparison.");
        }
    }

    @Step("Validating Recent Transactions Widget")
    public void getRecentTransactionTable(){

        try {
            Locator rowCount = getByXpath(recentTxnTableXpath+"//tr//td[@data-testid='recent-txn-description']");

            int txnCount = rowCount.count();
            String [] headers = {"Date", "Description", "Amount", "Balance"};
            String[][] table = new String[txnCount + 1][headers.length];
            table[0] = headers;

            List<List<String>> tableData = new ArrayList<>();

            for (int i = 1; i <= txnCount; i++) {
                for (int j = 0; j < headers.length; j++) {

                    String cellXpath = recentTxnTableXpath + "//tr[" + i + "]//td[" + (j + 1) + "]";
                    Locator cellLocator = getByXpath(cellXpath);
                    String cellText = cellLocator.innerText();
                    table[i][j] = cellText;

                }
            }

            ReportUtils.logInfo("Recent Transactions Table Data:");
            ReportUtils.logTable("Recent Transactions", table);
        } catch (Exception e) {
            log.error("Error retrieving recent transaction table: {}", e.getMessage());
        }

    }

    @Step("Validate Recent Transaction Table Data")
    public void validateRecentTransactionsWidget(){

        try {
            Locator recentTransactionsWidget = getByXpath(recentTxnTableXpath+"//tr//td[@data-testid='recent-txn-description']");

            waitForVisible(recentTransactionsWidget.nth(0), 5000);

            int txnCount = recentTransactionsWidget.count();

            ReportUtils.logInfo("Number of recent transactions displayed: " + txnCount);

            Assert.assertEquals(txnCount, 5, "Expected 5 recent transactions to be displayed, but found " + txnCount);

            List<LocalDate> dates = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

            for (int i = 0; i < txnCount; i++) {
                String dateText = getByXpath(recentTxnTableXpath + "//tr[" + (i + 1) + "]//td[1]").innerText();

                LocalDate date = LocalDate.parse(dateText, formatter);
                dates.add(date);
            }

            for (int i = 1; i < dates.size(); i++) {
                LocalDate previous = dates.get(i - 1);
                LocalDate current = dates.get(i);

                Assert.assertTrue(
                        previous.isAfter(current) || previous.isEqual(current),
                        "Transactions are not sorted by date descending. Found: "
                                + previous + " before " + current
                );
            }

            ReportUtils.logPass("Transactions are sorted by date descending");

            getRecentTransactionTable();
        } catch (Exception e) {
            log.error("Error validating recent transactions widget: {}", e.getMessage());
            ReportUtils.logError(e, "Error validating recent transactions widget.");
        }
    }

    @Step("Validate link on dashboard")
    public void validateLinkOnDashboard(String linkText, String expectedUrlContains){

        try {
            Locator linkLocator = getByXpath("//p[normalize-space(text())='" + linkText + "']//ancestor::a");

            click(linkLocator, linkText);

           waitForCondition(() -> page.url().contains(expectedUrlContains), 5000, "Expected URL to contain: " + expectedUrlContains);

            String currentUrl = page.url();

            ReportUtils.logInfo("Current URL after clicking the link: " + currentUrl);

            Assert.assertEquals(currentUrl.contains(expectedUrlContains), true, "The current URL does not contain the expected substring: " + expectedUrlContains);

            ReportUtils.logPass("The current URL contains the expected substring: " + expectedUrlContains);
        } catch (Exception e) {
            log.error("Error validating link on dashboard: {}", e.getMessage());
            ReportUtils.logError(e, "Error validating link on dashboard.");
        }

    }

    public void validateThemeToggle(String expectedTheme) {
        try {
            Locator themeToggle = getByXpath("//button[contains(@class,'themeToggle')]");

            String currentTheme = themeToggle.getAttribute("title");
            ReportUtils.logInfo("Current theme: " + currentTheme);

            if (currentTheme.contains(expectedTheme)) {
                click(themeToggle, "Theme Toggle Button");
                waitForCondition(() -> !themeToggle.getAttribute("title").contains(expectedTheme), 5000, "Waiting for theme to toggle to: " + expectedTheme);
            } else {
                ReportUtils.logInfo("Current theme matches the expected theme: " + expectedTheme);
            }

            Locator tagCheck = getByXpath("//html[contains(@class,'" + expectedTheme.toLowerCase() + "')]");

            Assert.assertTrue(tagCheck.isVisible(), "The HTML tag does not contain the expected theme class: " + expectedTheme.toLowerCase());
            ReportUtils.logPass("Theme toggle validated successfully. Current theme is: " + expectedTheme);


        } catch (Exception e) {
            log.error("Error validating theme toggle: {}", e.getMessage());
            ReportUtils.logError(e, "Error validating theme toggle.");
        }



    }

}
