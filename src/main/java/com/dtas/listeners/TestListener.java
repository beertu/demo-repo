package com.dtas.listeners;

import com.dtas.report.ExtentTestManager;
import com.dtas.utils.EmailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.*;
import java.io.File;
import java.time.LocalDate;

/**
 * TestNG listener that logs results, ensures graceful email reporting,
 * and sends the latest summary report via Outlook at the end of the test context.
 */
public class TestListener implements ITestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        LOGGER.info("Starting test: {}", testName);
        ExtentTestManager.startTest(testName, result.getMethod().getDescription());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOGGER.info("PASS - {}", result.getMethod().getMethodName());
        ExtentTestManager.logTestResult("PASS", "Test Passed: " + result.getMethod().getMethodName());
        ExtentTestManager.endTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOGGER.error("FAIL - {} : {}", result.getMethod().getMethodName(),
                result.getThrowable() == null ? "no exception" : result.getThrowable().getMessage());
        ExtentTestManager.logTestResult("FAIL", "Test Failed: " + result.getMethod().getMethodName());
        if (ExtentTestManager.getTest() != null && result.getThrowable() != null) {
            ExtentTestManager.getTest().fail(result.getThrowable());
        }
        ExtentTestManager.endTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOGGER.warn("SKIPPED - {}", result.getMethod().getMethodName());
        ExtentTestManager.logTestResult("SKIP", "Test Skipped: " + result.getMethod().getMethodName());
        ExtentTestManager.endTest();
    }

    @Override
    public void onStart(ITestContext context) {
        LOGGER.info("Test context started: {}", context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        LOGGER.info("All tests finished for: {}", context.getName());

        // ✅ Summary stats
        int passed = context.getPassedTests().size();
        int failed = context.getFailedTests().size();
        int skipped = context.getSkippedTests().size();
        int total = passed + failed + skipped;

        // ✅ Email subject and body
        String subject = "Automation Execution Report - " + context.getName() + " [" + LocalDate.now() + "]";
        StringBuilder body = new StringBuilder();
        body.append("Hello QA Team,").append(System.lineSeparator()).append(System.lineSeparator())
            .append("The automated test suite has completed execution.").append(System.lineSeparator()).append(System.lineSeparator())
            .append("Summary:").append(System.lineSeparator())
            .append("Total: ").append(total).append(System.lineSeparator())
            .append("Passed: ").append(passed).append(System.lineSeparator())
            .append("Failed: ").append(failed).append(System.lineSeparator())
            .append("Skipped: ").append(skipped).append(System.lineSeparator()).append(System.lineSeparator())
            .append("Attached is the latest summary report (if found).").append(System.lineSeparator());

        // ✅ Add simple AI-style analytics (pass rate insight)
        String aiInsights = String.format("AI Insights → Overall pass rate: %.2f%%",
                total == 0 ? 0.0 : (passed * 100.0 / total));
        body.append(System.lineSeparator()).append(aiInsights);

        // ✅ Locate latest report
        String reportDir = System.getProperty("user.dir") + File.separator + "ExtentReports";
        
        // Create ExtentReports directory if it doesn't exist
        File reportDirectory = new File(reportDir);
        if (!reportDirectory.exists()) {
            if (reportDirectory.mkdirs()) {
                LOGGER.info("Created ExtentReports directory: {}", reportDir);
            } else {
                LOGGER.warn("Failed to create ExtentReports directory: {}", reportDir);
            }
        }
        
        File latestReport = findLatestSummaryReport(reportDir);
        
        try {
            String attachmentPath = (latestReport != null) ? latestReport.getAbsolutePath() : null;

            if (attachmentPath != null) {
                LOGGER.info("Sending email with attachment: {}", attachmentPath);
            } else {
                LOGGER.warn("No report found to attach. Sending email without attachment.");
            }

            // ✅ Updated to match EmailUtils method signature
            EmailUtils.sendTestReport(
                    subject,
                    body.toString(),
                    attachmentPath,
                    null // or specify a custom recipient if needed
            );

            LOGGER.info("✅ Email dispatch attempted successfully.");

        } catch (Exception e) {
            LOGGER.error("❌ Failed to send test report email: {}", e.getMessage(), e);
        }
    }

    private File findLatestSummaryReport(String directoryPath) {
        // First try finding in ExtentReports directory (recursively)
        File dir = new File(directoryPath);
        if (dir.exists() && dir.isDirectory()) {
            // Search recursively for files ending with _summaryReport.html
            File[] summaryReports = dir.listFiles();
            File latestSummary = null;
            if (summaryReports != null) {
                // Walk subdirectories to locate summary reports
                java.util.Deque<File> stack = new java.util.ArrayDeque<>();
                stack.push(dir);
                while (!stack.isEmpty()) {
                    File current = stack.pop();
                    File[] children = current.listFiles();
                    if (children == null) continue;
                    for (File child : children) {
                        if (child.isDirectory()) {
                            stack.push(child);
                        } else if (child.isFile() && child.getName().endsWith("_summaryReport.html")) {
                            if (latestSummary == null || child.lastModified() > latestSummary.lastModified()) {
                                latestSummary = child;
                            }
                        }
                    }
                }
            }

            if (latestSummary != null) {
                LOGGER.info("Found summary report: {}", latestSummary.getAbsolutePath());
                return latestSummary;
            }

            // If no summary reports, try any HTML files in ExtentReports directory tree
            File latestHtml = null;
            java.util.Deque<File> stack2 = new java.util.ArrayDeque<>();
            stack2.push(dir);
            while (!stack2.isEmpty()) {
                File current = stack2.pop();
                File[] children = current.listFiles();
                if (children == null) continue;
                for (File child : children) {
                    if (child.isDirectory()) {
                        stack2.push(child);
                    } else if (child.isFile() && child.getName().toLowerCase().endsWith(".html")) {
                        if (latestHtml == null || child.lastModified() > latestHtml.lastModified()) {
                            latestHtml = child;
                        }
                    }
                }
            }

            if (latestHtml != null) {
                LOGGER.info("Found HTML report: {}", latestHtml.getAbsolutePath());
                return latestHtml;
            }
        }

        LOGGER.warn("No summary or HTML reports found under {}. Skipping test-output legacy fallbacks.", directoryPath);
        return null;
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        // Not used
    }
}