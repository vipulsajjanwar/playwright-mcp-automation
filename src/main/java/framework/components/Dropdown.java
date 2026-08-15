package framework.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.utils.ElementUtils;

public class Dropdown {

    private final Locator dropdownLocator;
    private final String name;

    public Dropdown(Page page, String selector, String name) {
        this.dropdownLocator = page.locator(selector);
        this.name = name;
    }

    public Dropdown(Locator locator, String name) {
        this.dropdownLocator = locator;
        this.name = name;
    }

    public void selectByValue(String value) {
        ElementUtils.selectByValue(dropdownLocator, value, name);
    }

    public void selectByLabel(String label) {
        ElementUtils.selectByLabel(dropdownLocator, label, name);
    }
}
