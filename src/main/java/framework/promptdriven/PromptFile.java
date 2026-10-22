package framework.promptdriven;

import java.nio.file.Path;
import java.util.*;

/**
 * Encapsulates a parsed prompt test file, including metadata, execution settings,
 * step instructions, and referenced variable placeholders.
 */
public class PromptFile {

    private final Path path;
    private final String name;
    private final String testName;
    private final String type; // "ui", "api", "mixed"
    private final String mode; // "soft", "fail-fast"
    private final String lock; // lock name or null
    private final List<String> tags;
    private final List<String> rawStepLines;
    private final Map<String, String> headers;
    private final Set<String> variableNames;
    private final String rawContent;

    public PromptFile(Builder builder) {
        this.path = builder.path;
        this.name = builder.name;
        this.testName = builder.testName;
        this.type = builder.type != null ? builder.type.toLowerCase(Locale.ROOT) : "ui";
        this.mode = builder.mode != null ? builder.mode.toLowerCase(Locale.ROOT) : "soft";
        this.lock = builder.lock;
        this.tags = builder.tags != null ? Collections.unmodifiableList(builder.tags) : Collections.emptyList();
        this.rawStepLines = Collections.unmodifiableList(builder.rawStepLines != null ? builder.rawStepLines : Collections.emptyList());
        this.headers = Collections.unmodifiableMap(builder.headers != null ? builder.headers : Collections.emptyMap());
        this.variableNames = Collections.unmodifiableSet(builder.variableNames != null ? builder.variableNames : Collections.emptySet());
        this.rawContent = builder.rawContent != null ? builder.rawContent : "";
    }

    public Path getPath() { return path; }
    public String getName() { return name; }
    public String getTestName() { return testName != null && !testName.isBlank() ? testName : name; }
    public String getType() { return type; }
    public String getMode() { return mode; }
    public boolean isFailFast() { return "fail-fast".equalsIgnoreCase(mode); }
    public String getLock() { return lock; }
    public List<String> getTags() { return tags; }
    public List<String> getRawStepLines() { return rawStepLines; }
    public Map<String, String> getHeaders() { return headers; }
    public Set<String> getVariableNames() { return variableNames; }
    public String getRawContent() { return rawContent; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Path path;
        private String name;
        private String testName;
        private String type = "ui";
        private String mode = "soft";
        private String lock;
        private List<String> tags = new ArrayList<>();
        private List<String> rawStepLines = new ArrayList<>();
        private Map<String, String> headers = new LinkedHashMap<>();
        private Set<String> variableNames = new LinkedHashSet<>();
        private String rawContent = "";

        public Builder path(Path path) { this.path = path; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder testName(String testName) { this.testName = testName; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder mode(String mode) { this.mode = mode; return this; }
        public Builder lock(String lock) { this.lock = lock; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }
        public Builder rawStepLines(List<String> rawStepLines) { this.rawStepLines = rawStepLines; return this; }
        public Builder headers(Map<String, String> headers) { this.headers = headers; return this; }
        public Builder variableNames(Set<String> variableNames) { this.variableNames = variableNames; return this; }
        public Builder rawContent(String rawContent) { this.rawContent = rawContent; return this; }

        public PromptFile build() {
            return new PromptFile(this);
        }
    }
}
