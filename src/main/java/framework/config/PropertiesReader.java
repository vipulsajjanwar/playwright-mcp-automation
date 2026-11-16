/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Utility for reading Java .properties files from classpath or file system with caching and type-safe accessors.
 * Created: 2026-08-15
 */
package framework.config;

import framework.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesReader {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesReader.class);

    public static Properties loadProperties(String filePath) {
        Properties properties = new Properties();
        if (!Files.exists(Paths.get(filePath))) {
            logger.warn("Property file not found at path: {}. Returning empty properties.", filePath);
            return properties;
        }

        try (InputStream input = new FileInputStream(filePath)) {
            properties.load(input);
            logger.info("Successfully loaded properties from: {}", filePath);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load properties file: " + filePath, e);
        }
        return properties;
    }
}
