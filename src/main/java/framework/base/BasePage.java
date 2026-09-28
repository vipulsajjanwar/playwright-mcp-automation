package framework.base;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Path;
import java.util.List;

import framework.reporting.ReportUtils;
import framework.utils.PageUtils;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasePage {

    protected final Page page;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected BasePage(Page page) {
        if (page == null) {
            throw new IllegalArgumentException("Page cannot be null");
        }
        this.page = page;
    }

    // ========================================================================
    // PAGE
    // ========================================================================

    public Page getPage() {
        return page;
    }

    public BrowserContext getContext() {
        return page.context();
    }

    public Browser getBrowser() {
        return page.context().browser();
    }

    public String getTitle() {
        return page.title();
    }

    public String getUrl() {
        return page.url();
    }

    public String getContent() {
        return page.content();
    }

    // ========================================================================
    // NAVIGATION
    // ========================================================================

    public void navigate(String url) {
        page.navigate(url, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    public void navigate(String url, int timeoutMs) {
        page.navigate(url, new Page.NavigateOptions()
                .setTimeout(timeoutMs)
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    public void reload() {
        page.reload(new Page.ReloadOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    public void reload(int timeoutMs) {
        page.reload(new Page.ReloadOptions()
                .setTimeout(timeoutMs)
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    public void goBack() {
        page.goBack(new Page.GoBackOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    public void goForward() {
        page.goForward(new Page.GoForwardOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    public void waitForLoadState() {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public void waitForLoadState(LoadState state) {
        page.waitForLoadState(state);
    }

    public void waitForLoadState(LoadState state, int timeoutMs) {
        page.waitForLoadState(state, new Page.WaitForLoadStateOptions()
                .setTimeout(timeoutMs));
    }

    // ========================================================================
    // LOCATORS
    // ========================================================================

    protected Locator locator(String selector) {
        return page.locator(selector);
    }

    protected Locator locator(String selector, String value) {
        return page.locator(String.format(selector, value));
    }

    protected Locator getByRole(AriaRole role, String name) {
        return page.getByRole(role, new Page.GetByRoleOptions().setName(name));
    }

    protected Locator getByRole(AriaRole role, String name, boolean exact) {
        return page.getByRole(role, new Page.GetByRoleOptions()
                .setName(name)
                .setExact(exact));
    }

    protected Locator getByLabel(String label) {
        return page.getByLabel(label);
    }

    protected Locator getByLabel(String label, boolean exact) {
        return page.getByLabel(label, new Page.GetByLabelOptions().setExact(exact));
    }

    protected Locator getByPlaceholder(String placeholder) {
        return page.getByPlaceholder(placeholder);
    }

    protected Locator getByPlaceholder(String placeholder, boolean exact) {
        return page.getByPlaceholder(placeholder,
                new Page.GetByPlaceholderOptions().setExact(exact));
    }

    protected Locator getByText(String text) {
        return page.getByText(text);
    }

    protected Locator getByText(String text, boolean exact) {
        return page.getByText(text, new Page.GetByTextOptions().setExact(exact));
    }

    protected Locator getByAltText(String altText) {
        return page.getByAltText(altText);
    }

    protected Locator getByTitle(String title) {
        return page.getByTitle(title);
    }

    protected Locator getByTestId(String testId) {
        return page.getByTestId(testId);
    }

    /** Note: Playwright's getByTestId has no "exact" option; the flag is ignored. */
    protected Locator getByTestId(String testId, boolean exact) {
        return page.getByTestId(testId);
    }

    protected Locator getById(String id) {
        return page.locator("#" + id);
    }

    protected Locator getByName(String name) {
        return page.locator("[name='" + name + "']");
    }

    protected Locator getByCss(String css) {
        return page.locator(css);
    }

    protected Locator getByXpath(String xpath) {
        return page.locator("xpath=" + xpath);
    }

    // ========================================================================
    // LOCATOR OPERATIONS
    // ========================================================================

    protected Locator first(Locator locator) {
        return locator.first();
    }

    protected Locator last(Locator locator) {
        return locator.last();
    }

    protected Locator nth(Locator locator, int index) {
        return locator.nth(index);
    }

    protected Locator filter(Locator locator, String text) {
        return locator.filter(new Locator.FilterOptions().setHasText(text));
    }

    protected Locator filterByText(Locator locator, String text) {
        return locator.filter(new Locator.FilterOptions().setHasText(text));
    }

    protected Locator filterByLocator(Locator locator, Locator child) {
        return locator.filter(new Locator.FilterOptions().setHas(child));
    }

    protected int count(Locator locator) {
        return locator.count();
    }

    // ========================================================================
    // VISIBILITY / STATE
    // ========================================================================

    protected boolean isVisible(Locator locator) {
        try {
            PageUtils.highlightElement(locator);
            return locator.isVisible();
        } catch (Exception e) {
            logger.warn("Exception while checking visibility: {} {}", e.getMessage(), locator);
            return false;
        }
    }

    protected boolean isVisible(Locator locator, int timeoutMs) {
        try {
            PageUtils.highlightElement(locator);
            locator.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeoutMs));
            return true;
        } catch (TimeoutError e) {
            logger.warn("Timeout while waiting for visibility: {} {}", e.getMessage(), locator);
            return false;
        }
    }

    protected boolean isHidden(Locator locator) {
        try {
            return locator.isHidden();
        } catch (Exception e) {
            logger.warn("Exception while checking hidden state: {} {}", e.getMessage(), locator);
            return false;
        }
    }

    protected boolean isEnabled(Locator locator) {
        try {
            return locator.isEnabled();
        } catch (Exception e) {
            logger.warn("Exception while checking enabled state: {} {}", e.getMessage(), locator);
            return false;
        }
    }

    protected boolean isDisabled(Locator locator) {
        try {
            return locator.isDisabled();
        } catch (Exception e) {
            logger.warn("Exception while checking disabled state: {} {}", e.getMessage(), locator);
            return false;
        }
    }

    protected boolean isChecked(Locator locator) {
        try {
            return locator.isChecked();
        } catch (Exception e) {
            logger.warn("Exception while checking checked state: {} {}", e.getMessage(), locator);
            return false;
        }
    }

    protected boolean isEditable(Locator locator) {
        try {
            return locator.isEditable();
        } catch (Exception e) {
            logger.warn("Exception while checking editable state: {} {}", e.getMessage(), locator);
            return false;
        }
    }

    protected boolean isAttached(Locator locator) {
        try {
            return locator.count() > 0;
        } catch (Exception e) {
            logger.warn("Exception while checking attached state: {} {}", e.getMessage(), locator);
            return false;
        }
    }

    // ========================================================================
    // WAIT METHODS
    // ========================================================================

    protected void waitForVisible(Locator locator) {
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));
    }

    protected void waitForVisible(Locator locator, int timeoutMs) {
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeoutMs));
    }

    protected void waitForHidden(Locator locator) {
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN));
    }

    protected void waitForHidden(Locator locator, int timeoutMs) {
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(timeoutMs));
    }

    protected void waitForAttached(Locator locator) {
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED));
    }

    protected void waitForDetached(Locator locator) {
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.DETACHED));
    }

    protected void waitForEnabled(Locator locator, int timeoutMs) {
        waitForCondition(
                () -> isEnabled(locator),
                timeoutMs,
                "Element did not become enabled within " + timeoutMs + " ms");
    }

    protected void waitForCondition(
            BooleanSupplier condition,
            int timeoutMs,
            String message) {

        long endTime = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return;
            }
            page.waitForTimeout(100);
        }

        throw new TimeoutError(message);
    }

    // ========================================================================
    // CLICK
    // ========================================================================

    protected void click(Locator locator) {
        locator.click();
    }

    protected void click(Locator locator, String elementName) {
        waitForVisible(locator);
        PageUtils.highlightElement(locator);
        ReportUtils.logInfo("Clicking on " + elementName);
        locator.click();
        logger.debug("Clicked element: {}", elementName);
        ReportUtils.logPass("Clicked element: " + elementName);
    }

    protected void click(Locator locator, String elementName, int timeoutMs) {
        ReportUtils.logInfo("Clicking on " + elementName);
        PageUtils.highlightElement(locator);
        locator.click(new Locator.ClickOptions().setTimeout(timeoutMs));
        logger.debug("Clicked element: {}", elementName);
        ReportUtils.logPass("Clicked element: " + elementName);
    }

    protected void doubleClick(Locator locator) {
        locator.dblclick();
    }

    protected void rightClick(Locator locator) {
        locator.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
    }

    protected void forceClick(Locator locator) {
        locator.click(new Locator.ClickOptions().setForce(true));
    }

    protected void clickIfVisible(Locator locator) {
        if (isVisible(locator)) {
            locator.click();
        }
    }

    // ========================================================================
    // INPUT / TEXT
    // ========================================================================

    protected void fill(Locator locator, String value) {
        locator.fill(value);
    }

    protected void fill(Locator locator, String value, String fieldName) {
        waitForVisible(locator);
        PageUtils.highlightElement(locator);
        locator.fill(value);
        logger.debug("Filled field: {}", fieldName);
        ReportUtils.logPass("Filled field: " + fieldName + " with value: " + value);
    }

    protected void clear(Locator locator) {
        locator.fill("");
    }

    protected void type(Locator locator, String text) {
        locator.pressSequentially(text);
    }

    protected void press(Locator locator, String key) {
        locator.press(key);
    }

    protected void press(String key) {
        page.keyboard().press(key);
    }

    protected String inputValue(Locator locator) {
        return locator.inputValue();
    }

    protected String text(Locator locator) {
        return locator.innerText().trim();
    }

    protected String textContent(Locator locator) {
        String value = locator.textContent();
        return value == null ? "" : value.trim();
    }

    protected String attribute(Locator locator, String attribute) {
        return locator.getAttribute(attribute);
    }

    protected String value(Locator locator) {
        return locator.inputValue();
    }

    // ========================================================================
    // SELECT
    // ========================================================================

    protected void selectOption(Locator locator, String value) {
        locator.selectOption(value);
    }

    protected void selectOptionByLabel(Locator locator, String label) {
        locator.selectOption(new SelectOption().setLabel(label));
    }

    protected void selectOptionByIndex(Locator locator, int index) {
        locator.selectOption(new SelectOption().setIndex(index));
    }

    protected String selectedValue(Locator locator) {
        return locator.inputValue();
    }

    // ========================================================================
    // CHECKBOX / RADIO
    // ========================================================================

    protected void check(Locator locator) {
        locator.check();
    }

    protected void uncheck(Locator locator) {
        locator.uncheck();
    }

    protected void checkIfRequired(Locator locator) {
        if (!locator.isChecked()) {
            locator.check();
        }
    }

    protected void uncheckIfRequired(Locator locator) {
        if (locator.isChecked()) {
            locator.uncheck();
        }
    }

    protected void setChecked(Locator locator, boolean checked) {
        locator.setChecked(checked);
    }

    // ========================================================================
    // HOVER / FOCUS
    // ========================================================================

    protected void hover(Locator locator) {
        locator.hover();
    }

    protected void hover(Locator locator, String elementName) {
        locator.hover();
        logger.debug("Hovered over: {}", elementName);
    }

    protected void focus(Locator locator) {
        locator.focus();
    }

    protected void blur(Locator locator) {
        locator.evaluate("element => element.blur()");
    }

    // ========================================================================
    // ATTRIBUTES
    // ========================================================================

    protected String getAttribute(Locator locator, String attributeName) {
        return locator.getAttribute(attributeName);
    }

    protected String getAriaLabel(Locator locator) {
        return locator.getAttribute("aria-label");
    }

    protected String getClass(Locator locator) {
        return locator.getAttribute("class");
    }

    protected String getId(Locator locator) {
        return locator.getAttribute("id");
    }

    protected String getRole(Locator locator) {
        return locator.getAttribute("role");
    }

    // ========================================================================
    // ASSERTIONS
    // ========================================================================

    protected void assertVisible(Locator locator) {
        assertThat(locator).isVisible();
    }

    protected void assertHidden(Locator locator) {
        assertThat(locator).isHidden();
    }

    protected void assertEnabled(Locator locator) {
        assertThat(locator).isEnabled();
    }

    protected void assertDisabled(Locator locator) {
        assertThat(locator).isDisabled();
    }

    protected void assertChecked(Locator locator) {
        assertThat(locator).isChecked();
    }

    protected void assertUnchecked(Locator locator) {
        assertThat(locator).not().isChecked();
    }

    protected void assertText(Locator locator, String expectedText) {
        assertThat(locator).hasText(expectedText);
    }

    protected void assertContainsText(Locator locator, String expectedText) {
        assertThat(locator).containsText(expectedText);
    }

    protected void assertValue(Locator locator, String expectedValue) {
        assertThat(locator).hasValue(expectedValue);
    }

    protected void assertAttribute(Locator locator, String attribute, String expectedValue) {
        assertThat(locator).hasAttribute(attribute, expectedValue);
    }

    protected void assertCount(Locator locator, int expectedCount) {
        assertThat(locator).hasCount(expectedCount);
    }

    // ========================================================================
    // SCREENSHOT
    // ========================================================================

    protected byte[] screenshot() {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }

    protected byte[] screenshot(Path path) {
        return page.screenshot(new Page.ScreenshotOptions()
                .setPath(path)
                .setFullPage(true));
    }

    protected byte[] elementScreenshot(Locator locator, Path path) {
        return locator.screenshot(new Locator.ScreenshotOptions().setPath(path));
    }

    // ========================================================================
    // JAVASCRIPT
    // ========================================================================

    protected Object evaluate(String script) {
        return page.evaluate(script);
    }

    protected Object evaluate(String script, Object arg) {
        return page.evaluate(script, arg);
    }

    protected Object evaluate(Locator locator, String script) {
        return locator.evaluate(script);
    }

    protected Object evaluate(Locator locator, String script, Object arg) {
        return locator.evaluate(script, arg);
    }

    // ========================================================================
    // SCROLL
    // ========================================================================

    protected void scrollIntoView(Locator locator) {
        locator.scrollIntoViewIfNeeded();
    }

    protected void scrollToTop() {
        page.evaluate("() => window.scrollTo(0, 0)");
    }

    protected void scrollToBottom() {
        page.evaluate("() => window.scrollTo(0, document.body.scrollHeight)");
    }

    // ========================================================================
    // MOUSE
    // ========================================================================

    protected void mouseClick(double x, double y) {
        page.mouse().click(x, y);
    }

    protected void mouseMove(double x, double y) {
        page.mouse().move(x, y);
    }

    protected void mouseDown() {
        page.mouse().down();
    }

    protected void mouseUp() {
        page.mouse().up();
    }

    // ========================================================================
    // KEYBOARD
    // ========================================================================

    protected void keyPress(String key) {
        page.keyboard().press(key);
    }

    protected void keyDown(String key) {
        page.keyboard().down(key);
    }

    protected void keyUp(String key) {
        page.keyboard().up(key);
    }

    protected void keyboardType(String text) {
        page.keyboard().type(text);
    }

    // ========================================================================
    // DIALOG
    // ========================================================================

    protected void acceptNextDialog() {
        page.onceDialog(Dialog::accept);
    }

    protected void dismissNextDialog() {
        page.onceDialog(Dialog::dismiss);
    }

    protected void acceptDialogWithText(String expectedMessage) {
        page.onceDialog(dialog -> {
            logger.info("Dialog message: {}", dialog.message());

            if (expectedMessage != null && !dialog.message().contains(expectedMessage)) {
                throw new AssertionError("Unexpected dialog message: " + dialog.message());
            }

            dialog.accept();
        });
    }

    // ========================================================================
    // NEW TAB / POPUP
    // ========================================================================

    protected Page waitForNewPage(Runnable action) {
        return page.waitForPopup(action);
    }

    protected Page waitForPopup(Runnable action) {
        return page.waitForPopup(action);
    }

    // ========================================================================
    // FRAME
    // ========================================================================

    protected FrameLocator frame(String selector) {
        return page.frameLocator(selector);
    }

    protected FrameLocator frame(Locator iframe) {
        return page.frameLocator(iframe.getAttribute("src"));
    }

    // ========================================================================
    // COOKIES
    // ========================================================================

    protected List<Cookie> cookies() {
        return page.context().cookies();
    }

    protected void addCookie(Cookie cookie) {
        page.context().addCookies(List.of(cookie));
    }

    protected void clearCookies() {
        page.context().clearCookies();
    }

    // ========================================================================
    // STORAGE
    // ========================================================================

    protected Object localStorage(String key) {
        return page.evaluate("key => window.localStorage.getItem(key)", key);
    }

    protected void setLocalStorage(String key, String value) {
        page.evaluate(
                "([key, value]) => window.localStorage.setItem(key, value)",
                List.of(key, value));
    }

    protected void removeLocalStorage(String key) {
        page.evaluate("key => window.localStorage.removeItem(key)", key);
    }

    protected void clearLocalStorage() {
        page.evaluate("() => window.localStorage.clear()");
    }

    // ========================================================================
    // TAB / PAGE INFORMATION
    // ========================================================================

    protected List<Page> pages() {
        return page.context().pages();
    }

    protected int pageCount() {
        return page.context().pages().size();
    }

    protected boolean hasMultiplePages() {
        return pageCount() > 1;
    }

    // ========================================================================
    // DOWNLOAD / UPLOAD / DRAG & DROP
    // ========================================================================

    protected Download waitForDownload(Runnable action) {
        return page.waitForDownload(action);
    }

    protected void uploadFile(Locator locator, Path file) {
        locator.setInputFiles(file);
    }

    protected void uploadFiles(Locator locator, Path... files) {
        locator.setInputFiles(files);
    }

    protected void dragAndDrop(Locator source, Locator target) {
        source.dragTo(target);
    }

    protected String tooltip(Locator locator) {
        return locator.getAttribute("title");
    }

    // ========================================================================
    // WAIT FOR URL / RESPONSE / REQUEST / NETWORK
    // ========================================================================

    protected void waitForUrl(String url) {
        page.waitForURL(url);
    }

    protected void waitForUrl(String url, int timeoutMs) {
        page.waitForURL(url, new Page.WaitForURLOptions().setTimeout(timeoutMs));
    }

    protected void waitForUrlContains(String text) {
        page.waitForURL("**/*" + text + "*");
    }

    protected Response waitForResponse(String url, Runnable action) {
        return page.waitForResponse(url, action);
    }

    protected Request waitForRequest(String url, Runnable action) {
        return page.waitForRequest(url, action);
    }

    protected void waitForNetworkIdle() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    // ========================================================================
    // TIMEOUT
    // ========================================================================

    protected void setDefaultTimeout(int timeoutMs) {
        page.setDefaultTimeout(timeoutMs);
    }

    protected void setNavigationTimeout(int timeoutMs) {
        page.setDefaultNavigationTimeout(timeoutMs);
    }

    protected void pause(int milliseconds) {
        page.waitForTimeout(milliseconds);
    }

    // ========================================================================
    // COMMON VALIDATIONS
    // ========================================================================

    protected void validateElementVisible(Locator locator, String elementName) {
        if (!isVisible(locator, 10_000)) {
            throw new AssertionError(elementName + " is not visible.");
        }
        logger.info("{} is visible", elementName);
        ReportUtils.logPass(elementName + " is visible");
    }

    protected void validateText(Locator locator, String expected, String elementName) {
        String actual = text(locator);
        validateText(actual, expected, elementName, "equalsIgnoreCase");
    }

    protected void validateContainsText(Locator locator, String expected, String elementName) {
        String actual = text(locator);

        validateText(actual, expected, elementName, "contains");
    }

    protected void validateAttribute(
            Locator locator,
            String attribute,
            String expected,
            String elementName) {

        String actual = getAttribute(locator, attribute);

        if (!expected.equals(actual)) {
            throw new AssertionError(
                    elementName + " attribute [" + attribute + "] mismatch. Expected: ["
                            + expected + "] Actual: [" + actual + "]");
        }
    }

    protected void validateTitle(String expected, String elementName, String type) {
        String actual = page.title();

        validateText(actual, expected, elementName, type);
    }
    @Step("Validate text of {elementName} is {type} [{expected}]")
    protected void validateText(String actual, String expected, String elementName, String type) {
        try {
            ReportUtils.logInfo(elementName + " actual text: [" + actual + "]");
            ReportUtils.logInfo(elementName + " expected text: [" + expected + "]");
            boolean isMatch;

            if(type.equals("contains")){
                isMatch = actual.contains(expected);
            }else if(type.equals("equals")){
                isMatch = actual.equals(expected);
            }else{
                isMatch = actual.equalsIgnoreCase(expected);
            }

            if (!isMatch) {
                ReportUtils.logFail(elementName + " text mismatch. Expected: [" + expected + "] Actual: [" + actual + "]", "validateText", page);
            }else{
                logger.info("{} text matches. Expected: [{}] Actual: [{}]", elementName, expected, actual);
                ReportUtils.logPass(elementName + " text matches. Expected: [" + expected + "] Actual: [" + actual + "]");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========================================================================
    // FUNCTIONAL INTERFACE
    // ========================================================================

    @FunctionalInterface
    protected interface BooleanSupplier {
        boolean getAsBoolean();
    }

}
