# Enterprise Selenium Java to Playwright Java Migration Guide

Modernizing an enterprise test automation suite from Selenium WebDriver to Playwright Java significantly improves test execution speed, eliminates flaky tests caused by timing issues, and simplifies browser dependency management.

---

## 1. Core Concept Mapping

| Selenium Concept | Playwright Equivalent | Major Improvement |
|---|---|---|
| `WebDriver driver` | `Page page` | Lightweight tab instance; automatic waiting. |
| `WebElement element` | `Locator locator` | Lazy evaluation; auto-retries on action. |
| `By.id()`, `By.xpath()` | `getByRole()`, `getByLabel()`, `getByPlaceholder()` | Accessible locators resilient to layout refactoring. |
| `WebDriverWait` / `Thread.sleep()` | Built-in Auto-Waiting | Automatically waits for visibility, actionability, and stability. |
| `Actions actions` | Native methods (`click()`, `hover()`, `dragTo()`) | Reliable user event simulation without OS mouse hooks. |
| `JavascriptExecutor` | `page.evaluate()` | Direct execution with typed return values. |
| `PageFactory.initElements()` | Direct `Locator` assignment in POM constructor | Eliminates stale element exceptions (`StaleElementReferenceException`). |
| `TakesScreenshot` | `page.screenshot()` | Full page, element-level, and trace integration. |

---

## 2. API Code Equivalents

### A. Element Locators

```java
// Selenium (Discouraged / Brittle)
driver.findElement(By.xpath("//input[@id='username']"));
driver.findElement(By.id("login-button"));

// Playwright (Preferred / Resilience-First)
page.getByLabel("Username");
page.getByPlaceholder("Username");
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"));
```

### B. User Actions

```java
// Selenium
element.clear();
element.sendKeys("admin_user");
button.click();

// Playwright
locator.fill("admin_user"); // Automatically clears and fills
locator.click();
```

### C. Dropdowns & Select Elements

```java
// Selenium
Select select = new Select(driver.findElement(By.id("role")));
select.selectByValue("ADMIN");

// Playwright
page.locator("#role").selectOption("ADMIN");
```

### D. File Uploads

```java
// Selenium
driver.findElement(By.id("file-input")).sendKeys("/path/to/file.txt");

// Playwright
page.locator("#file-input").setInputFiles(Paths.get("/path/to/file.txt"));
```

---

## 3. Architectural Anti-Patterns to Avoid During Migration

1. **Do NOT port `Thread.sleep()` or excessive explicit waits**: Playwright automatically waits for elements to be attached, visible, stable, and actionable before every click or fill.
2. **Do NOT use static `WebDriver` or static `Page` instances**: Always use `ThreadLocal<BrowserContext>` and `ThreadLocal<Page>` per test thread.
3. **Do NOT use `PageFactory.initElements()`**: Playwright `Locator` objects are lightweight declarations created on the fly and do not throw `StaleElementReferenceException`.
4. **Do NOT create a fresh browser process for every test**: Share a single `Browser` instance per JVM process, and instantiate lightweight isolated `BrowserContext` objects per parallel test.

---

## 4. Parallel Execution Architecture

```text
Selenium Architecture (Heavyweight):
[JVM] ──> WebDriver Process 1 (Chrome.exe)
      ──> WebDriver Process 2 (Chrome.exe)
      ──> WebDriver Process 3 (Chrome.exe)

Playwright Architecture (Modern & Lightweight):
[JVM] ──> Shared Playwright Browser Process
          ├── Thread 1 ──> BrowserContext 1 (Isolated Storage/Cookies) ──> Page
          ├── Thread 2 ──> BrowserContext 2 (Isolated Storage/Cookies) ──> Page
          └── Thread 3 ──> BrowserContext 3 (Isolated Storage/Cookies) ──> Page
```
