/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Playwright-specific file action helpers for download interception, upload triggering, and file path resolution.
 * Created: 2026-10-07
 */
package framework.utils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.FilePayload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class PlaywrightFileActions {

    private static final double DOWNLOAD_TIMEOUT_MS = 60_000;

    private PlaywrightFileActions() {
    }

    // ================================================================== DOWNLOAD

    /** Clicks the trigger, waits for the download, saves it into targetDir under the server's filename. */
    public static Path download(Page page, Locator trigger, Path targetDir) {
        return download(page, trigger, targetDir, null);
    }

    /**
     * Clicks the trigger, waits for the download and saves it.
     * Playwright deletes its own temp copy when the browser context closes, so saving is mandatory.
     *
     * @param fileName custom file name, or null to use the browser's suggested name
     */
    public static Path download(Page page, Locator trigger, Path targetDir, String fileName) {
        Download download = page.waitForDownload(
                new Page.WaitForDownloadOptions().setTimeout(DOWNLOAD_TIMEOUT_MS),
                () -> trigger.click());

        String failure = download.failure(); // blocks until the download finishes
        if (failure != null) {
            throw new IllegalStateException("Download failed: " + failure);
        }

        String name = fileName != null ? fileName : sanitize(download.suggestedFilename());
        FileUtils.createDirectory(targetDir);
        Path target = targetDir.resolve(name);
        download.saveAs(target);

        if (FileUtils.size(target) == 0) {
            throw new IllegalStateException("Downloaded file is empty: " + target);
        }
        return target;
    }

    /** Downloads through the HTTP client, no browser. Uses page.request() so cookies and auth carry over. */
    public static Path downloadViaRequest(APIRequestContext request, String url, Path target) {
        APIResponse response = request.get(url);
        if (!response.ok()) {
            throw new IllegalStateException("GET " + url + " returned " + response.status());
        }
        return FileUtils.createFile(target, response.body());
    }

    // ================================================================== UPLOAD

    /** Sets files directly on an input[type=file]. Works even if the input is hidden. Preferred method. */
    public static void upload(Locator fileInput, Path... files) {
        requireExist(files);
        fileInput.setInputFiles(files);
    }

    /** For pages with no reachable input: clicks a button that opens the native chooser, then feeds it. */
    public static void uploadWithChooser(Page page, Locator trigger, Path... files) {
        requireExist(files);
        FileChooser chooser = page.waitForFileChooser(() -> trigger.click());
        chooser.setFiles(files);
    }

    /** Uploads content straight from memory, with no file on disk. */
    public static void uploadFromMemory(Locator fileInput, String fileName, String mimeType, byte[] content) {
        fileInput.setInputFiles(new FilePayload(fileName, mimeType, content));
    }

    /** Removes any selected files from the input. */
    public static void clearUpload(Locator fileInput) {
        fileInput.setInputFiles(new Path[0]);
    }

    // ================================================================== DRAG AND DROP

    /** Standard element-to-element drag. Try this first. */
    public static void dragAndDrop(Locator source, Locator target) {
        source.dragTo(target);
    }

    /**
     * Manual mouse drag for sortable lists and libraries where dragTo() does nothing.
     * Source and target must both be inside the viewport at the same time.
     */
    public static void dragAndDropWithMouse(Page page, Locator source, Locator target) {
        source.scrollIntoViewIfNeeded();
        BoundingBox from = source.boundingBox();
        BoundingBox to = target.boundingBox();
        if (from == null || to == null) {
            throw new IllegalStateException("Source or target is not visible");
        }
        page.mouse().move(from.x + from.width / 2, from.y + from.height / 2);
        page.mouse().down();
        page.mouse().move(to.x + to.width / 2, to.y + to.height / 2, new Mouse.MoveOptions().setSteps(15));
        page.mouse().up();
    }

    /**
     * Drops a local file onto an HTML5 dropzone by dispatching synthetic dragenter, dragover and drop events.
     * Use this only when the page has no input[type=file] at all; otherwise use {@link #upload}.
     * The file is passed to the browser as base64, so keep it small (a few MB at most).
     */
    public static void dropFile(Page page, Locator dropZone, Path file) {
        requireExist(file);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read file: " + file, e);
        }

        JSHandle dataTransfer = page.evaluateHandle(
                "({name, mime, b64}) => {"
                        + "  const dt = new DataTransfer();"
                        + "  const bin = atob(b64);"
                        + "  const arr = new Uint8Array(bin.length);"
                        + "  for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);"
                        + "  dt.items.add(new File([arr], name, { type: mime }));"
                        + "  return dt;"
                        + "}",
                Map.of("name", file.getFileName().toString(),
                        "mime", mimeType(file),
                        "b64", Base64.getEncoder().encodeToString(bytes)));

        for (String event : List.of("dragenter", "dragover", "drop")) {
            dropZone.dispatchEvent(event, Map.of("dataTransfer", dataTransfer));
        }
    }

    // ================================================================== internal

    private static void requireExist(Path... files) {
        for (Path file : files) {
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("File not found: " + file.toAbsolutePath());
            }
        }
    }

    private static String mimeType(Path file) {
        try {
            String type = Files.probeContentType(file);
            return type != null ? type : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    /** Server-supplied filenames can contain characters that are illegal on Windows. */
    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
