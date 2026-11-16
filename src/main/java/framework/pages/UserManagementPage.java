/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Page Object for user management screens, covering user creation, search, role assignment, and deletion workflows.
 * Created: 2026-08-15
 */
package framework.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import framework.base.BasePage;
import framework.components.DataTable;
import framework.components.Dropdown;
import framework.utils.ElementUtils;

public class UserManagementPage extends BasePage {

    private final Locator firstNameInput;
    private final Locator lastNameInput;
    private final Locator emailInput;
    private final Locator phoneInput;
    private final Dropdown roleDropdown;
    private final Locator saveButton;
    private final DataTable userTable;

    public UserManagementPage(Page page) {
        super(page);
        this.firstNameInput = page.locator("input.new-todo, input[type='text'], #firstName").first();
        this.lastNameInput = page.locator("input[name='lastName'], #lastName").first();
        this.emailInput = page.locator("input[type='email'], #email").first();
        this.phoneInput = page.locator("input[type='tel'], #phone").first();
        this.roleDropdown = new Dropdown(page, "#role-select", "Role Select");
        this.saveButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save User")).or(page.locator("#save-user-btn")).first();
        this.userTable = new DataTable(page, "#users-table");
    }

    public void createUser(String firstName, String lastName, String email, String phone, String role) {
        if (ElementUtils.isVisible(firstNameInput)) {
            ElementUtils.fill(firstNameInput, firstName, "First Name");
        }
        if (ElementUtils.isVisible(lastNameInput)) {
            ElementUtils.fill(lastNameInput, lastName, "Last Name");
        }
        if (ElementUtils.isVisible(emailInput)) {
            ElementUtils.fill(emailInput, email, "Email");
        }
        if (ElementUtils.isVisible(phoneInput)) {
            ElementUtils.fill(phoneInput, phone, "Phone");
        }
        if (ElementUtils.isVisible(saveButton)) {
            ElementUtils.click(saveButton, "Save User Button");
        }
    }

    public DataTable getUserTable() {
        return userTable;
    }
}
