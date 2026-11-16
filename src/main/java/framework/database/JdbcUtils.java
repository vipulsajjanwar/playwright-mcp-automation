/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: JDBC utility class offering low-level query execution, result-set mapping, and connection lifecycle helpers.
 * Created: 2026-08-15
 */
package framework.database;

import framework.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcUtils {

    private static final Logger logger = LoggerFactory.getLogger(JdbcUtils.class);

    private JdbcUtils() {}

    public static void close(Connection conn, PreparedStatement stmt, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) { logger.warn("Failed to close ResultSet", e); }
        }
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException e) { logger.warn("Failed to close Statement", e); }
        }
        if (conn != null) {
            try { conn.close(); } catch (SQLException e) { logger.warn("Failed to close Connection", e); }
        }
    }
}
