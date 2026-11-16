/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Page Object for file upload functionality, encapsulating input element targeting, file attachment, and upload verification.
 * Created: 2026-08-15
 */
package framework.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.base.BasePage;
import framework.utils.ElementUtils;

public class FileUploadPage extends BasePage {

    private final Locator fileInput;
    private final Locator uploadButton;
    private final Locator uploadedFilesList;

    public FileUploadPage(Page page) {
        super(page);
        this.fileInput = page.locator("input[type='file'], #file-upload");
        this.uploadButton = page.locator("#file-submit, button:has-text('Upload')");
        this.uploadedFilesList = page.locator("#uploaded-files, .uploaded-file-item");
    }

    public void uploadFile(String filePath) {
        ElementUtils.setFileInput(fileInput, filePath, "File Input");
        if (ElementUtils.isVisible(uploadButton)) {
            ElementUtils.click(uploadButton, "Upload Button");
        }
    }

    public String getUploadedFileName() {
        return ElementUtils.getText(uploadedFilesList, "Uploaded File Name");
    }
}
