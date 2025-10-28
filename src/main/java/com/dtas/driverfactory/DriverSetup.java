package com.dtas.driverfactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.options.Proxy;

import java.util.ResourceBundle;

// Remove all Playwright and Selenium WebDriver imports if they are not used elsewhere.
// import com.microsoft.playwright.*;
// import org.openqa.selenium.*; // etc.

/**
 * Class for general driver configurations (non-Playwright/Selenium specific if not used).
 * Currently, no Playwright/Selenium driver initialization is handled here.
 * @author Jan Robert
 */
public class DriverSetup {

    // Using the class name for logger initialization is generally better.
    private static final Logger LOGGER = LoggerFactory.getLogger(DriverSetup.class);

    // If these are only used for Selenium Grid, they can be removed if not using Selenium.
    // private static final String SETTING_GRID_URL = "hubUrl";
    // private static final String SETTING_GRID_PROXY = "hubBrowserProxyUrl";

    // REMOVE these static Playwright fields as they are managed by BaseTest's ThreadLocal
    // public static Browser browser = null;
    // public static BrowserContext context = null;
    // public static Page page = null;
    // public static Playwright playwright = null;

    // REMOVE the startPage method completely as its logic is in BaseTest's @BeforeMethod
    // public static Page startPage(String browserType) { ... }

    // REMOVE these getters if not used by any other part of the framework
    // public static Playwright getPlaywright() { return playwright; }
    // public static Browser getBrowser() { return browser; }
    // public static BrowserContext getContext() { return context; }

    /**
     * Return Proxy object if settings require it.
     * Keep this only if you intend to use it for *other* network configurations, not Playwright.
     * Playwright has its own proxy settings in BrowserType.LaunchOptions or Browser.NewContextOptions.
     * @param props properties to look in for hubBrowserProxyUrl
     * @return null if no proxy set, proxy object if proxy url found
     */
    private static Proxy getProxyIfAvailable(ResourceBundle props) {
        // If you're not using Selenium Grid anymore, this method is likely obsolete.
        // It's not compatible with Playwright's proxy setup.
        // If it's for something else, keep it, but remove the Selenium 'Proxy' import.
        return null; // Return null or remove if completely obsolete
    }
}