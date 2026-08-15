package framework.base;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import framework.utils.ElementUtils;
import framework.utils.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasePage {

    protected final Page page;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public BasePage(Page page) {
        this.page = page;
    }

    public Page getPage() {
        return page;
    }

    public String getTitle() {
        return PageUtils.getTitle(page);
    }

    public String getUrl() {
        return PageUtils.getUrl(page);
    }

    protected Locator getByRole(AriaRole role, String name) {
        return page.getByRole(role, new Page.GetByRoleOptions().setName(name));
    }

    protected Locator getByLabel(String label) {
        return page.getByLabel(label);
    }

    protected Locator getByPlaceholder(String placeholder) {
        return page.getByPlaceholder(placeholder);
    }

    protected Locator getByText(String text) {
        return page.getByText(text);
    }

    protected Locator locator(String selector) {
        return page.locator(selector);
    }
}
