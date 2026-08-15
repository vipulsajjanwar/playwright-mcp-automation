package framework.utils;

import framework.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {}

    public static String readFileToString(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            throw new FrameworkException("Failed to read file: " + filePath, e);
        }
    }

    public static void writeStringToFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
            logger.info("Successfully written content to file: {}", filePath);
        } catch (IOException e) {
            throw new FrameworkException("Failed to write to file: " + filePath, e);
        }
    }

    public static boolean exists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
}
