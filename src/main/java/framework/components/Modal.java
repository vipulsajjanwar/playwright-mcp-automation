package framework.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.utils.ElementUtils;
import framework.utils.WaitUtils;

public class Modal {

    private final Page page;
    private final Locator modalContainer;
    private final Locator title;
    private final Locator confirmButton;
    private final Locator cancelButton;

    public Modal(Page page, String modalSelector) {
        this.page = page;
        this.modalContainer = page.locator(modalSelector);
        this.title = modalContainer.locator(".modal-title, h2, h3");
        this.confirmButton = modalContainer.locator("button.btn-primary, button:has-text('Confirm'), button:has-text('OK')");
        this.cancelButton = modalContainer.locator("button.btn-secondary, button:has-text('Cancel')");
    }

    public void waitForModalToAppear() {
        WaitUtils.waitForVisible(modalContainer);
    }

    public String getTitleText() {
        return ElementUtils.getText(title, "Modal Title");
    }

    public void confirm() {
        ElementUtils.click(confirmButton, "Modal Confirm Button");
    }

    public void cancel() {
        ElementUtils.click(cancelButton, "Modal Cancel Button");
    }
}
