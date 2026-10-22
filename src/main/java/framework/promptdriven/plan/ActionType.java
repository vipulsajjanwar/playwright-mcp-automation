package framework.promptdriven.plan;

import java.util.Locale;

/**
 * Closed action vocabulary for the deterministic execution engine.
 * Any action emitted by an LLM outside this enum will be rejected by {@link PlanValidator}.
 */
public enum ActionType {

    // UI Actions
    NAVIGATE,
    CLICK,
    FILL,
    SELECT,
    CHECK,
    UNCHECK,
    PRESS,
    HOVER,
    WAIT_FOR,
    UPLOAD,
    SWITCH_TAB,
    STORE_TEXT,
    SCREENSHOT,

    // UI Verifications
    VERIFY_VISIBLE,
    VERIFY_HIDDEN,
    VERIFY_TEXT,
    VERIFY_URL,
    VERIFY_TITLE,
    VERIFY_COUNT,
    VERIFY_TABLE,
    VERIFY_ENABLED,
    VERIFY_DISABLED,
    VERIFY_COMPONENTS,

    // API Actions
    API_BASE,
    API_REQUEST,
    API_STORE,

    // API Verifications
    VERIFY_STATUS,
    VERIFY_JSON,
    VERIFY_FIELD_EXISTS,
    VERIFY_EVERY_ITEM_HAS,
    VERIFY_HEADER,
    VERIFY_RESPONSE_TIME,
    VERIFY_SCHEMA,

    // Data Actions
    GENERATE_DATA,
    SET_VAR,

    // Ambiguity / Error
    UNRESOLVED;

    public boolean isVerification() {
        return name().startsWith("VERIFY_");
    }

    public boolean isUiAction() {
        return switch (this) {
            case NAVIGATE, CLICK, FILL, SELECT, CHECK, UNCHECK, PRESS, HOVER,
                 WAIT_FOR, UPLOAD, SWITCH_TAB, STORE_TEXT, SCREENSHOT,
                 VERIFY_VISIBLE, VERIFY_HIDDEN, VERIFY_TEXT, VERIFY_URL, VERIFY_TITLE,
                 VERIFY_COUNT, VERIFY_TABLE, VERIFY_ENABLED, VERIFY_DISABLED, VERIFY_COMPONENTS -> true;
            default -> false;
        };
    }

    public boolean isApiAction() {
        return switch (this) {
            case API_BASE, API_REQUEST, API_STORE,
                 VERIFY_STATUS, VERIFY_JSON, VERIFY_FIELD_EXISTS,
                 VERIFY_EVERY_ITEM_HAS, VERIFY_HEADER, VERIFY_RESPONSE_TIME, VERIFY_SCHEMA -> true;
            default -> false;
        };
    }

    public static ActionType fromString(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return ActionType.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
