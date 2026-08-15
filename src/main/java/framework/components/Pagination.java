package framework.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.utils.ElementUtils;

public class Pagination {

    private final Page page;
    private final Locator nextButton;
    private final Locator prevButton;

    public Pagination(Page page) {
        this.page = page;
        this.nextButton = page.locator(".pagination .next, button:has-text('Next')");
        this.prevButton = page.locator(".pagination .prev, button:has-text('Previous')");
    }

    public void clickNext() {
        ElementUtils.click(nextButton, "Pagination Next");
    }

    public void clickPrevious() {
        ElementUtils.click(prevButton, "Pagination Previous");
    }
}
