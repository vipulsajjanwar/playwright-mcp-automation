package com.icedq.admin;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import framework.base.BasePage;
import framework.excel.ExcelReader;
import framework.reporting.ReportUtils;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppLoginPage extends BasePage {

    private static final Logger log = LoggerFactory.getLogger(AppLoginPage.class);
    private final Page page;
    private static final int DEFAULT_TIMEOUT = 30_000;
    private static final int NAVIGATION_TIMEOUT = 5 * 60_000;
    private static final int LOGOUT_TIMEOUT = 60_000;
    private static final int MAX_LOGIN_RETRY = 2;
    private static final int MAX_PAGE_RETRY = 5;

    // ------------------------------------------------------------------
    // Locators
    // ------------------------------------------------------------------

    private final Locator userName;
    private final Locator passWord;
    private final Locator signIn;

    private final Locator signOut;

    private final Locator loginHeader;
    private final Locator welcome;

    private final Locator sendAnyWayButton;
    private final Locator proceedToApplication;
    private final Locator backToApplicationLink;
    private final Locator administrationLabel;

    private final Locator icedqTitle;
    private final Locator forgetPassword;

    private final Locator discardButtons;
    private final Locator sidebarCloseIcon;

    private final Locator workspacePopUpWindow;
    private final Locator okButton;

    private final Locator invalidCredentialMsg;

    private final Locator newPassword;
    private final Locator confirmNewPassword;
    private final Locator submitButton;

    private final String dynamicName;
    public final String forbiddenToastMsg;

    private final Locator icedqLogo;
    private final Locator dataTSpinner;
    private final Locator userProfile;
    private final Locator homeLink;

    private final String popupOpenDiscardCancel;

    public AppLoginPage(Page page) {
        super(page);
        this.page = page;

        userName = getByXpath("//input[@id='username']");
        passWord = getByXpath("//input[@id='password']");
        signIn = getByName("login");

        signOut = getByXpath("Profile.Logout.button");

        loginHeader = getByXpath("//head//title[normalize-space(text())='Sign in to iCEDQ DataOps Platform']");
        welcome = getByXpath("//h1[contains(text(),'welcome')]");

        sendAnyWayButton = getByXpath("//button[@id='details-button']");
        proceedToApplication = getByXpath("//a[@id='proceed-link']");
        backToApplicationLink = getByXpath("//a[contains(text(),'Back to Application')]");
        administrationLabel = getByXpath("//h5[contains(text(),'Administration')]");

        icedqTitle = getByXpath("//div[@id='kc-header-wrapper']");
        forgetPassword = getByXpath("//a[contains(text(),'Forgot Password')]");

        discardButtons = getByXpath("dataT.discardButton");
        sidebarCloseIcon = getByXpath("dataT.Sidebars.CloseIcon");

        workspacePopUpWindow = getByXpath("WorkspacePopUpWindow");
        okButton = getByXpath("WorkspaceOkButton");

        invalidCredentialMsg = getByXpath("//div//span[@id='input-error']");

        newPassword = getByXpath("//input[@name='password-new']");
        confirmNewPassword = getByXpath("//input[@name='password-confirm']");
        submitButton = getByXpath("//input[@type='submit']");

        dynamicName = "$name";

        forbiddenToastMsg = "You are not authorized to access this application. Please contact your administrator.";

        icedqLogo = getByXpath("//a[@aria-label='iceDQ home']//img[@alt='iceDQ Logo']");
        dataTSpinner = getByXpath("//div[@id='sp-container']//div[@class='e-spinner-pane e-spin-show']//div | //div[@class='spinner']");
        userProfile = getByXpath("(//div[contains(@class,'user-dd')]//button[@id='ejsbtndd' or @target='#userPopup' or contains(@id,'dropdown')])[1] | //lib-profile-menu//button[contains(@aria-label,'profile menu')]");

        popupOpenDiscardCancel = "discard";

        homeLink = getByXpath("//lib-icedq-breadcrumb//a[normalize-space(text())='Home']");
    }

    // ==================================================================
    // LOGIN
    // ==================================================================
    @Step("Login with user details: {username}")
    public boolean loginWithUserDetails(
            String url,
            String appUser
            ) {

        try {
            String username;
            String password;
            String temporaryPassword = "";

            if (appUser != null && !appUser.isBlank()) {
                if (appUser.equalsIgnoreCase("sadmin")) {
                    username = "admin";
                    password = "Admin@123";
                } else {
                    String[] loginDetails =
                           new ExcelReader().readDataFromExcelUsingColumnName(appUser);

                    if (loginDetails == null || loginDetails.length < 2) {
                        throw new IllegalStateException(
                                "Invalid login details returned for user: " + appUser);
                    }

                    username = loginDetails[0];
                    password = loginDetails[1];

                    if (loginDetails.length > 2 && loginDetails[2] != null) {
                        temporaryPassword = loginDetails[2];
                    }
                }
            } else {
                username = "admin";
                password = "Admin@123";
            }

            boolean loginSuccess =
                    loginToAppWithDifferentRoleNew(url, username, password, temporaryPassword);

            if (!loginSuccess) {
               ReportUtils.logFail(
                        "Unable to login with supplied credentials",
                        "loginWithappUserAndSelectWorkspace", page);
                return false;
            }

           ReportUtils.logInfo("Login successful. Selecting module and workspace.");

        } catch (Exception e) {
           ReportUtils.logFail(
                    "Exception occurs kindly check: " + e.getMessage(),
                    "loginWithappUserAndSelectWorkspace", page);
            return false;
        }
        return true;
    }

    public boolean loginToAppWithDifferentRoleNew(String url,
            String username,
            String password,
            String temporaryPassword) {

        for (int attempt = 1; attempt <= MAX_LOGIN_RETRY; attempt++) {
            try {
               ReportUtils.logInfo(
                        "Starting login attempt " + attempt + "/" + MAX_LOGIN_RETRY);

                // Step 1: make sure application is at login page
                if (!checkForUserNameOnPage(url)) {
                    throw new AssertionError("Unable to reach application login page.");
                }

                // Step 2: validate login page
                validateLoginPage();

                // Step 3: enter credentials
                if (!enterUserNameAndPasswordToSignIn(username, password)) {
                    throw new AssertionError("Unable to enter username/password.");
                }

                // Step 4: wait for login result
                waitForLoginResult();

                // Step 5: successful login
                if (isVisible(welcome, 10_000)) {
                    displayWelcomeMessage();
                    return true;
                }

                // Step 6: invalid credentials / temporary password
                handleInvalidOrChangedPassword(username, password, temporaryPassword);

                // Step 7: wait again for successful login
                waitForLoginResult();

                if (isVisible(welcome, 15_000)) {
                    displayWelcomeMessage();
                    return true;
                }

                // Step 8: refresh and recover
               ReportUtils.logInfo("Welcome message not visible. Attempting login recovery.");

                page.reload(new Page.ReloadOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                waitForApplicationReady();

                if (isVisible(backToApplicationLink, 3_000)) {
                   ReportUtils.logFail(
                            "Logout screen appears, kindly check",
                            "loginToAppWithDifferentRoleNew", page);

                    click(backToApplicationLink, "Back to Application");
                    waitForApplicationReady();

                    if (isVisible(welcome, 10_000)) {
                        displayWelcomeMessage();
                        return true;
                    }
                }

                // Step 9: final validation
                if (isVisible(welcome, 15_000)) {
                    displayWelcomeMessage();
                    return true;
                }

               ReportUtils.logFail(
                        "User not logged into application. Welcome message was not displayed.",
                        "loginToAppWithDifferentRoleNew", page);

            } catch (Exception e) {
                ReportUtils.logError(
                        e, "loginToAppWithDifferentRoleNew attempt " + attempt);
            }

            if (attempt < MAX_LOGIN_RETRY) {
               ReportUtils.logInfo("Retrying login. Next attempt: " + (attempt + 1));
                safeNavigateToApplication(url);
            }
        }

        return false;
    }

    private void validateLoginPage() {
        checkForField(userName,
                "You are on the Login Page!", "Login Page is not loaded!");
        checkForField(icedqTitle,
                "icedq Logo image is visible", "iceDQ logo is not loaded!");
        checkForField(forgetPassword,
                "Forgot Password link is visible", "Forgot Password link is not visible!");
        checkForField(userName,
                "Username field is visible", "Username field is not visible!");
        checkForField(passWord,
                "Password field is visible", "Password field is not visible!");
        checkForField(signIn,
                "Sign In button is visible", "Sign In button is not visible!");
    }

    private void waitForLoginResult() {
        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        } catch (Exception e) {
           ReportUtils.logInfo("DOM content load wait completed with: " + e.getMessage());
        }
        waitForApplicationReady();
    }

    private void displayWelcomeMessage() {
        String welcomeText = welcome.textContent();

       ReportUtils.logInfo("Welcome message is visible: " + welcomeText);
       ReportUtils.logPass("You are on the icedq welcome page!");
        log.info("Welcome message is visible: " + welcomeText);
    }

    // ==================================================================
    // PASSWORD / CREDENTIAL HANDLING
    // ==================================================================

    private void handleInvalidOrChangedPassword(
            String username,
            String password,
            String temporaryPassword) {

        if (isVisible(invalidCredentialMsg, 3_000)) {
           ReportUtils.logInfo("Invalid credential message detected.");

            if (temporaryPassword == null || temporaryPassword.isBlank()) {
               ReportUtils.logFail(
                        "Invalid credentials detected but temporary password is not available.",
                        "handleInvalidOrChangedPassword", page);
                return;
            }

            // Refresh before retrying with the temporary password.
            page.reload(new Page.ReloadOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            waitForApplicationReady();

            if (!enterUserNameAndPasswordToSignIn(username, temporaryPassword)) {
                throw new AssertionError("Unable to login using temporary password.");
            }

            // Password-change screen should now appear.
            if (isVisible(newPassword, 10_000)) {
               ReportUtils.logInfo("New password screen detected.");

                if (!enterNewPasswordToSignIn(password)) {
                    throw new AssertionError("Unable to set new password.");
                }
            }

        } else if (isVisible(newPassword, 3_000)) {
            // Password-change screen appeared directly.
           ReportUtils.logInfo("New password screen detected directly.");

            if (!enterNewPasswordToSignIn(password)) {
                throw new AssertionError("Unable to submit new password.");
            }
        }
    }
    @Step("Login to application with user: {username}")
    public boolean enterUserNameAndPasswordToSignIn(String username, String password) {
        try {
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Username cannot be null or empty.");
            }
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("Password cannot be null or empty.");
            }

            fill(userName, username, "Username");
            validateInputValue(userName, username, "Username");

            fill(passWord, password, "Password");
            // Do not log the password value.
            validateInputNotEmpty(passWord, "Password");

            click(signIn, "Sign In");

           ReportUtils.logInfo("Username and password submitted successfully.");
            return true;

        } catch (Exception e) {
            ReportUtils.logError(e, "enterUserNameAndPasswordToSignIn");
            return false;
        }
    }
    @Step("Enter new password and confirm to sign in")
    public boolean enterNewPasswordToSignIn(String password) {
        try {
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("New password cannot be null or empty.");
            }

            fill(newPassword, password, "New Password");
            validateInputNotEmpty(newPassword, "New Password");

            fill(confirmNewPassword, password, "Confirm Password");
            validateInputNotEmpty(confirmNewPassword, "Confirm Password");

            click(submitButton, "Submit");

           ReportUtils.logInfo("New password submitted successfully.");
            return true;

        } catch (Exception e) {
            ReportUtils.logError(e, "enterNewPasswordToSignIn");
            return false;
        }
    }

    // ==================================================================
    // LOGIN PAGE / CERTIFICATE / POPUP RECOVERY
    // ==================================================================
    @Step("Handle certificate or popup recovery if present")
    public boolean checkForUserNameOnPage(String url) {
        for (int attempt = 1; attempt <= MAX_PAGE_RETRY; attempt++) {
            try {
                // Already on login page.
                if (isVisible(userName, 3_000)) {
                   ReportUtils.logInfo("You are on login page...");
                    return true;
                }

                // Browser certificate / Send Anyway flow.
                if (validateBrowserCertificate()) {
                    return true;
                }

                // Recover application state.
                if (handleAllPermutationForLogin(url) && isVisible(userName, 5_000)) {
                    return true;
                }

                // Back to application screen.
                if (isVisible(backToApplicationLink, 3_000)) {
                    safeNavigateToApplication(url);

                    if (isVisible(userName, 10_000)) {
                        return true;
                    }
                }

                // Workspace popup or password-change screen.
                if (isVisible(workspacePopUpWindow, 2_000) || isVisible(newPassword, 2_000)) {
                    safeNavigateToApplication(url);

                    if (isVisible(userName, 10_000)) {
                        return true;
                    }
                }

            } catch (TimeoutError e) {
                ReportUtils.logError(e, "checkForUserNameOnPage");
                safeNavigateToApplication(url);

            } catch (Exception e) {
                ReportUtils.logError(e, "checkForUserNameOnPage");
            }

            if (attempt < MAX_PAGE_RETRY) {
               ReportUtils.logInfo(
                        "Retrying checkForUserNameOnPage. Attempt: " + (attempt + 1));
                safeNavigateToApplication(url);
            }
        }

        return isVisible(userName, 5_000);
    }
    @Step("Handle browser certificate validation if present")
    public void navigateToURL(String url, int timeoutMinutes) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Application URL cannot be null or empty.");
        }

        try {
           ReportUtils.logInfo("Navigating to Application URL: " + url);

            page.setDefaultNavigationTimeout(timeoutMinutes * 60_000L);
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            waitForApplicationReady();

        } catch (Exception e) {
            ReportUtils.logError(e, "navigateToURL");
            throw e;
        }
    }

    private void safeNavigateToApplication(String url) {
        try {
            navigateToURL(url, 3);
        } catch (Exception e) {
            ReportUtils.logError(e, "safeNavigateToApplication");
        }
    }

    public void checkForField(Locator locator, String successMessage, String errorMessage) {

        if (isVisible(locator, 5_000)) {
           ReportUtils.logInfo(successMessage);
        } else {
           ReportUtils.logFail(errorMessage, "checkForField", page);
        }
    }
    @Step("Handle all permutations for login recovery")
    public boolean navigateToAppPage() {
        try {
            // Browser certificate warning.
            if (!isVisible(sendAnyWayButton, 3_000)) {
                return false;
            }

            click(sendAnyWayButton, "Send Anyway");
           ReportUtils.logInfo("Successfully clicked on Send Anyway button.");

            // Administration / application landing page.
            isVisible(administrationLabel, 15_000);

            click(proceedToApplication, "Proceed To Application");

            // Login page should now be visible.
            if (!isVisible(userName, 30_000)) {
               ReportUtils.logFail(
                        "Username field not visible after Proceed To Application.",
                        "navigateToAppPage", page);
                return false;
            }

           ReportUtils.logInfo("Application login page is available.");
            return true;

        } catch (Exception e) {
            ReportUtils.logError(e, "navigateToAppPage");
            return false;
        }
    }

    private boolean validateBrowserCertificate() {
        if (!isVisible(sendAnyWayButton, 3_000)) {
            return false;
        }

       ReportUtils.logInfo("Browser certificate warning detected.");

        return navigateToAppPage() && isVisible(userName, 15_000);
    }

    // ==================================================================
    // LOGIN STATE RECOVERY
    // ==================================================================

    private boolean recheckAppURLLaunch(String url) {
        if (isVisible(userName, 2_000)) {
            return false;
        }

       ReportUtils.logInfo("Login page not visible. Re-launching application.");

        safeNavigateToApplication(url);

        if (isVisible(sendAnyWayButton, 2_000)) {
            navigateToAppPage();
        }

        return isVisible(userName, 10_000);
    }

    private boolean validateWorkspacePopup() {
        if (!isVisible(workspacePopUpWindow, 2_000)) {
            return false;
        }

       ReportUtils.logInfo("Workspace popup detected.");
        click(okButton, "OK");

        return isVisible(userName, 10_000);
    }

    private boolean validateLogoutFromApp() {
        if (!isVisible(userProfile, 2_000)) {
            return false;
        }

       ReportUtils.logInfo("Existing logged-in session detected. Logging out.");

        return logoutFromApplication() && isVisible(userName, 10_000);
    }

    private boolean handleUnwantedPopupAndLogout() {
        if (!isVisible(userProfile, 2_000)) {
            return false;
        }

        handleDiscardDeletePopupOpenAndLogout();

        return logoutFromApplication() && isVisible(userName, 10_000);
    }

    private boolean handleAllPermutationForLogin(String url) {
        return recheckAppURLLaunch(url)
                || validateWorkspacePopup()
                || validateLogoutFromApp()
                || handleUnwantedPopupAndLogout();
    }

    // ==================================================================
    // LOGOUT
    // ==================================================================

    public boolean logoutFromApplication() {
        try {
            if (!logoutSteps()) {
               ReportUtils.logFail(
                        "Unable to open user profile/logout menu.",
                        "logoutFromApplication", page);
                return false;
            }

            waitForSignOut();

            if (!isUserLoggedOut()) {
               ReportUtils.logFail(
                        "Unable to log out from the application.",
                        "logoutFromApplication", page);
                return false;
            }

           ReportUtils.logInfo("User Logout is successful");
           ReportUtils.logPass("User Logout successfully from the application!!");
            return true;

        } catch (Exception e) {
            throw new AssertionError("Unable to log out from the application!!", e);
        }
    }

    private boolean logoutSteps() {
        try {
            // Click icedq logo if it is part of the current navigation state.
            if (isVisible(icedqLogo, 3_000)) {
                click(icedqLogo, "icedq Logo");
               ReportUtils.logInfo("Clicked on icedq logo.");
                waitForSpinnerToDisappear();
            }

            // Open profile.
            if (!isVisible(userProfile, 5_000)) {
               ReportUtils.logInfo(
                        "User profile not immediately visible. Retrying navigation.");

                if (isVisible(icedqLogo, 3_000)) {
                    click(icedqLogo, "icedq Logo");
                    waitForSpinnerToDisappear();
                }
            }

            if (!isVisible(userProfile, 10_000)) {
               ReportUtils.logFail(
                        "User profile option is not visible.", "logoutSteps", page);
                return false;
            }

            click(userProfile, "User Profile");
           ReportUtils.logInfo("Clicked on profile drop-down option.");
            waitForSpinnerToDisappear();

            // Logout option.
            if (!isVisible(signOut, 10_000)) {
               ReportUtils.logFail(
                        "Logout button is not visible after opening profile dropdown.",
                        "logoutSteps", page);
                return false;
            }

            return true;

        } catch (Exception e) {
            ReportUtils.logError(e, "logoutSteps");
            return false;
        }
    }

    private void waitForSpinnerToDisappear() {
        try {
            if (dataTSpinner.count() == 0) {
                return;
            }

            dataTSpinner.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN)
                    .setTimeout(DEFAULT_TIMEOUT));

        } catch (TimeoutError e) {
           ReportUtils.logInfo("Spinner did not disappear within timeout.");
        }
    }

    private void waitForSignOut() {
        if (!isVisible(signOut, 15_000)) {
            throw new AssertionError("Logout button was not visible.");
        }

        click(signOut, "Logout");
       ReportUtils.logInfo("Clicked on Logout button.");
    }

    /**
     * Post-conditions of a successful logout:
     * 1. Login username field is visible.
     * 2. Logout button is no longer visible.
     */
    private boolean isUserLoggedOut() {
        boolean loginVisible = isVisible(userName, LOGOUT_TIMEOUT);

        if (!loginVisible) {
           ReportUtils.logInfo("Username field not visible after logout. Refreshing page.");

            page.reload(new Page.ReloadOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            loginVisible = isVisible(userName, 30_000);
        }

        waitForSpinnerToDisappear();

        boolean logoutButtonVisible = isVisible(signOut, 2_000);

        if (!loginVisible) {
           ReportUtils.logFail(
                    "Login page was not displayed after logout.", "isUserLoggedOut", page);
            return false;
        }

        if (logoutButtonVisible) {
           ReportUtils.logFail(
                    "Logout button is still visible after logout.", "isUserLoggedOut", page);
            return false;
        }

        return true;
    }

    // ==================================================================
    // ALTERNATIVE LOGOUT FLOW
    // ==================================================================

    public boolean logOut() {
        try {
            // Already on login page.
            if (isVisible(signIn, 3_000)) {
               ReportUtils.logInfo("You are already on login page.");
                return true;
            }

            // Navigate through Home breadcrumb if available.
            if (isVisible(homeLink, 3_000)) {
                click(homeLink, "Home");
                waitForApplicationReady();
                waitForSpinnerToDisappear();
            }

            // Open user profile.
            if (!isVisible(userProfile, 30_000)) {
                throw new AssertionError("User profile option not visible.");
            }

            click(userProfile, "User Profile");
           ReportUtils.logInfo("Clicked on profile drop-down option.");

            // Logout button.
            if (!isVisible(signOut, 30_000)) {
               ReportUtils.logFail(
                        "Logout button not visible on current page.", "logOut", page);

                // Reopen profile once.
                if (isVisible(userProfile, 3_000)) {
                    click(userProfile, "User Profile");

                    if (!isVisible(signOut, 10_000)) {
                        throw new AssertionError("Logout button is still not visible.");
                    }
                } else {
                    throw new AssertionError("User profile is not visible.");
                }
            }

            click(signOut, "Logout");
           ReportUtils.logInfo("Clicked on Logout button.");

            // Primary logout validation.
            if (isVisible(signIn, 10_000)) {
                reportSuccessfulLogout();
                return true;
            }

            // Optional popup handling.
            handleOptionalElements();

            // Secondary logout validation.
            if (isVisible(signIn, 10_000)) {
                reportSuccessfulLogout();
                return true;
            }

            throw new AssertionError("User is not redirected to login page after logout.");

        } catch (Exception e) {
            throw new AssertionError("Unable to log out from the application!", e);
        }
    }

    private void reportSuccessfulLogout() {
       ReportUtils.logInfo("User Logout is successful");
       ReportUtils.logPass("User Logout successfully from the application!!");
    }

    // ==================================================================
    // POPUPS
    // ==================================================================

    public void handleDiscardDeletePopupOpenAndLogout() {
        Locator discardLocator =
                getDynamicLocator(popupOpenDiscardCancel, dynamicName, "Discard");
        Locator cancelLocator =
                getDynamicLocator(popupOpenDiscardCancel, dynamicName, "Cancel");

        if (isVisible(discardLocator, 2_000)) {
            click(discardLocator, "Discard");
        } else if (isVisible(cancelLocator, 2_000)) {
            click(cancelLocator, "Cancel");
        }
    }

    private void handleOptionalElements() {
        if (isVisible(discardButtons, 2_000)) {
            click(discardButtons, "Discard");
        }

        if (isVisible(sidebarCloseIcon, 2_000)) {
            click(sidebarCloseIcon, "Sidebar close icon");
        }
    }

    // ==================================================================
    // PLAYWRIGHT UTILITY METHODS
    // ==================================================================

    private boolean isVisible(Locator locator, double timeoutMs) {
        if (locator == null) {
            return false;
        }

        try {
            locator.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeoutMs));

            return locator.isVisible();

        } catch (Exception e) {
            // Includes TimeoutError: element not visible within the timeout.
            return false;
        }
    }

    private void validateInputValue(Locator locator, String expectedValue, String fieldName) {
        String actualValue = locator.inputValue();

        if (!expectedValue.equals(actualValue)) {
            throw new AssertionError(fieldName + " value was not entered correctly.");
        }

       ReportUtils.logPass(fieldName + " value entered successfully.");
    }

    private void validateInputNotEmpty(Locator locator, String fieldName) {
        String value = locator.inputValue();

        if (value == null || value.isBlank()) {
            throw new AssertionError(fieldName + " field is empty.");
        }
    }

    /**
     * Deliberately does not wait for networkidle: iceDQ (Angular) keeps
     * long-running requests/polling open. Waits on the app spinner instead.
     */
    private void waitForApplicationReady() {
        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        } catch (Exception e) {
           ReportUtils.logInfo("DOM load state wait completed: " + e.getMessage());
        }

        waitForSpinnerToDisappear();
    }

    // ==================================================================
    // PROPERTY / LOCATOR ADAPTER
    // ==================================================================

    private Locator getDynamicLocator(String template, String dynamicKey, String value) {
        return page.locator(normalizeSelector(template.replace(dynamicKey, value)));
    }

    /**
     * Returns the selector unchanged. Property values may be Playwright
     * selectors (css=..., xpath=..., text=...) or raw CSS.
     */
    private String normalizeSelector(String selector) {
        if (selector == null || selector.isBlank()) {
            throw new IllegalArgumentException("Locator selector cannot be null or empty.");
        }
        return selector;
    }
    @Step("Verify Login page title is displayed")
    public void validateLoginPageTitle(String expectedTitle) {
        validateTitle(expectedTitle, "Login page title validation", "equalsIgnoreCase");
    }
}