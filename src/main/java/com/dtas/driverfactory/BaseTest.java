package com.dtas.driverfactory;

import com.dtas.commons.FunctionLibrary;
import com.dtas.commons.TestDataHandler;
import com.dtas.listeners.AnnotationTransformer;
import com.dtas.listeners.ExecutionListener;
// TestListener is registered via Execution.xml to avoid duplicate invocations
import com.dtas.report.ExtentTestManager;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList; // Import ArrayList
import java.util.List;     // Import List
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Abstract base class for all test classes in the Playwright test automation framework.
 * This class provides core functionality for:
 * - Browser setup and teardown using Playwright
 * - Configuration management through global.properties
 * - Test data handling via Excel integration
 * - ExtentReports setup and management
 * - Thread-safe browser session management
 * - URL navigation with environment detection
 * 
 * All test classes should extend this class to inherit the framework's core features.
 * The class uses TestNG annotations and listeners for test lifecycle management.
 * 
 * Thread Safety:
 * - Browser instances are managed per-thread using ThreadLocal
 * - Configuration loading is synchronized
 * - ExtentReports integration is thread-safe
 * 
 * @author Jan Robert Bodino
 * @see ExecutionListener
 * @see AnnotationTransformer
 */
@Listeners({ ExecutionListener.class, AnnotationTransformer.class })
public abstract class BaseTest {

    /** Thread-local storage for Playwright instance per test thread */
    protected static ThreadLocal<Playwright> playwrightThread = new ThreadLocal<>();
    
    /** Thread-local storage for Browser instance per test thread */
    protected static ThreadLocal<Browser> browserThread = new ThreadLocal<>();
    
    /** Thread-local storage for BrowserContext instance per test thread */
    protected static ThreadLocal<BrowserContext> contextThread = new ThreadLocal<>();
    
    /** Thread-local storage for Page instance per test thread */
    protected static ThreadLocal<Page> pageThread = new ThreadLocal<>();

    /** Logger instance for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);
    
    /** Global properties loaded from global.properties file */
    private static ResourceBundle globalProperties;
    
    /** Path to the external Excel test data file */
    private static String externalSheetPath;
    
    /** Type of test data source (currently only 'excel' is supported) */
    private static String testDataSource;
    
    /** Handler for test data operations */
    protected TestDataHandler testHandler;
    
    /** Base path for ExtentReports output */
    private static String reportPath;
    
    /** Full path for the current test report file */
    public static String newPath;

    /**
     * Gets the Playwright Page instance for the current test thread.
     * This method is thread-safe and returns the Page instance specific to the calling thread.
     *
     * @return The current thread's Playwright Page instance
     */
    public Page getPage() {
        return pageThread.get();
    }

    /**
     * Base constructor for test classes.
     * Intentionally empty as initialization is handled by @BeforeClass and @BeforeMethod methods
     * to ensure proper TestNG lifecycle management.
     */
    public BaseTest() {
        // Constructor intentionally empty - initialization in @BeforeClass/@BeforeMethod
    }

    @BeforeClass(alwaysRun = true)
    public void baseTestClassSetup() {
        getGlobalProperties();

        // Initialize TestDataHandler once per test class
        this.testHandler = new TestDataHandler();

        if (reportPath != null && !reportPath.isEmpty()) {
            LOGGER.info("Attempting to delete ExtentReports directory: {}", reportPath);
            FunctionLibrary.deleteDirectory(reportPath);
        } else {
            LOGGER.warn("Report path is null or empty. Cannot clean ExtentReports directory.");
        }
    }


    @BeforeMethod
    @Parameters({ "browser" })
    public void setupPlaywrightForMethod(@Optional("chromium") String browserType) throws IOException {
        LOGGER.info("Initializing Playwright browser for new test method: {}", browserType);
        try {
            Playwright playwrightInstance = Playwright.create();
            playwrightThread.set(playwrightInstance);
            LOGGER.debug("Playwright instance created for thread: {}", Thread.currentThread().threadId());

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(false); // Keep headless as false for visible browser
            LOGGER.debug("Set browser options: headless mode disabled.");

            // Add --start-maximized argument for Chromium-based browsers
            List<String> arguments = new ArrayList<>();
            if (browserType.equalsIgnoreCase("chrome") || browserType.equalsIgnoreCase("chromium")) {
                arguments.add("--start-maximized");
                launchOptions.setArgs(arguments); // Set the arguments to launchOptions
                LOGGER.debug("Added '--start-maximized' argument for Chromium browser.");
            }


            Browser browserInstance;

            switch (browserType.toLowerCase()) {
                case "firefox":
                    browserInstance = playwrightInstance.firefox().launch(launchOptions);
                    break;
                case "webkit":
                    browserInstance = playwrightInstance.webkit().launch(launchOptions);
                    break;
                case "chrome":
                case "chromium":
                default:
                    browserInstance = playwrightInstance.chromium().launch(launchOptions);
                    break;
            }
            browserThread.set(browserInstance);
            LOGGER.info("{} browser launched successfully for thread: {}", browserType, Thread.currentThread().threadId());

            // When using --start-maximized, it's often recommended to set viewport to null
            // to let the browser determine its own size.
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(null); // Set viewport to null for maximize

            BrowserContext contextInstance = browserInstance.newContext(contextOptions);
            contextThread.set(contextInstance);

            LOGGER.debug("BrowserContext created with viewport size set to null (for maximize) for thread: {}", Thread.currentThread().threadId());


            Page pageInstance = contextInstance.newPage();
            pageThread.set(pageInstance);
            LOGGER.info("Playwright page initialized for thread: {}", Thread.currentThread().threadId());

            if (pageInstance == null) {
                throw new IllegalStateException("Playwright Page is null after initialization! Critical error.");
            }

            setupPages(pageInstance);
            LOGGER.debug("setupPages() method executed for thread: {}", Thread.currentThread().threadId());

        } catch (PlaywrightException e) {
            LOGGER.error("Playwright error in setupPlaywrightForMethod for thread {}: {}", Thread.currentThread().getId(), e.getMessage(), e);
            Assert.fail("Playwright browser setup failed: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("General error in setupPlaywrightForMethod for thread {}: {}", Thread.currentThread().getId(), e.getMessage(), e);
            Assert.fail("Browser setup failed: " + e.getMessage(), e);
        }
    }
    @AfterMethod(alwaysRun = true)
    public void teardownPlaywrightAfterMethod(ITestResult testResult) {
        try {
            Page currentPage = pageThread.get();
            BrowserContext currentContext = contextThread.get();
            Browser currentBrowser = browserThread.get();
            Playwright currentPlaywright = playwrightThread.get();

            if (currentPage == null) {
                LOGGER.warn("Page instance is already null for thread: {}", Thread.currentThread().threadId());
            }

            if (currentPage != null) {
                LOGGER.debug("Closing Playwright page for thread: {}", Thread.currentThread().getId());
                currentPage.close();
            } else {
                LOGGER.warn("Page was already null when attempting to close for thread: {}", Thread.currentThread().getId());
            }

            if (currentContext != null) {
                currentContext.close();
            }
            if (currentBrowser != null) {
                currentBrowser.close();
            }
            if (currentPlaywright != null) {
                currentPlaywright.close();
            }

            pageThread.remove();
            contextThread.remove();
            browserThread.remove();
            playwrightThread.remove();

            LOGGER.info("Playwright resources closed for thread: {}", Thread.currentThread().threadId());

        } catch (Exception e) {
            LOGGER.error("Error during Playwright cleanup in teardownPlaywrightAfterMethod for thread {}: {}", Thread.currentThread().threadId(), e.getMessage(), e);
        }
    }

    protected void setupPages(Page page) {
        LOGGER.debug("setupPages() method invoked (can be overridden by subclasses).");
    }

    public static String getRepoPath() {
        if (globalProperties == null) {
            LOGGER.warn("globalProperties not loaded when calling getRepoPath(). Attempting to load.");
            getGlobalProperties();
        }

        if (newPath == null || newPath.isEmpty()) {
            String reportFileName = "report.html";
            try {
                reportFileName = globalProperties.getString("reportFileName");
            } catch (MissingResourceException e) {
                LOGGER.warn("Property 'reportFileName' not found in global.properties. Using default: {}", reportFileName, e);
            }

            LocalTime time = LocalTime.now().withNano(0);
            LocalDate date = LocalDate.now();

            if (reportPath == null || reportPath.isEmpty()) {
                LOGGER.warn("reportPath is null or empty. Using current working directory for report generation.");
                reportPath = System.getProperty("user.dir");
            }
            newPath = reportPath + System.getProperty("file.separator") + date + System.getProperty("file.separator") + String.valueOf(time).replace(":", "") + "_" + reportFileName;
            LOGGER.info("Constructed Report Path: {}", newPath);
        }
        return newPath;
    }

    public static String externalSheetPath() {
        if (globalProperties == null) {
            LOGGER.warn("globalProperties not loaded when calling externalSheetPath(). Attempting to load.");
            getGlobalProperties();
        }
        return externalSheetPath;
    }

    public static synchronized void verifyExtentTestInstance(String testName, String description) {
        if (ExtentTestManager.getTest() == null) {
            LOGGER.warn("ExtentTest instance is missing for current thread. Initializing default test: {}", testName);
            ExtentTestManager.startTest(testName, description);
        }
    }

    public void launchUrl() {
        if (globalProperties == null) {
            LOGGER.warn("globalProperties not loaded when calling launchUrl(). Attempting to load.");
            getGlobalProperties();
        }

        String applicationUrl = null;
        try {
            applicationUrl = globalProperties.getString("applicationURL");
        } catch (MissingResourceException e) {
            LOGGER.error("Property 'applicationURL' not found in global.properties. Cannot launch URL.", e);
            ExtentTestManager.logTestResult("FAIL", "Application URL is not set in global.properties.");
            Assert.fail("Application URL property missing.", e);
        }

        Page currentPage = getPage();

        if (currentPage == null) {
            LOGGER.error("Playwright Page instance is NULL within launchUrl! This indicates a problem with @BeforeMethod setup.");
            Assert.fail("Playwright Page is not initialized. Cannot launch URL.");
        }

        String env = "PROD"; // Default environment
        try {
            if (currentPage == null) {
                LOGGER.error("Page instance is not initialized! Critical error in launchUrl.");
                ExtentTestManager.logTestResult("FAIL", "Page instance is not initialized.");
                Assert.fail("Page instance is null. Cannot launch URL.");
                return;
            }

            if (applicationUrl == null || applicationUrl.isEmpty()) {
                LOGGER.error("Application URL is missing or empty! Cannot navigate.");
                ExtentTestManager.logTestResult("FAIL", "Application URL is empty or null.");
                Assert.fail("Application URL is empty or null.");
                return;
            }

            currentPage.navigate(applicationUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            LOGGER.info("Successfully navigated to {}", applicationUrl);

            // Determine environment from URL after successful navigation
            if (applicationUrl.contains("dev")) {
                env = "DEV";
            } else if (applicationUrl.contains("qa")) {
                env = "QA";
            } else if (applicationUrl.contains("test")) {
                env = "TEST";
            } else {
                env = "PROD";
            }

            ExtentTestManager.logTestResult("INFO", "Launched URL in " + env + " Environment: " + applicationUrl);
        } catch (PlaywrightException e) {
            LOGGER.error("Playwright operation failed during URL launch for thread {}: {}", Thread.currentThread().threadId(), e.getMessage(), e);
            ExtentTestManager.logTestResult("FAIL", "Launch URL failed due to Playwright error: " + e.getMessage());
            Assert.fail("Failed to launch URL with Playwright: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            LOGGER.error("Invalid state error during URL launch for thread {}: {}", Thread.currentThread().threadId(), e.getMessage(), e);
            ExtentTestManager.logTestResult("FAIL", "Invalid state error during URL launch: " + e.getMessage());
            Assert.fail("Invalid state during URL launch: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            LOGGER.error("Runtime error during URL launch for thread {}: {}", Thread.currentThread().threadId(), e.getMessage(), e);
            ExtentTestManager.logTestResult("FAIL", "Runtime error during URL launch: " + e.getMessage());
            Assert.fail("Runtime error during URL launch: " + e.getMessage(), e);
        }
    }

    protected static synchronized void getGlobalProperties() {
        if (globalProperties == null) {
            LOGGER.info("Loading global.properties for the first time.");
            try {
                globalProperties = ResourceBundle.getBundle("global");
                LOGGER.info("global.properties loaded successfully.");

                testDataSource = globalProperties.getString("test_data_source");
                reportPath = FunctionLibrary.readPathFromPropertyFile("reportPath");

                if (testDataSource == null || testDataSource.trim().isEmpty()) {
                    LOGGER.error("Property 'test_data_source' is missing or empty in global.properties.");
                    throw new IllegalStateException("Critical property 'test_data_source' is missing or empty.");
                }

                if ("excel".equalsIgnoreCase(testDataSource)) {
                    externalSheetPath = FunctionLibrary.readPathFromPropertyFile("external_sheet_path");
                    if (externalSheetPath == null || externalSheetPath.isEmpty()) {
                        LOGGER.error("Property 'external_sheet_path' is missing or empty for 'excel' data source.");
                        throw new IllegalStateException("Critical property 'external_sheet_path' is missing or empty for Excel data source.");
                    }
                    LOGGER.info("Test data source set to Excel. External sheet path: {}", externalSheetPath);
                } else {
                    LOGGER.error("Unsupported 'test_data_source' value: '{}'. Only 'excel' is currently supported.", testDataSource);
                    throw new IllegalArgumentException("Unsupported test data source: " + testDataSource);
                }

                if (reportPath == null || reportPath.isEmpty()) {
                    LOGGER.error("Property 'reportPath' is missing or empty in global.properties.");
                    throw new IllegalStateException("Critical property 'reportPath' is missing or empty.");
                } else {
                    LOGGER.info("Report path initialized: {}", reportPath);
                }

            } catch (MissingResourceException e) {
                LOGGER.error("Failed to load global.properties or a required key is missing: {}", e.getMessage(), e);
                throw new RuntimeException("Fatal: global.properties or a required key not found. " + e.getMessage(), e);
            } catch (SecurityException e) {
                LOGGER.error("Security error while loading global properties: {}", e.getMessage(), e);
                throw new RuntimeException("Security error during global properties setup: " + e.getMessage(), e);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Invalid configuration in global properties: {}", e.getMessage(), e);
                throw new RuntimeException("Invalid configuration in global properties: " + e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("Unexpected error while loading global properties: {}", e.getMessage(), e);
                throw new RuntimeException("Fatal error during global properties setup: " + e.getMessage(), e);
            }
        } else {
            LOGGER.debug("global.properties already loaded.");
        }
    }
}