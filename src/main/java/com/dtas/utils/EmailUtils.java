package com.dtas.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Email utility adapted to support both 3-arg and 4-arg sendTestReport signatures.
 * - sendTestReport(subject, body, attachmentPath) -> uses configured recipient
 * - sendTestReport(subject, body, attachmentPath, recipient) -> uses provided recipient (if null/empty uses configured recipient)
 *
 * Looks up global.properties for attachmentPath/reportFileName when attachmentPath is null.
 */
public class EmailUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailUtils.class);

    private static final String EMAIL_CONFIG_FILE = System.getProperty("user.dir") + "/src/main/resources/email.properties";
    private static final String GLOBAL_CONFIG_FILE = System.getProperty("user.dir") + "/src/main/resources/global.properties";

    private static final Properties SMTP_PROPS = new Properties(); // mail transport properties (not credentials)
    private static final Properties CREDENTIALS = new Properties();
    private static final Properties GLOBAL = new Properties();

    static {
        // Gmail SMTP properties
        SMTP_PROPS.put("mail.smtp.host", "smtp.gmail.com");
        SMTP_PROPS.put("mail.smtp.port", "587");
        SMTP_PROPS.put("mail.smtp.auth", "true");
        SMTP_PROPS.put("mail.smtp.starttls.enable", "true");
        SMTP_PROPS.put("mail.smtp.starttls.required", "true");
        SMTP_PROPS.put("mail.smtp.ssl.protocols", "TLSv1.2");
        SMTP_PROPS.put("mail.smtp.ssl.trust", "smtp.gmail.com");
    // Disable JavaMail debug output to avoid printing full MIME/HTML content to the console
    SMTP_PROPS.put("mail.debug", "false");

        // Load credentials (smtp.username, smtp.password, smtp.from, smtp.to) from email.properties
        boolean loaded = false;
        
        // First try loading from classpath
        try (InputStream is = EmailUtils.class.getClassLoader().getResourceAsStream("email.properties")) {
            if (is != null) {
                CREDENTIALS.load(is);
                LOGGER.info("Successfully loaded email.properties from classpath");
                loaded = true;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load from classpath: {}", e.getMessage());
        }
        
        // If classpath loading failed, try file system
        if (!loaded) {
            try (FileInputStream fis = new FileInputStream(EMAIL_CONFIG_FILE)) {
                CREDENTIALS.load(fis);
                LOGGER.info("Successfully loaded email.properties from file system: {}", EMAIL_CONFIG_FILE);
                loaded = true;
            } catch (IOException e) {
                LOGGER.error("Failed to load from file system {}: {}", EMAIL_CONFIG_FILE, e.getMessage());
            }
        }
        
        if (loaded) {
            // Log all property names that were loaded (but not their values)
            LOGGER.info("Loaded properties: {}", String.join(", ", CREDENTIALS.stringPropertyNames()));
            
            // Verify each required property
            String[] required = {"smtp.username", "smtp.password", "smtp.from", "smtp.to"};
            for (String prop : required) {
                if (CREDENTIALS.getProperty(prop) == null) {
                    LOGGER.error("Required property '{}' is missing from email.properties", prop);
                } else {
                    if (!prop.contains("password")) {
                        LOGGER.info("Found property '{}' with value: {}", prop, CREDENTIALS.getProperty(prop));
                    } else {
                        LOGGER.info("Found property '{}' (value not logged)", prop);
                    }
                }
            }
        } else {
            LOGGER.error("Failed to load email.properties from both classpath and file system");
        }

        // Load global config (attachmentPath, reportFileName, etc.)
        try (FileInputStream fis = new FileInputStream(GLOBAL_CONFIG_FILE)) {
            GLOBAL.load(fis);
            LOGGER.info("Loaded global config from {}", GLOBAL_CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.warn("Failed to load global properties from {}: {}", GLOBAL_CONFIG_FILE, e.getMessage());
        }
    }

    /**
     * Backwards-compatible 3-arg method. Uses configured smtp.to as recipient.
     *
     * @param subject        email subject
     * @param body           email body (plain text)
     * @param attachmentPath explicit attachment path or null
     */
    public static void sendTestReport(String subject, String body, String attachmentPath) {
        sendTestReport(subject, body, attachmentPath, null);
    }

    /**
     * New 4-arg method: explicit recipient optional (if null/empty uses smtp.to from email.properties).
     *
     * @param subject        email subject
     * @param body           email body (plain text)
     * @param attachmentPath explicit attachment path or null (folder or file). If null, resolves via global.properties/ExtentReports.
     * @param recipient      explicit recipient email address (optional). If null/empty, uses smtp.to from email.properties.
     */
    public static void sendTestReport(String subject, String body, String attachmentPath, String recipient) {
        // Get credentials and log their presence (but not values)
        String username = CREDENTIALS.getProperty("smtp.username");
        String password = CREDENTIALS.getProperty("smtp.password"); // App password recommended for personal accounts
        String from = CREDENTIALS.getProperty("smtp.from");
        String configuredTo = CREDENTIALS.getProperty("smtp.to");
        
        LOGGER.debug("Credential check - Username present: {}, Password present: {}, From present: {}, To present: {}", 
            username != null, password != null, from != null, configuredTo != null);
            
        // Dump all property names (not values) for debugging
        LOGGER.debug("Available properties: {}", String.join(", ", CREDENTIALS.stringPropertyNames()));

        // Determine effective recipient
        String to = (recipient != null && !recipient.trim().isEmpty()) ? recipient.trim() : configuredTo;

        if (username == null || password == null || from == null || to == null) {
            String msg = "Missing required email configuration (smtp.username/smtp.password/smtp.from/smtp.to) in " + EMAIL_CONFIG_FILE;
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }

        // Build mail session with debug enabled
    // Disable JavaMail debug output (don't show SMTP DATA / email body in console)
    SMTP_PROPS.put("mail.debug", "false");
        SMTP_PROPS.put("mail.smtp.auth", "true");
        
        final String finalUsername = username;
        final String finalPassword = password;
        
        LOGGER.info("Attempting to authenticate with username: {}", finalUsername);
        Session session = Session.getInstance(SMTP_PROPS, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(finalUsername, finalPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject == null ? "Automation Test Report" : subject);

            // Compose body + AI insights (body is plain text)
            StringBuilder sb = new StringBuilder();
            if (body != null) sb.append(body).append(System.lineSeparator()).append(System.lineSeparator());

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(sb.toString());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);

            // Resolve attachment file (explicit path wins)
            File attachmentFile = resolveAttachmentFile(attachmentPath);

            if (attachmentFile != null && attachmentFile.exists() && attachmentFile.isFile()) {
                MimeBodyPart attach = new MimeBodyPart();
                attach.attachFile(attachmentFile);
                multipart.addBodyPart(attach);
                LOGGER.info("Attached report: {}", attachmentFile.getAbsolutePath());
            } else {
                LOGGER.warn("No report attached (file not found). Looked at: {}", (attachmentFile == null ? "no candidate" : attachmentFile.getAbsolutePath()));
            }

            message.setContent(multipart);

            LOGGER.info("Sending test report email to {}", to);
            Transport.send(message);
            LOGGER.info("Test report email sent successfully to {}", to);

        } catch (AuthenticationFailedException e) {
            LOGGER.error("Authentication failed: {}. Use an Outlook App Password (or OAuth2) for Microsoft 365.", e.getMessage(), e);
            throw new RuntimeException("Outlook authentication failed. Ensure you are using an App Password or OAuth2.", e);
        } catch (MessagingException | IOException e) {
            LOGGER.error("Failed to send test report email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send test report email", e);
        }
    }

    /**
     * Resolve the file to attach:
     * - if attachmentPath provided:
     *     - if file -> return it
     *     - if directory -> pick latest *_summaryReport.html or newest .html inside
     *     - if relative, try relative to project root
     * - else:
     *     - use global.attachmentPath (or default "ExtentReports") and global.reportFileName (or latest *_summaryReport)
     */
    private static File resolveAttachmentFile(String attachmentPath) {
        try {
            // If explicit path provided, try that first (explicit wins)
            if (attachmentPath != null && !attachmentPath.trim().isEmpty()) {
                File explicit = new File(attachmentPath);
                if (!explicit.exists()) {
                    explicit = new File(System.getProperty("user.dir"), attachmentPath);
                }
                if (explicit.exists()) {
                    if (explicit.isFile()) return explicit;
                    if (explicit.isDirectory()) return findLatestReportInDir(explicit);
                }
                // If explicit path was provided but doesn't exist, log and continue to fallbacks
                LOGGER.warn("Explicit attachmentPath provided but not found: {}", attachmentPath);
            }

            // Do NOT use legacy emailable-report.html fallback.
            // Rationale: we prefer explicit attachmentPath or ExtentReports summary files only.
            // If needed, users can still point TestListener to test-output explicitly.

            // Last try global properties
            String cfgFolder = GLOBAL.getProperty("attachmentPath", GLOBAL.getProperty("reportPath", "ExtentReports"));
            String cfgFileName = GLOBAL.getProperty("reportFileName", "summaryReport.html");

            File dir = new File(System.getProperty("user.dir"), cfgFolder);
            if (!dir.exists() || !dir.isDirectory()) {
                LOGGER.warn("ExtentReports directory not found at {}", dir.getAbsolutePath());
                return null;
            }

            // prefer exact configured file name if present
            File exact = new File(dir, cfgFileName);
            if (exact.exists() && exact.isFile()) return exact;

            // otherwise find latest matching *_summaryReport.html or newest html
            return findLatestReportInDir(dir);

        } catch (Exception e) {
            LOGGER.error("Error resolving attachment file: {}", e.getMessage(), e);
            return null;
        }
    }

    private static File findLatestReportInDir(File dir) {
        LOGGER.info("Searching for report files in directory: {}", dir.getAbsolutePath());
        try (Stream<Path> s = Files.list(dir.toPath())) {
            // first prefer *_summaryReport.html
            Optional<Path> opt = s
                    .filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .filter(p -> {
                        boolean matches = p.getFileName().toString().toLowerCase().endsWith("_summaryreport.html");
                        LOGGER.debug("Checking file: {} - matches pattern: {}", p, matches);
                        return matches;
                    })
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));

            if (opt.isPresent()) {
                LOGGER.info("Found matching summary report: {}", opt.get());
                return opt.get().toFile();
            }

            // fallback: find newest .html
            LOGGER.info("No summary report found, looking for any HTML report");
            try (Stream<Path> s2 = Files.list(dir.toPath())) {
                Optional<Path> newest = s2
                    .filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .filter(p -> {
                        boolean matches = p.getFileName().toString().toLowerCase().endsWith(".html");
                        LOGGER.debug("Checking file: {} - matches pattern: {}", p, matches);
                        return matches;
                    })
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
                
                if (newest.isPresent()) {
                    LOGGER.info("Found HTML report: {}", newest.get());
                    return newest.get().toFile();
                } else {
                    LOGGER.warn("No HTML reports found in directory");
                    return null;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error scanning directory {}: {}", dir.getAbsolutePath(), e.getMessage(), e);
            return null;
        }
    }
}