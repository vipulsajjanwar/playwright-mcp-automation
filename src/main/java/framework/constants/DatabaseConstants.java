/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Constants for database connectivity: driver class names, connection timeout defaults, and SQL query templates.
 * Created: 2026-08-15
 */
package framework.constants;

public final class DatabaseConstants {

    private DatabaseConstants() {}

    public static final String H2_DRIVER = "org.h2.Driver";
    public static final String POSTGRES_DRIVER = "org.postgresql.Driver";
    public static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";

    public static final String DEFAULT_H2_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    public static final String DEFAULT_H2_USER = "sa";
    public static final String DEFAULT_H2_PASS = "";
}
