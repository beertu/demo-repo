package com.dtas.commons;

import static org.testng.Assert.assertEquals;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hc.client5.http.utils.Base64;
import org.apache.commons.io.FileUtils; // Correct import for Apache Commons IO FileUtils

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dtas.driverfactory.DriverSetup;
import com.dtas.report.ExtentTestManager;
import com.microsoft.playwright.BrowserContext.WaitForConditionOptions;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * A utility class providing reusable functions for test automation using Playwright.
 * This library includes methods for:
 * - UI interactions (click, type, select)
 * - Synchronization and waits
 * - Window/tab handling
 * - File upload operations
 * - Alert handling
 * - Property file operations
 * - Path management
 * - Database operations (stubbed)
 * - File system operations
 * 
 * The class is designed to work with Playwright's Page and Locator objects,
 * providing convenient wrappers around common automation tasks.
 *
 * @author Jan Robert Bodino
 */
public class FunctionLibrary {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionLibrary.class); // Correct logger init

    /**
     * Waits for a specified condition on a page element to be satisfied.
     * This method provides different wait strategies based on the condition specified.
     * 
     * @param page The Playwright Page object to perform the wait on
     * @param condition The wait condition to apply. Valid values are:
     *                 - "visibilityOfElement": Wait for element to be visible
     *                 - "invisibilityOfElement": Wait for element to be hidden
     *                 - "elementToBeClickable": Wait for element to be editable
     *                 - "alertPresent": Wait for an alert role to be present
     * @param elem The Playwright Locator object representing the element to wait for
     * @throws RuntimeException if the wait condition times out
     */
    public static void syncTill(Page page, String condition, Locator elem) {
        // Removed unused pTimeOut variable and simplified.
        if (condition.equalsIgnoreCase("visibilityOfElement")) {
            page.waitForCondition(() -> elem.isVisible());
        } else if (condition.equalsIgnoreCase("invisibilityOfElement")) {
            page.waitForCondition(() -> elem.isHidden());
        } else if (condition.equalsIgnoreCase("elementToBeClickable")) {
            page.waitForCondition(() -> elem.isEditable());
        } else if (condition.contentEquals("alertPresent")) {
            // Note: This only checks if the alert role is visible, not a true alert handler.
            // Playwright's page.onDialog is typically used for actual alert handling.
            page.getByRole(AriaRole.ALERT).isVisible();
        }
    }

    /**
     * This function used to wait period of time as declared
     */
    public static void syncTillTimePeriod(int t) {
        try {
            Thread.sleep(t * 1000);
        } catch (InterruptedException e) {
            LOGGER.error("Wait time out error", e); // Log the exception for better debugging
            Thread.currentThread().interrupt(); // Restore the interrupted status
        }
    }

    /**
     * This function is time out page process. Time as integer
     * timeout as milliseconds
     */
    public static void syncTillPageTime(Page page, int time) {
        page.waitForTimeout(time * 1000);
    }

    /**
     * This function used to clear and set text in the element.
     */
    public static void enterValueInTextBox(Page page, Locator element, String txt) {
        syncTill(page, "visibilityOfElement", element);
        syncTill(page, "elementToBeClickable", element);
        element.hover(); // Optional, but can improve reliability for some elements
        element.clear();
        element.fill(txt);
    }

    /**
     * This function used to type text in the element.
     */
    public static void typeValueInTextBox(Page page, Locator element, String txt) {
        syncTill(page, "visibilityOfElement", element);
        syncTill(page, "elementToBeClickable", element);
        element.hover();
        element.click();
        element.clear(); // Playwright's fill is generally preferred over keyboard.type + clear
        page.keyboard().type(txt); // Still works, but fill is often more robust
    }

    /**
     * This function used to click an element.
     */
    public static void clickElement(Page page, Locator element) {
        syncTill(page, "elementToBeClickable", element);
        element.hover(); // Optional, but can improve reliability
        element.click();
    }

    /**
     * This function used to double click an element.
     */
    public static void dblClickElement(Page page, Locator element) {
        syncTill(page, "elementToBeClickable", element);
        element.hover();
        element.dblclick();
    }

    /**
     * This function is to switch the page from Current Window/Tab to newly opened
     * Window/Tab. Element should be click.
     */
    public static Page goToNewPage(Page page, Locator element, int tab) {
        element.hover();
        Page newPage = page.context().waitForPage(() -> { element.click(); });
        syncTillTimePeriod(5); // Consider using page.waitForLoadState() or more specific waits
        newPage.waitForLoadState();

        List <Page> pagesX = newPage.context().pages();
        System.out.println("Number of pages: " + pagesX.size());

        // Ensure the tab index is valid
        if (tab >= 0 && tab < pagesX.size()) {
            newPage = pagesX.get(tab);
            System.out.println("New Tab/Window URL: " + newPage.url());
            System.out.println("New Tab/Window Title: " + newPage.title());
        } else {
            LOGGER.warn("Attempted to switch to tab index {} but only {} pages are open. Staying on current page.", tab, pagesX.size());
        }

        // It's generally better to return the newPage and let the calling code
        // update its 'page' reference, rather than directly reassigning the
        // passed 'page' parameter (which doesn't affect the caller's variable).
        return newPage;
    }

    /**
     * This function is to switch pages by Index. 0 as default
     */
    public static Page goToPage(Page page, int tab) {
        // No need for 'num' variable, directly use 'tab' after validation
        List <Page> pagesX = page.context().pages();
        System.out.println("Number of pages: " + pagesX.size());

        if (tab >= 0 && tab < pagesX.size()) {
            page = pagesX.get(tab);
            System.out.println("Switched to Tab/Window URL: " + page.url());
            System.out.println("Switched to Tab/Window Title: " + page.title());
        } else {
            LOGGER.warn("Attempted to switch to tab index {} but only {} pages are open. Remaining on current page.", tab, pagesX.size());
            // You might want to throw an exception here if an invalid tab index is critical.
        }
        return page;
    }

    /**
     * This function used to upload files
     */
    public static void uploadFiles(Page page, Locator element, String path) {
        syncTill(page, "elementToBeClickable", element);

        FileChooser fC = page.waitForFileChooser(() -> element.click());
        fC.setFiles(Paths.get(path));

        // Other way to upload files using playwright
        // page.locator("//input[@type=\"file\"]").setInputFiles(Paths.get(FunctionLibrary.readPathFromPropertyFile("uploadPath")));
        // page.setInputFiles("//input[@type=\"file\"]", Paths.get(FunctionLibrary.readPathFromPropertyFile("uploadPath")));
    }

    /**
     * This function is to verify text contains from webelement.
     * Added  - 08242022
     */
    public static boolean verifyTextContainsFromWebelement(Locator element, String textToVerify) {
        element.hover();
        String elementText = element.textContent();
        return elementText.contains(textToVerify) || elementText.equalsIgnoreCase(textToVerify);
    }

    /**
     * This function is to verify text from text box.
     */
    public static boolean verifyTextValueFromTextBox(Locator element, String textToVerify) {
        element.hover();
        String elementValue = element.getAttribute("value");
        return elementValue != null && elementValue.equalsIgnoreCase(textToVerify);
    }

    /**
     * Selects an option from a dropdown element based on specified conditions.
     *
     * @param page The Playwright Page object
     * @param conditions The selection strategy to use:
     *                  - "VALUE": Select by option value
     *                  - "LABEL": Select by visible text/label
     *                  - "INDEX": Select by numeric index (0-based)
     * @param element The Playwright Locator object for the dropdown element
     * @param valueToSelect The value to select based on the condition:
     *                     - For VALUE: the option's value attribute
     *                     - For LABEL: the option's visible text
     *                     - For INDEX: a numeric index as string
     * @throws IllegalArgumentException if the condition is not supported or
     *         if an invalid index is provided for INDEX condition
     */
    public static void selectOptions(Page page, String conditions, Locator element, String valueToSelect) {
        syncTill(page, "visibilityOfElement", element);
        element.hover();
        
        try {
            switch (conditions.toUpperCase()) {
                case "VALUE" -> element.selectOption(valueToSelect);
                case "LABEL" -> element.selectOption(new SelectOption().setLabel(valueToSelect));
                case "INDEX" -> {
                    try {
                        element.selectOption(new SelectOption().setIndex(Integer.parseInt(valueToSelect)));
                    } catch (NumberFormatException e) {
                        String msg = "Invalid index '" + valueToSelect + "' provided for selectOptions";
                        LOGGER.error(msg, e);
                        throw new IllegalArgumentException(msg, e);
                    }
                }
                default -> {
                    String msg = "Unsupported select option condition: " + conditions;
                    LOGGER.error(msg);
                    throw new IllegalArgumentException(msg);
                }
            }
            LOGGER.debug("Successfully selected option '{}' by {} from dropdown", valueToSelect, conditions);
        } catch (Exception e) {
            LOGGER.error("Failed to select option '{}' by {} from dropdown: {}", 
                valueToSelect, conditions, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Accept any alert if present.
     */
    public static void acceptAlert(Page page) {
        page.onDialog(dlg -> {
            LOGGER.info("Alert with message: '{}' and type: '{}' detected. Accepting.", dlg.message(), dlg.type());
            dlg.accept();
        });
        // The line below should only be used if there's an explicit button to click *after* a dialog
        // is handled, and it's not part of the dialog itself.
        // page.getByRole(AriaRole.BUTTON).click();
        LOGGER.info("Alert accept handler set."); // This message indicates the handler is set, not that an alert was accepted yet.
    }

    /**
     * Get Browser Name
     */
    public static String GetBrosweName(Page page) {
        if (page != null && page.context() != null && page.context().browser() != null) {
            return page.context().browser().browserType().name() + " v:" + page.context().browser().version();
        }
        return "Unknown Browser";
    }

    /**
     * Writes test execution results to a CSV log file.
     * The CSV format is: "Nuclear IT, result, timestamp, application"
     *
     * @param application The name of the application being tested
     * @param result The test execution result
     * @throws FileNotFoundException if the log file path cannot be found or created
     */
    public static void writeToCsv(String application, String result) throws FileNotFoundException {
        String logPath = readPathFromPropertyFile("csvLogPath"); // Get path from properties
        if (logPath == null || logPath.trim().isEmpty()) {
            LOGGER.error("CSV log path is not configured in properties");
            throw new FileNotFoundException("CSV log path is not configured");
        }

        try (FileWriter fileWriter = new FileWriter(logPath, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
             PrintWriter printWriter = new PrintWriter(bufferedWriter)) {

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            printWriter.println("Nuclear IT, " + result + "," + timestamp + "," + application);
            LOGGER.info("Successfully wrote log entry for application: {}", application);

        } catch (IOException e) {
            LOGGER.error("Error writing to CSV file '{}': {}", logPath, e.getMessage(), e);
            throw new RuntimeException("Failed to write to CSV log", e);
        }
    }

    /**
     * Function to decode the Base64 encrypted password from properties file.
     * @param cipherText : password value encrypted with Base64 encryption kept in
     * properties file
     * @return : plain text which is the actual text.
     */
    public static String base64Decoder(String cipherText) {
        byte[] byteArray = Base64.decodeBase64(cipherText.getBytes());
        String decodedString = new String(byteArray);
        return decodedString;
    }

//-- Function Library for Database Scripts (all commented out currently)
    public static Connection connectTruncateDB() {
        return null; // Return null as logic is commented out
    }

    public static Connection insertDashboardLogs(int appID, LocalDateTime startTime, LocalDateTime endTime, String status) {
        return null; // Return null as logic is commented out
    }

    public static Connection insertDashboardErrorLogs(String msg) {
        return null; // Return null as logic is commented out
    }

    public static Connection callStoredProcedure() {
        return null; // Return null as logic is commented out
    }

//-- Function Library for Configurations
    /**
     * This function is to read and get value in property file.
     */
    public static String readPropertyValue(String key) {
        try {
            return ResourceBundle.getBundle("global").getString(key);
        } catch (MissingResourceException e) {
            LOGGER.error("Property key '{}' not found in global.properties.", key, e);
            throw new RuntimeException("Missing property: " + key, e);
        }
    }

    /**
     * This function is get all resource in global properties
     */
    public static ResourceBundle getResourceBundle() {
        try {
            return ResourceBundle.getBundle("global");
        } catch (MissingResourceException e) {
            LOGGER.error("global.properties bundle not found.", e);
            throw new RuntimeException("Cannot find global.properties.", e);
        }
    }

    /**
     * This function is to delete directory.
     */
    public static void deleteDirectory(String directoryName) {
        File directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdir(); // Create the directory if it doesn't exist
            LOGGER.info("Directory created: {}", directoryName);
            return; // No need to clean if just created
        }

        if (!directory.isDirectory()) {
            LOGGER.error("Provided path {} is not a directory. Cannot clean.", directoryName);
            return;
        }

        try {
            FileUtils.cleanDirectory(directory); // Apache Commons IO method
            LOGGER.info("Directory cleaned: {}", directoryName);
        } catch (IOException e) {
            LOGGER.error("Failed to clean directory {}: {}", directoryName, e.getMessage(), e);
            // Consider throwing a runtime exception if this is critical for test setup
        }
    }

    /**
     * This function is to get user home path.
     */
    public static String getUserHomePath() {
        String path = null;
        try {
            path = System.getProperty("user.dir");
            // This part is for when running from target/classes or target/test-classes
            // It tries to go back to the project root.
            if (path.contains(File.separator + "target")) {
                path = path.substring(0, path.lastIndexOf(File.separator + "target"));
            }
        } catch (Exception e) { // Catch Throwable as before, but Exception is usually sufficient
            LOGGER.error("Failed to get user home path", e);
            throw new RuntimeException("Failed to determine user home path.", e); // Re-throw to indicate critical failure
        }
        return path;
    }

    /**
     * This function is to read property from property file and construct a full absolute path.
     * It combines the project's root directory with the relative path from the properties.
     */
    public static String readPathFromPropertyFile(String pathKey) {
        String fullPath = null;
        try {
            String relativePathValue = readPropertyValue(pathKey); // Get the relative path value from global.properties
            String userHome = getUserHomePath(); // Get the project root path (e.g., C:\Users\Jan Robert\Desktop\Test-Automation\Outage-Playwright\sg653-dtas-Outage-Playwright)

            if (relativePathValue == null || relativePathValue.trim().isEmpty()) {
                LOGGER.warn("Property '{}' is empty or not found in global.properties. Returning null for path.", pathKey);
                return null;
            }
            if (userHome == null || userHome.trim().isEmpty()) {
                LOGGER.error("User home path is null or empty. Cannot construct full path for '{}'. Returning null.", pathKey);
                return null;
            }

            // Use java.io.File to correctly combine paths, handling separators and relative parts
            // This ensures cross-platform compatibility and avoids manual string splitting that could cause IndexOutOfBounds.
            File combinedPath = new File(userHome, relativePathValue);
            fullPath = combinedPath.getAbsolutePath(); // Get the absolute path

            LOGGER.info("Constructed path for '{}': {}", pathKey, fullPath);

        } catch (MissingResourceException e) {
            LOGGER.error("Property key '{}' not found in global.properties. Please ensure it is defined.", pathKey, e);
            // This is a critical configuration issue, re-throwing is appropriate.
            throw new RuntimeException("Missing property '" + pathKey + "' in global.properties.", e);
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred while reading path from property file for key '{}': {}", pathKey, e.getMessage(), e);
            throw new RuntimeException("Failed to construct path for '" + pathKey + "'.", e);
        }
        return fullPath;
    }
}