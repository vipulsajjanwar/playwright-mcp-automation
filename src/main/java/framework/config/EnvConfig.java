/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Environment-specific configuration holder mapping base URLs, credentials, and service endpoints per deployment environment.
 * Created: 2026-10-07
 */
package framework.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Set;

public class EnvConfig {
    private static final Set<String> VALID = Set.of("dev", "qa", "prod");
    private static final String ENV = resolveEnv();
    private static final Dotenv base = load(".env");
    private static final Dotenv specific = load(".env." + ENV);

    private EnvConfig() {}

    private static String resolveEnv() {
        // priority: -Denv=qa  >  APP_ENV=qa  >  default "dev"
        String env = System.getProperty("env", System.getenv("env"));
        env = (env == null || env.isBlank()) ? "dev" : env.toLowerCase().trim();
        if (!VALID.contains(env)) {
            throw new IllegalStateException("Unknown environment: '" + env + "'. Use one of " + VALID);
        }
        return env;
    }

    private static Dotenv load(String filename) {
        return Dotenv.configure().filename(filename).ignoreIfMissing().load();
    }

    public static String get(String key) {
        String v = specific.get(key);          // env-specific file (or real env var) wins
        if (v == null) v = base.get(key);      // fall back to shared .env
        return v;
    }

    public static String require(String key) {
        String v = get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing config '" + key + "' for env '" + ENV + "'");
        }
        return v;
    }

    public static String env() { return ENV; }
}
