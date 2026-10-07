package framework.utils;

import framework.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // ------------------------------------------------------------------ create

    public static Path createDirectory(Path dir) {
        try {
            return Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create directory: " + dir, e);
        }
    }

    /** Creates (or overwrites) a text file. Parent folders are created if missing. */
    public static Path createFile(Path file, String content) {
        return createFile(file, content.getBytes(StandardCharsets.UTF_8));
    }

    /** Creates (or overwrites) a binary file. Parent folders are created if missing. */
    public static Path createFile(Path file, byte[] content) {
        try {
            createParent(file);
            return Files.write(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create file: " + file, e);
        }
    }

    /** Creates a zero-filled file of an exact size. Handy for upload size-limit tests. */
    public static Path createDummyFile(Path file, long sizeBytes) {
        try {
            createParent(file);
            try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
                raf.setLength(sizeBytes);
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create dummy file: " + file, e);
        }
    }

    // ------------------------------------------------------------------ inspect

    /** True for a file OR a directory. Use isFile / isDirectory when the type matters. */
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    /** True only for a regular file, false for a directory or a missing path. */
    public static boolean isFile(Path path) {
        return Files.isRegularFile(path);
    }

    public static boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    /**
     * Waits until the file exists, is non-empty, and its size stopped changing between two polls.
     * Returns false on timeout. Meant for files written by another process or tool; a Playwright
     * download saved with saveAs() is already complete and does not need this.
     */
    public static boolean waitForFile(Path file, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        long lastSize = -1;
        while (System.nanoTime() < deadline) {
            try {
                if (Files.isRegularFile(file)) {
                    long size = Files.size(file);
                    if (size > 0 && size == lastSize) {
                        return true;
                    }
                    lastSize = size;
                }
            } catch (IOException e) {
                lastSize = -1; // file vanished or is locked mid-write, start the stability check over
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public static long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read size of: " + file, e);
        }
    }

    public static String humanSize(Path file) {
        return humanReadable(size(file));
    }

    public static String humanReadable(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = Math.min((int) (Math.log(bytes) / Math.log(1024)), 4);
        char unit = "KMGT".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), unit);
    }

    public static String readString(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read file: " + file, e);
        }
    }

    public static List<Path> list(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.sorted().collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list directory: " + dir, e);
        }
    }

    public static String getExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    /** "report.pdf" -> "report_3f9a1c2b.pdf". Avoids collisions in parallel runs. */
    public static String uniqueName(String fileName) {
        String suffix = "_" + UUID.randomUUID().toString().substring(0, 8);
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName + suffix : fileName.substring(0, dot) + suffix + fileName.substring(dot);
    }

    // ------------------------------------------------------------------ rename / move / copy

    /** Renames within the same folder. newName must be a bare name, not a path. */
    public static Path rename(Path file, String newName) {
        if (newName.contains("/") || newName.contains("\\")) {
            throw new IllegalArgumentException("newName must not contain path separators: " + newName);
        }
        try {
            return Files.move(file, file.resolveSibling(newName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot rename " + file + " to " + newName, e);
        }
    }

    /** Moves a file into targetDir, keeping its name. Overwrites an existing file with the same name. */
    public static Path move(Path source, Path targetDir) {
        try {
            createDirectory(targetDir);
            return Files.move(source, targetDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot move " + source + " to " + targetDir, e);
        }
    }

    public static Path copy(Path source, Path target) {
        try {
            createParent(target);
            return Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot copy " + source + " to " + target, e);
        }
    }

    // ------------------------------------------------------------------ delete

    /** Deletes a file or a whole directory tree. Returns false if it did not exist. */
    public static boolean delete(Path path) {
        if (!Files.exists(path)) {
            return false;
        }
        try {
            if (Files.isDirectory(path)) {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                Files.delete(path);
            }
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete: " + path, e);
        }
    }

    /** Deletes everything inside dir but keeps dir itself. */
    public static void cleanDirectory(Path dir) {
        if (Files.isDirectory(dir)) {
            list(dir).forEach(FileUtils::delete);
        }
    }

    // ------------------------------------------------------------------ internal

    private static void createParent(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
