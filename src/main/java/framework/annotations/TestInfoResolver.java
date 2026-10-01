package framework.annotations;

import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TestInfoResolver {
    public static final String UNKNOWN = "UNKNOWN";

    public record Resolved(
            String id,
            String author,
            String description,
            TestInfo.Severity severity,
            String module,
            List<String> links,
            List<String> tags,
            boolean annotated) {
    }

    private TestInfoResolver() {
    }

    public static Resolved resolve(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        Class<?> clazz = result.getMethod().getRealClass();

        TestInfo m = method.getAnnotation(TestInfo.class);
        TestInfo c = clazz.getAnnotation(TestInfo.class);

        String fallbackId = clazz.getSimpleName() + "#" + method.getName();

        String id = m != null && !m.id().isBlank() ? m.id() : fallbackId;
        String author = firstNonBlank(m == null ? null : m.author(),
                c == null ? null : c.author(), UNKNOWN);
        String description = firstNonBlank(m == null ? null : m.description(),
                c == null ? null : c.description(), result.getMethod().getDescription());
        String module = firstNonBlank(m == null ? null : m.module(),
                c == null ? null : c.module(), UNKNOWN);

        TestInfo.Severity severity = TestInfo.Severity.UNSET;
        if (m != null && m.severity() != TestInfo.Severity.UNSET) {
            severity = m.severity();
        } else if (c != null) {
            severity = c.severity();
        }

        List<String> links = m != null && m.links().length > 0
                ? List.of(m.links())
                : c == null ? List.of() : List.of(c.links());

        Set<String> tags = new LinkedHashSet<>();
        if (c != null) tags.addAll(List.of(c.tags()));
        if (m != null) tags.addAll(List.of(m.tags()));

        return new Resolved(id, author, description, severity, module,
                links, new ArrayList<>(tags), m != null || c != null);
    }

    private static String firstNonBlank(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return fallback;
    }
}
