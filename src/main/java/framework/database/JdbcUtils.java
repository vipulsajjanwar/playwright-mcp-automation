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
