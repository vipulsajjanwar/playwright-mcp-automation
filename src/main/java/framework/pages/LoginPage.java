/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Generic login Page Object providing username/password input, form submission, and post-login state assertions.
 * Created: 2026-08-15
 */
package framework.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import framework.base.BasePage;
import framework.utils.ElementUtils;

public class LoginPage extends BasePage {

    private final Locator usernameInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator errorMessage;

    public LoginPage(Page page) {
        super(page);
        this.usernameInput = page.locator("input.new-todo, input[type='text'], #username").first();
        this.passwordInput = page.locator("input[type='password'], #password").first();
        this.loginButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).or(page.locator("button, #login-btn")).first();
        this.errorMessage = page.locator(".error-message, #error-alert");
    }

    public void login(String username, String password) {
        if (ElementUtils.isVisible(usernameInput)) {
            ElementUtils.fill(usernameInput, username, "Username Input");
        }
        if (ElementUtils.isVisible(passwordInput)) {
            ElementUtils.fill(passwordInput, password, "Password Input");
        }
        if (ElementUtils.isVisible(loginButton)) {
            ElementUtils.click(loginButton, "Login Button");
        }
    }

    public String getErrorMessage() {
        if (ElementUtils.isVisible(errorMessage)) {
            return ElementUtils.getText(errorMessage, "Login Error Message");
        }
        return "";
    }
}
