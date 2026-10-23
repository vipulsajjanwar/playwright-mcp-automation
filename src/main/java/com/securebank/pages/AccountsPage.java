package com.securebank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.base.BasePage;
import framework.reporting.ReportUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AccountsPage extends BasePage {
    private static final Logger log = LoggerFactory.getLogger(AccountsPage.class);
    private final Page page;

    public AccountsPage(Page page) {
        super(page);
        this.page = page;
    }

    public double calculateTotalBalance(){

        double totalBalance = 0;
        try {

           new DashboardPage(page).clickOnModuleTab("Accounts", "My Accounts");

            Locator totalBalanceElement = getByXpath("//table[@aria-label='Accounts']//tr//td[@data-testid='account-row-balance']");

            int totalAccountBal = totalBalanceElement.count();
            List<String> accountBalances = new ArrayList<>();
            for(int i=0; i<totalAccountBal; i++) {
                String balanceText = totalBalanceElement.nth(i).innerText();
                ReportUtils.logInfo("Account " + (i + 1) + " Balance: " + balanceText);
                accountBalances.add(balanceText);
            }

            ReportUtils.logInfo("Calculating total balance from all accounts");
            totalBalance = accountBalances.stream()
                    .mapToDouble(balance -> Double.parseDouble(balance.replace("$", "").replace(",", "")))
                    .sum();
            ReportUtils.logInfo("Total Balance across all accounts: $" + totalBalance);

        } catch (Exception e) {
            log.error("Error calculating total balance: {}", e.getMessage());
        }

        return totalBalance;
    }
}
