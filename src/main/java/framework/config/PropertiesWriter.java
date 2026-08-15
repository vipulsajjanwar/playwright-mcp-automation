package framework.config;

import framework.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class PropertiesWriter {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesWriter.class);

    public static void saveProperties(Properties properties, String filePath, String comments) {
        try (OutputStream output = new FileOutputStream(filePath)) {
            properties.store(output, comments);
            logger.info("Successfully updated property file: {}", filePath);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to save property file: " + filePath, e);
        }
    }
}
