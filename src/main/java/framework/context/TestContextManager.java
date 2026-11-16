/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Thread-local registry for TestContext instances, ensuring each parallel test thread accesses its own isolated context.
 * Created: 2026-10-01
 */
package framework.context;

/**
 * Thread-safe holder for the current execution thread's {@link TestContext}.
 * Ensures complete isolation during parallel test execution.
 */
public final class TestContextManager {

    private static final ThreadLocal<TestContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private TestContextManager() {}

    public static TestContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    public static void setContext(TestContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }

    public static boolean hasContext() {
        return CONTEXT_HOLDER.get() != null;
    }
}
