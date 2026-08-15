package framework.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.utils.ElementUtils;

public class Header {

    private final Page page;
    private final Locator userMenu;
    private final Locator logoutButton;
    private final Locator searchInput;

    public Header(Page page) {
        this.page = page;
        this.userMenu = page.locator("#user-menu, .user-profile-menu");
        this.logoutButton = page.locator("#logout-btn, button:has-text('Logout')");
        this.searchInput = page.locator("input[type='search'], #global-search");
    }

    public void openUserMenu() {
        ElementUtils.click(userMenu, "User Menu");
    }

    public void logout() {
        openUserMenu();
        ElementUtils.click(logoutButton, "Logout Button");
    }

    public void search(String query) {
        ElementUtils.fill(searchInput, query, "Global Search Input");
        searchInput.press("Enter");
    }
}
