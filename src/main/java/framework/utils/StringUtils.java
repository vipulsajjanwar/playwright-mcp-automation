/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: String manipulation utilities: trimming, normalisation, truncation, regex helpers, and camelCase conversion.
 * Created: 2026-08-15
 */
package framework.utils;

import java.util.UUID;

public class StringUtils {

    private StringUtils() {}

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String getRandomUuid() {
        return UUID.randomUUID().toString();
    }

    public static String capitalize(String str) {
        if (isEmpty(str)) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
