package framework.database;

import framework.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryExecutor {

    private static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);

    private QueryExecutor() {}

    public static List<Map<String, Object>> executeQuery(String sql, Object... params) {
        logger.info("Executing SQL Query: {}", sql);
        List<Map<String, Object>> resultList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i).toUpperCase(), rs.getObject(i));
                }
                resultList.add(row);
            }
            return resultList;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to execute SQL query: " + sql, e);
        } finally {
            JdbcUtils.close(conn, stmt, rs);
        }
    }

    public static int executeUpdate(String sql, Object... params) {
        logger.info("Executing SQL Update: {}", sql);
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to execute SQL update: " + sql, e);
        } finally {
            JdbcUtils.close(conn, stmt, null);
        }
    }
}
