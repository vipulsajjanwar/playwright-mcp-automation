/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Manages JDBC connections and connection pools for database validation and test data setup/teardown.
 * Created: 2026-08-15
 */
package framework.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import framework.config.ConfigManager;
import framework.constants.DatabaseConstants;
import framework.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static volatile HikariDataSource dataSource;

    private DatabaseManager() {}

    public static synchronized HikariDataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            String driver = ConfigManager.get("db.driver", DatabaseConstants.H2_DRIVER);
            String url = ConfigManager.get("db.url", DatabaseConstants.DEFAULT_H2_URL);
            String username = ConfigManager.get("db.username", DatabaseConstants.DEFAULT_H2_USER);
            String password = ConfigManager.get("db.password", DatabaseConstants.DEFAULT_H2_PASS);

            logger.info("Initializing HikariCP Data Source [Driver: {}, URL: {}]", driver, url);

            HikariConfig config = new HikariConfig();
            config.setDriverClassName(driver);
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);

            try {
                dataSource = new HikariDataSource(config);
            } catch (Exception e) {
                throw new DatabaseException("Failed to initialize database pool", e);
            }
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static synchronized void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing HikariCP Data Source...");
            dataSource.close();
            dataSource = null;
        }
    }
}
