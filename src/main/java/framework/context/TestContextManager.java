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
