/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Immutable value object representing a single executable step in a compiled test plan.
 * Created: 2026-10-22
 */
package framework.promptdriven.plan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * A discrete executable step within a {@link TestPlan}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanStep {

    @JsonProperty("id")
    private int id;

    @JsonProperty("action")
    private String action;

    @JsonProperty("args")
    private Map<String, Object> args = Collections.emptyMap();

    @JsonProperty("source")
    private String source;

    @JsonProperty("critical")
    private boolean critical = true;

    public PlanStep() {}

    public PlanStep(int id, String action, Map<String, Object> args, String source, boolean critical) {
        this.id = id;
        this.action = action;
        this.args = args != null ? args : Collections.emptyMap();
        this.source = source;
        this.critical = critical;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public ActionType getActionType() {
        return ActionType.fromString(action);
    }

    public Map<String, Object> getArgs() { return args != null ? args : Collections.emptyMap(); }
    public void setArgs(Map<String, Object> args) { this.args = args; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isCritical() { return critical; }
    public void setCritical(boolean critical) { this.critical = critical; }

    public Object getArg(String key) {
        return args != null ? args.get(key) : null;
    }

    public String getStringArg(String key) {
        Object val = getArg(key);
        return val != null ? String.valueOf(val) : null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTarget() {
        Object t = getArg("target");
        if (t instanceof Map) {
            return (Map<String, Object>) t;
        }
        return Collections.emptyMap();
    }
}
