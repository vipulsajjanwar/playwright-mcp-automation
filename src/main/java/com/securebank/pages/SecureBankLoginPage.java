package com.securebank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.base.BasePage;
import framework.reporting.ReportUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureBankLoginPage extends BasePage {
    private static final Logger log = LoggerFactory.getLogger(SecureBankLoginPage.class);
    private final Page page;

    public SecureBankLoginPage(Page page) {
        super(page);
        this.page = page;
    }

    Locator usernameField = getByXpath("//input[@id='login-username']");
    Locator passwordField = getByXpath("//input[@id='login-password']");
    Locator loginButton = getByXpath("//button[@aria-label='Sign in to SecureBank']");
    Locator dashboardHeader = getByXpath("//h1[contains(text(),'Welcome back,')]");

    public boolean configureLoginFunctionality(String username, String password) {
        try {

            if(!usernameField.isVisible() && !page.url().contains("/login")){
                log.info("User is already logged in. No need to login again.");
                return true;
            }

            ReportUtils.logInfo("Entering username: "+ username);
            usernameField.fill(username);

            ReportUtils.logInfo("Entering password: "+ password);
            passwordField.fill(password);

            ReportUtils.logInfo("Clicking on login button");
            loginButton.click();

            validateElementVisible(dashboardHeader, "Dashboard header is visible after login");
            return true; // Return true if login is successful
        } catch (Exception e) {
            log.error("Error during login functionality: {}", e.getMessage());
            return false; // Return false if there was an error
        }
    }

    public boolean loginWithRole(String role){
        String username = "";
        String password = "";

        switch (role.toLowerCase()) {
            case "admin":
                username = "standard_user";
                password = "bank_sauce";
                break;
            case "user":
                username = "standard_user";
                password = "bank_sauce";
                break;
            case "guest":
                username = "guest_user";
                password = "guest_password";
                break;
            default:
                log.error("Invalid role specified: {}", role);
                return false; // Return false for invalid role
        }

        return configureLoginFunctionality(username, password);
    }




}
