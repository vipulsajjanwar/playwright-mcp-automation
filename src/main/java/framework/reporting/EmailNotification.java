/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Email notification service sending HTML test execution summaries on suite completion or failure thresholds.
 * Created: 2026-09-28
 */
package framework.reporting;

import framework.config.ConfigManager;
import framework.constants.FrameworkConstants;
import framework.constants.TestConstants;
import framework.excel.ExcelUtils;
import framework.utils.CustomTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

public class EmailNotification {

    protected static final Logger logger = LoggerFactory.getLogger(EmailNotification.class);

    static String eHost;
    static String ePort;
    static String eUserName;
    static String ePassword;
    static String eTo;
    static String eCc;
    static String eBcc;
    static String eSubject;
    static String eMessage;
    String eBody;
    static String eSignature;

    static String brOpening="<br/>";
    static String tdOpeningStyle="<td style='padding: 12px; text-align: center; border: 1px solid #ddd; font-size: 10pt;'>";
    static String tdClosing="</td>";

    private EmailNotification(){
    }

    private static void setProperties(){
        eHost = ConfigManager.get("email.smtp.host");
        ePort = ConfigManager.get("email.smtp.port");
        eUserName = ConfigManager.get("email.smtp.username");
        ePassword = ConfigManager.get("email.smtp.password");
        eTo= ConfigManager.get("email.to");
        eCc= ConfigManager.get("email.cc");
        eBcc= ConfigManager.get("email.bcc");
        eSubject= ConfigManager.get("email.subject");
        eMessage= ConfigManager.get("email.message");
        eSignature = ConfigManager.get("email.signature");
    }

    public static String encodeImageToBase64(String imagePath) throws IOException {
        File file = new File(imagePath);
        byte[] imageBytes = new byte[(int) file.length()];

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            int bytesRead = fileInputStream.read(imageBytes);
            if (bytesRead != imageBytes.length) {
                throw new IOException("Could not read the entire file: " + imagePath);
            }
        }

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static void sendEMail(boolean isEnabled) {
        try {

           if(!isEnabled) return;

            setProperties();
            Session session = buildSmtpSession();

            String fileLocation = buildFileLocation();
            String filename = FrameworkConstants.REPORT_NAME;
            String zipFile = ExcelUtils.zipFile(filename, ".zip", fileLocation);
            long zipSize = ExcelUtils.getFileSizeNIO(zipFile);

            String base64Image = encodeImageToBase64(resourcePath("company_logo.png"));
            String base64Icon  = encodeImageToBase64(resourcePath("company_Icon.png"));

            if(eTo.isEmpty() || eTo.equalsIgnoreCase("NA")) {
                logger.info("Report path: "+fileLocation+"/"+filename);
                logger.info("No email recipients specified. Skipping email sending.");
                return;
            }
            logger.info("Report path: "+fileLocation+"/"+filename);
            Message message = buildMessage(session, base64Image, base64Icon, filename, zipFile, zipSize);
            Transport.send(message);

            logger.info("=====Email Sent=====");

        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

// --- SMTP session ---

    private static Session buildSmtpSession() {

        Properties props = new Properties();
        props.put("mail.smtp.host", eHost);
        props.put("mail.smtp.port", ePort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        // Do NOT set SSLSocketFactory here — incompatible with STARTTLS on port 587
        // This will handle the complete authentication
        return Session.getDefaultInstance(props,

                new javax.mail.Authenticator() {

                    protected PasswordAuthentication getPasswordAuthentication() {

                        return new PasswordAuthentication(eUserName, ePassword);

                    }

                });
    }

// --- Message assembly ---

    private static Message buildMessage(Session session,
                                        String base64Image, String base64Icon,
                                        String filename, String zipFile, long zipSize) throws MessagingException {

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(eUserName));

        if (eTo != null && !eTo.isBlank()) {
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(eTo)
            );
        }

        if (eCc != null && !eCc.isBlank()) {
            message.setRecipients(
                    Message.RecipientType.CC,
                    InternetAddress.parse(eCc)
            );
        }

        if (eBcc != null && !eBcc.isBlank()) {
            message.setRecipients(
                    Message.RecipientType.BCC,
                    InternetAddress.parse(eBcc)
            );
        }

        if (eSubject != null) {message.setSubject(eSubject);}

        Multipart multipart = new MimeMultipart();

        // HTML body part
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(buildHtmlBody(base64Image, base64Icon, filename, zipFile, zipSize), "text/html");
        multipart.addBodyPart(htmlPart);

        // Attachment — only when file is small enough
        if (zipSize <= 33_000) {
            MimeBodyPart attachPart = new MimeBodyPart();
            DataSource source = new FileDataSource(zipFile);
            attachPart.setDataHandler(new DataHandler(source));
            attachPart.setFileName(new File(zipFile).getName());
            multipart.addBodyPart(attachPart);
        }

        message.setContent(multipart);
        return message;
    }

// --- HTML body (single method, no duplication) ---

    private static String buildHtmlBody(String base64Image, String base64Icon,
                                        String filename, String zipFile, long zipSize) {
        StringBuilder b = new StringBuilder();

        b.append("Hi <b>Team</b>,").append(brOpening).append(brOpening);
        b.append("This automation test suite has been executed on the machine: <b>")
                .append(System.getProperty("HOSTNAME"))
                .append("</b>. Please find the summary of the test results below:");

        b.append("<html><body style='font-family:Arial,sans-serif;background-color:#f4f4f9;margin:0;padding:20px;'>");
        b.append("<div style='max-width:900px;margin:0 auto;padding:20px;background-color:#ffffff;border-radius:8px;box-shadow:0 4px 6px rgba(0,0,0,0.1);'>");

      //  appendLogoAndHeading(b, base64Image);
        appendResultsTable(b);
        appendMetadata(b, filename, zipSize);
        appendKibanaLink(b );
        appendAttachmentNote(b, filename, zipSize);
        //appendSignature(b, base64Icon);

        b.append("</div></body></html>");
        return b.toString();
    }

    private static void appendLogoAndHeading(StringBuilder b, String base64Image) {
        b.append("<table role='presentation' align='center' style='margin:0 auto 20px auto;border-collapse:collapse;'><tr>")
                .append("<td style='padding-right:12px;vertical-align:middle;'>")
                .append("<img src='data:image/png;base64,").append(base64Image)
                .append("' alt='vs-cortexflow logo' width='100' style='display:block;width:100px;height:auto;border:0;'/></td>")
                .append("<td style='vertical-align:middle;'>")
                .append("<h3 style='margin:0;color:#26215C;font-family:Arial,sans-serif;'>Test Automation Results</h3></td>")
                .append("</tr></table>")
                .append("<hr style='border:none;border-top:3px solid #1D9E75;margin:10px 0 20px 0;'/>");
    }

    private static void appendResultsTable(StringBuilder b) {
        b.append("<table style='width:100%;border-collapse:collapse;margin-top:20px;'>")
                .append("<thead style='background-color:#b58110;color:white;text-align:center;font-size:14px;'><tr>");

        for (String header : new String[]{"Start Time (IST)", "End Time (IST)", "Test Suite Name", "Passed", "Failed", "Skipped"}) {
            b.append("<th style='padding:12px;border:1px solid #ddd;'>").append(header).append("</th>");
        }
        b.append("</tr></thead>");

        b.append("<tbody style='background-color:#ffffff;font-size:13px;'>")
                .append("<tr style='background-color:#f2f2f2;'>")
                .append(tdOpeningStyle).append(TestConstants.startTime).append(tdClosing)
                .append(tdOpeningStyle).append(TestConstants.endTime).append(tdClosing)
                .append(tdOpeningStyle).append(TestConstants.SuiteName).append(tdClosing)
                .append("<td style='padding:12px;text-align:center;border:1px solid #ddd;color:green;font-size:14pt;'>").append(TestConstants.passedTC).append(tdClosing)
                .append("<td style='padding:12px;text-align:center;border:1px solid #ddd;color:red;font-size:14pt;'>").append(TestConstants.failedTC).append(tdClosing)
                .append("<td style='padding:12px;text-align:center;border:1px solid #ddd;color:blue;font-size:14pt;'>").append(TestConstants.skippedTC).append(tdClosing)
                .append("</tr></tbody></table>").append(brOpening);
    }

    private static void appendMetadata(StringBuilder b, String filename, long zipSize) {
        b.append("<p><strong>Application URL:</strong> <a href='").append(TestConstants.BaseURL)
                .append("' style='color:#007bff;text-decoration:none;'>").append(TestConstants.BaseURL).append("</a></p>")
                .append("<p><strong>Total number of test cases:</strong> <b>").append(TestConstants.TOTALTESTCASES).append("</b></p>");

        if (!eMessage.isEmpty()) {
            b.append("<p><strong>About Execution:</strong> <b>").append(eMessage).append("</b></p>");
        }

        b.append("<p><strong>Total Execution Time:</strong> ")
                .append(CustomTimeUtil.getTimeDifference(TestConstants.startTime, TestConstants.endTime, "HH.mm.ss-MM/dd/yyyy"))
                .append("</p>");

        b.append("<p><strong>X-Ray Test Execution Status:</strong> All test case statuses will be updated in X-Ray.</p>");

    }

    private static void appendKibanaLink(StringBuilder b) {
//        if (!BaseSuite.elkFlag) return;
//
//        String buildId = BaseSuite.buildId.toLowerCase();
//        String queryPart = "(filters:!((meta:(index:sanity-index1,key:buildId,params:(query:'" + buildId
//                + "'),type:phrase),query:(match_phrase:(buildId:'" + buildId + "')))))";
//        String encodedQuery = URLEncoder.encode(queryPart, StandardCharsets.UTF_8);
//
//        String suiteType = BaseSuite.suiteType.toLowerCase();
//        String kibanaPath;
//        String label;
//
//        if (suiteType.equals("sanity")) {
//            kibanaPath = cfg.kibanaSanityPath();
//            label = "NextGEN iceDQ Sanity Tests";
//        } else if (suiteType.equals("regression")) {
//            kibanaPath = cfg.kibanaRegressionPath();
//            label = "NextGEN iceDQ Regression Tests";
//        } else {
//            return;
//        }
//
//        String url = BaseSuite.kibanaURL + kibanaPath + encodedQuery;
//        b.append(dashboardTitle).append(url).append("' style='color:#007bff;text-decoration:none;'>").append(label).append("</a></p>");
    }

    private static void appendAttachmentNote(StringBuilder b, String filename, long zipSize) {
        b.append("<br/><br/>");
        if (zipSize > 33_000) {
            b.append("Report Link: <b>").append(filename).append("</b><br/><br/>");
        }
        b.append("<b>Note:</b> This email was sent automatically by ");
        b.append("<span style='color: orange; font-weight: bold;'>").append("App").append("</span>");
        b.append(" system. Please do not reply.<br/><br/>");
    }

    private static void appendSignature(StringBuilder b, String base64Icon) {
        b.append("<p><b style='color:#bbbab5;'>Thanks &amp; Regards,</b></p>")
                .append("<table style='width:100%;border-collapse:collapse;'><tr>")
                .append("<td style='width:10%;text-align:center;vertical-align:middle;'>")
                .append("<img src='data:image/png;base64,").append(base64Icon)
                .append("' alt='Company Logo' style='max-width:90px;height:auto;'><br/>")
                .append("<a href='https://icedq.com/' style='font-weight:bold;text-decoration:none;color:#4b7ad3;'>iceDQ.ai</a>")
                .append("</td>")
                .append("<td style='padding:0.1px;width:0.5px;background-color:#ffdd33;height:100px;'>&nbsp;</td>")
                .append("<td style='padding-left:10px;'>")
                .append("<b><i style='color:#4b7ad3;'>").append(eSignature).append(" </i>Team</b><br/>")
                .append("<i>Rethink Data Reliability!</i>")
                .append("</td></tr></table>");
    }

// --- Helpers ---

    private static String buildFileLocation() {
        return System.getProperty("user.dir") +"/"+ FrameworkConstants.REPORT_DIR;
    }

    private static String resourcePath(String filename) {
        return System.getProperty("user.dir") + "/src/test/resources/asset/" + filename;
    }
}
