package framework.promptdriven.plan;

import framework.ai.AiConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates a {@link TestPlan} against the closed action vocabulary, required arguments,
 * ambiguity constraints, and configured plan safety limits.
 */
public class PlanValidator {

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : List.of();
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }

    public static ValidationResult validate(TestPlan plan) {
        List<String> errors = new ArrayList<>();

        if (plan == null) {
            return new ValidationResult(false, List.of("Plan is null"));
        }

        List<PlanStep> steps = plan.getSteps();
        if (steps == null || steps.isEmpty()) {
            return new ValidationResult(false, List.of("Plan contains no steps"));
        }

        int maxSteps = AiConfig.getMaxSteps();
        if (steps.size() > maxSteps) {
            errors.add(String.format("Plan exceeds maximum allowed steps: %d > %d", steps.size(), maxSteps));
        }

        for (int i = 0; i < steps.size(); i++) {
            PlanStep step = steps.get(i);
            int stepIndex = i + 1;

            if (step == null) {
                errors.add(String.format("Step %d is null", stepIndex));
                continue;
            }

            String actionStr = step.getAction();
            if (actionStr == null || actionStr.isBlank()) {
                errors.add(String.format("Step %d has missing action name", stepIndex));
                continue;
            }

            ActionType actionType = ActionType.fromString(actionStr);
            if (actionType == null) {
                errors.add(String.format("Step %d action '%s' is not in the closed action vocabulary", stepIndex, actionStr));
                continue;
            }

            if (actionType == ActionType.UNRESOLVED) {
                String reason = step.getStringArg("reason");
                errors.add(String.format("Step %d is UNRESOLVED (source: '%s', reason: %s)",
                        stepIndex, step.getSource(), reason != null ? reason : "Ambiguous instruction"));
                continue;
            }

            validateRequiredArgs(actionType, step, stepIndex, errors);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private static void validateRequiredArgs(ActionType type, PlanStep step, int index, List<String> errors) {
        Map<String, Object> args = step.getArgs();
        Map<String, Object> target = step.getTarget();

        switch (type) {
            case NAVIGATE, API_BASE -> {
                if (!args.containsKey("url") || String.valueOf(args.get("url")).isBlank()) {
                    errors.add(String.format("Step %d (%s) requires 'url' argument", index, type));
                }
            }
            case CLICK, HOVER, CHECK, UNCHECK, VERIFY_VISIBLE, VERIFY_HIDDEN, VERIFY_ENABLED, VERIFY_DISABLED -> {
                if (target == null || target.isEmpty()) {
                    errors.add(String.format("Step %d (%s) requires 'target' locator argument", index, type));
                }
            }
            case FILL -> {
                if (target == null || target.isEmpty()) {
                    errors.add(String.format("Step %d (FILL) requires 'target' locator argument", index));
                }
                if (!args.containsKey("value")) {
                    errors.add(String.format("Step %d (FILL) requires 'value' argument", index));
                }
            }
            case SELECT -> {
                if (target == null || target.isEmpty()) {
                    errors.add(String.format("Step %d (SELECT) requires 'target' locator argument", index));
                }
                if (!args.containsKey("option") && !args.containsKey("value") && !args.containsKey("label")) {
                    errors.add(String.format("Step %d (SELECT) requires 'option', 'value', or 'label' argument", index));
                }
            }
            case PRESS -> {
                if (!args.containsKey("key") || String.valueOf(args.get("key")).isBlank()) {
                    errors.add(String.format("Step %d (PRESS) requires 'key' argument", index));
                }
            }
            case UPLOAD -> {
                if (target == null || target.isEmpty()) {
                    errors.add(String.format("Step %d (UPLOAD) requires 'target' locator argument", index));
                }
                if (!args.containsKey("file") || String.valueOf(args.get("file")).isBlank()) {
                    errors.add(String.format("Step %d (UPLOAD) requires 'file' path argument", index));
                }
            }
            case STORE_TEXT -> {
                if (target == null || target.isEmpty()) {
                    errors.add(String.format("Step %d (STORE_TEXT) requires 'target' locator argument", index));
                }
                if (!args.containsKey("as") || String.valueOf(args.get("as")).isBlank()) {
                    errors.add(String.format("Step %d (STORE_TEXT) requires 'as' variable name argument", index));
                }
            }
            case VERIFY_TEXT -> {
                if (target == null || target.isEmpty()) {
                    errors.add(String.format("Step %d (VERIFY_TEXT) requires 'target' locator argument", index));
                }
                if (!args.containsKey("expected")) {
                    errors.add(String.format("Step %d (VERIFY_TEXT) requires 'expected' text argument", index));
                }
            }
            case VERIFY_URL, VERIFY_TITLE, VERIFY_COUNT, VERIFY_STATUS -> {
                if (!args.containsKey("expected")) {
                    errors.add(String.format("Step %d (%s) requires 'expected' argument", index, type));
                }
            }
            case VERIFY_TABLE -> {
                if (!args.containsKey("columns")) {
                    errors.add(String.format("Step %d (VERIFY_TABLE) requires 'columns' array argument", index));
                }
            }
            case VERIFY_COMPONENTS -> {
                if (!args.containsKey("items")) {
                    errors.add(String.format("Step %d (VERIFY_COMPONENTS) requires 'items' array argument", index));
                }
            }
            case API_REQUEST -> {
                if (!args.containsKey("method")) {
                    errors.add(String.format("Step %d (API_REQUEST) requires 'method' argument (GET, POST, etc.)", index));
                }
            }
            case API_STORE -> {
                if (!args.containsKey("as")) {
                    errors.add(String.format("Step %d (API_STORE) requires 'as' variable argument", index));
                }
            }
            case VERIFY_FIELD_EXISTS -> {
                if (!args.containsKey("jsonPath")) {
                    errors.add(String.format("Step %d (VERIFY_FIELD_EXISTS) requires 'jsonPath' argument", index));
                }
            }
            case VERIFY_JSON -> {
                if (!args.containsKey("jsonPath") || !args.containsKey("expected")) {
                    errors.add(String.format("Step %d (VERIFY_JSON) requires 'jsonPath' and 'expected' arguments", index));
                }
            }
            case VERIFY_EVERY_ITEM_HAS -> {
                if (!args.containsKey("fields")) {
                    errors.add(String.format("Step %d (VERIFY_EVERY_ITEM_HAS) requires 'fields' array argument", index));
                }
            }
            case GENERATE_DATA -> {
                if (!args.containsKey("type") || !args.containsKey("as")) {
                    errors.add(String.format("Step %d (GENERATE_DATA) requires 'type' and 'as' arguments", index));
                }
            }
            case SET_VAR -> {
                if (!args.containsKey("name") || !args.containsKey("value")) {
                    errors.add(String.format("Step %d (SET_VAR) requires 'name' and 'value' arguments", index));
                }
            }
            default -> {}
        }
    }
}
