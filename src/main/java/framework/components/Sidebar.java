/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Reusable Page Component for sidebar navigation menus, handling expand, collapse, and menu item selection.
 * Created: 2026-08-15
 */
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
