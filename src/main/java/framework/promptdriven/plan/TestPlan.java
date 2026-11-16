/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Compiled, executable representation of a prompt-driven test, containing ordered PlanStep instances and metadata.
 * Created: 2026-10-22
 */
package framework.promptdriven.plan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a validated, deterministic JSON test plan.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestPlan {

    @JsonProperty("version")
    private String version = "1.0";

    @JsonProperty("promptHash")
    private String promptHash;

    @JsonProperty("generatedAt")
    private String generatedAt;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("model")
    private String model;

    @JsonProperty("steps")
    private List<PlanStep> steps = new ArrayList<>();

    public TestPlan() {}

    public TestPlan(List<PlanStep> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getPromptHash() { return promptHash; }
    public void setPromptHash(String promptHash) { this.promptHash = promptHash; }

    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<PlanStep> getSteps() { return steps != null ? steps : new ArrayList<>(); }
    public void setSteps(List<PlanStep> steps) { this.steps = steps; }
}
