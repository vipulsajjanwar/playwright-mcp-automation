package framework.promptdriven.exec;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import framework.ai.AiConfig;
import framework.ai.AiFailureAnalyzer;
import framework.promptdriven.plan.ActionType;
import framework.promptdriven.plan.PlanStep;
import framework.promptdriven.report.StepResult;
import framework.utils.ElementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Executes UI actions with Playwright, deterministic locator resolution priority,
 * security host restrictions, and bounded self-healing.
 */
public class UiActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(UiActionExecutor.class);

    private final ExecutionContext context;

    public UiActionExecutor(ExecutionContext context) {
        this.context = context;
    }

    public StepResult execute(PlanStep step) {
        ActionType type = step.getActionType();
        long startTime = System.currentTimeMillis();
        VariableResolver resolver = context.getVariableResolver();

        StepResult result = new StepResult(step.getId(), step.getAction(), step.getSource(), "PASS");
        result.setCritical(step.isCritical());

        try {
            switch (type) {
                case NAVIGATE -> {
                    String url = resolver.resolve(step.getStringArg("url"));
                    context.validateHostAllowed(url);
                    result.setExpected("Navigate to " + url);
                    context.getPage().navigate(url);
                    result.setActual("Navigated to " + context.getPage().url());
                }
                case CLICK -> {
                    Map<String, Object> target = step.getTarget();
                    result.setTarget(target.toString());
                    executeWithHealing(step, target, loc -> loc.click(), result);
                }
                case FILL -> {
                    Map<String, Object> target = step.getTarget();
                    String rawVal = step.getStringArg("value");
                    String val = resolver.resolve(rawVal);
                    result.setTarget(target.toString());
                    result.setExpected("Fill value: " + resolver.mask(val));
                    executeWithHealing(step, target, loc -> loc.fill(val), result);
                    result.setActual("Field filled successfully");
                }
                case SELECT -> {
                    Map<String, Object> target = step.getTarget();
                    String opt = resolver.resolve(step.getStringArg("option"));
                    if (opt == null) opt = resolver.resolve(step.getStringArg("value"));
                    if (opt == null) opt = resolver.resolve(step.getStringArg("label"));
                    final String finalOpt = opt;
                    result.setTarget(target.toString());
                    executeWithHealing(step, target, loc -> loc.selectOption(finalOpt), result);
                }
                case CHECK -> {
                    Map<String, Object> target = step.getTarget();
                    result.setTarget(target.toString());
                    executeWithHealing(step, target, loc -> loc.check(), result);
                }
                case UNCHECK -> {
                    Map<String, Object> target = step.getTarget();
                    result.setTarget(target.toString());
                    executeWithHealing(step, target, loc -> loc.uncheck(), result);
                }
                case PRESS -> {
                    String key = resolver.resolve(step.getStringArg("key"));
                    if (!step.getTarget().isEmpty()) {
                        Locator loc = resolveLocator(step.getTarget());
                        loc.press(key);
                    } else {
                        context.getPage().keyboard().press(key);
                    }
                    result.setActual("Pressed key: " + key);
                }
                case HOVER -> {
                    Map<String, Object> target = step.getTarget();
                    result.setTarget(target.toString());
                    executeWithHealing(step, target, loc -> loc.hover(), result);
                }
                case WAIT_FOR -> {
                    if (!step.getTarget().isEmpty()) {
                        Locator loc = resolveLocator(step.getTarget());
                        loc.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
                    } else if (step.getArgs().containsKey("text")) {
                        String txt = resolver.resolve(step.getStringArg("text"));
                        context.getPage().getByText(txt).first().waitFor();
                    } else if (step.getArgs().containsKey("url")) {
                        String url = resolver.resolve(step.getStringArg("url"));
                        context.getPage().waitForURL(url);
                    }
                }
                case UPLOAD -> {
                    Map<String, Object> target = step.getTarget();
                    String file = resolver.resolve(step.getStringArg("file"));
                    executeWithHealing(step, target, loc -> loc.setInputFiles(Paths.get(file)), result);
                }
                case SWITCH_TAB -> {
                    int index = Integer.parseInt(step.getStringArg("index"));
                    List<Page> pages = context.getBrowserContext().pages();
                    if (index >= 0 && index < pages.size()) {
                        context.setPage(pages.get(index));
                    }
                }
                case STORE_TEXT -> {
                    Map<String, Object> target = step.getTarget();
                    String asVar = step.getStringArg("as");
                    Locator loc = resolveLocator(target);
                    String text = loc.innerText().trim();
                    context.setVariable(asVar, text);
                    result.setActual("Stored '" + text + "' as " + asVar);
                }
                case SCREENSHOT -> {
                    String name = step.getStringArg("name");
                    Path path = context.getScreenshotsDir().resolve(name + ".png");
                    context.getPage().screenshot(new Page.ScreenshotOptions().setPath(path));
                    result.setScreenshotPath(path.toString());
                }
                default -> throw new UnsupportedOperationException("Unsupported UI action: " + type);
            }
        } catch (Exception e) {
            result.setStatus("FAIL");
            result.setError(e.getMessage());
            logger.error("Failed UI step {}: {}", step.getId(), e.getMessage());
        } finally {
            result.setDurationMs(System.currentTimeMillis() - startTime);
        }

        return result;
    }

    private void executeWithHealing(PlanStep step, Map<String, Object> target, ConsumerWithException<Locator> action, StepResult result) throws Exception {
        try {
            Locator locator = resolveLocator(target);
            action.accept(locator);
        } catch (Exception primaryEx) {
            logger.warn("Primary locator {} failed for step {}: {}. Initiating bounded self-healing...",
                    target, step.getId(), primaryEx.getMessage());

            // 1. Classification & diagnosis
            AiFailureAnalyzer.analyzeFailure(
                    context.getPromptName(),
                    primaryEx.getMessage(),
                    "",
                    context.getPage() != null ? context.getPage().url() : "",
                    target.toString()
            );

            // 2. Generate up to 2 alternative targets
            List<Map<String, Object>> alternatives = generateAlternativeTargets(target);
            boolean healed = false;

            for (int i = 0; i < Math.min(2, alternatives.size()); i++) {
                Map<String, Object> altTarget = alternatives.get(i);
                try {
                    logger.info("Self-healing attempt {}/2 for step {} using alternative: {}", i + 1, step.getId(), altTarget);
                    Locator altLoc = resolveLocator(altTarget);
                    action.accept(altLoc);
                    healed = true;
                    result.setStatus("HEALED");
                    result.setOldTarget(target.toString());
                    result.setNewTarget(altTarget.toString());
                    logger.info("Self-healing SUCCESS for step {}! Resolved with: {}", step.getId(), altTarget);
                    break;
                } catch (Exception altEx) {
                    logger.debug("Alternative {} failed: {}", altTarget, altEx.getMessage());
                }
            }

            if (!healed) {
                throw primaryEx;
            }
        }
    }

    private List<Map<String, Object>> generateAlternativeTargets(Map<String, Object> original) {
        List<Map<String, Object>> list = new ArrayList<>();

        String text = null;
        if (original.containsKey("name")) text = String.valueOf(original.get("name"));
        else if (original.containsKey("label")) text = String.valueOf(original.get("label"));
        else if (original.containsKey("placeholder")) text = String.valueOf(original.get("placeholder"));
        else if (original.containsKey("text")) text = String.valueOf(original.get("text"));

        if (text != null && !text.isBlank()) {
            // Alternative 1: getByText
            if (!original.containsKey("text")) {
                list.add(Map.of("text", text));
            }
            // Alternative 2: CSS partial contains
            list.add(Map.of("css", "*:has-text('" + text + "')"));
            // Alternative 3: button / input role
            list.add(Map.of("role", "button", "name", text));
        }

        return list;
    }

    public Locator resolveLocator(Map<String, Object> target) {
        Page page = context.getPage();
        if (page == null) {
            throw new IllegalStateException("Playwright Page is null in ExecutionContext");
        }
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("Target locator map is empty");
        }

        VariableResolver resolver = context.getVariableResolver();

        // 1. Role: role -> getByRole(role, name)
        if (target.containsKey("role")) {
            String roleStr = resolver.resolve(String.valueOf(target.get("role"))).toUpperCase(Locale.ROOT);
            AriaRole role = parseAriaRole(roleStr);
            if (role != null) {
                if (target.containsKey("name")) {
                    String name = resolver.resolve(String.valueOf(target.get("name")));
                    return page.getByRole(role, new Page.GetByRoleOptions().setName(name));
                }
                return page.getByRole(role);
            }
        }

        // 2. Label: label -> getByLabel
        if (target.containsKey("label")) {
            String label = resolver.resolve(String.valueOf(target.get("label")));
            return page.getByLabel(label);
        }

        // 3. Placeholder: placeholder -> getByPlaceholder
        if (target.containsKey("placeholder")) {
            String placeholder = resolver.resolve(String.valueOf(target.get("placeholder")));
            return page.getByPlaceholder(placeholder);
        }

        // 4. Text: text -> getByText
        if (target.containsKey("text")) {
            String text = resolver.resolve(String.valueOf(target.get("text")));
            return page.getByText(text).first();
        }

        // 5. TestId: testId -> getByTestId
        if (target.containsKey("testId")) {
            String testId = resolver.resolve(String.valueOf(target.get("testId")));
            return page.getByTestId(testId);
        }

        // 6. CSS: css -> locator(css)
        if (target.containsKey("css")) {
            String css = resolver.resolve(String.valueOf(target.get("css")));
            return page.locator(css);
        }

        // 7. XPath
        if (target.containsKey("xpath")) {
            String xpath = resolver.resolve(String.valueOf(target.get("xpath")));
            return page.locator("xpath=" + xpath);
        }

        throw new IllegalArgumentException("Could not resolve locator from target: " + target);
    }

    private AriaRole parseAriaRole(String roleStr) {
        try {
            return AriaRole.valueOf(roleStr);
        } catch (Exception e) {
            return switch (roleStr.toLowerCase(Locale.ROOT)) {
                case "button" -> AriaRole.BUTTON;
                case "textbox", "input" -> AriaRole.TEXTBOX;
                case "link" -> AriaRole.LINK;
                case "checkbox" -> AriaRole.CHECKBOX;
                case "radio" -> AriaRole.RADIO;
                case "combobox", "select" -> AriaRole.COMBOBOX;
                case "table" -> AriaRole.TABLE;
                case "heading" -> AriaRole.HEADING;
                default -> null;
            };
        }
    }

    @FunctionalInterface
    public interface ConsumerWithException<T> {
        void accept(T t) throws Exception;
    }
}
