package framework.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.utils.ElementUtils;

public class Sidebar {

    private final Page page;

    public Sidebar(Page page) {
        this.page = page;
    }

    public void navigateToMenu(String menuName) {
        Locator menuItem = page.locator(".sidebar-menu, nav").getByText(menuName);
        ElementUtils.click(menuItem, "Sidebar Menu Item: " + menuName);
    }
}
