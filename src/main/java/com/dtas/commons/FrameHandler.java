package com.dtas.commons;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to handle iframes in Playwright.
 * Provides methods to switch to frames and verify frame content.
 * @author Jan Robert Bodino
 */
public class FrameHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrameHandler.class);
    private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds
    private static final int RETRY_ATTEMPTS = 3;
    
    /**
     * Switches to a frame by its name or id.
     * Uses retry mechanism with fixed delay.
     * @param page The current page
     * @param frameNameOrId The frame name or ID to switch to
     * @return The frame if found and switched successfully, null otherwise
     */
    public static Frame switchToFrame(Page page, String frameNameOrId) {
        LOGGER.info("Attempting to switch to frame: {}", frameNameOrId);
        
        for (int attempt = 0; attempt < RETRY_ATTEMPTS; attempt++) {
            try {
                // Wait for frame to be present
                page.waitForSelector("iframe[name='" + frameNameOrId + "'], iframe#" + frameNameOrId, 
                    new Page.WaitForSelectorOptions().setTimeout(DEFAULT_TIMEOUT));

                Frame foundFrame = page.frame(frameNameOrId);
                if (foundFrame != null) {
                    // Verify frame is accessible
                    foundFrame.waitForLoadState();
                    LOGGER.info("Successfully switched to frame: {}", frameNameOrId);
                    return foundFrame;
                }
                
                LOGGER.warn("Frame {} not found on attempt {}", frameNameOrId, attempt + 1);
                // Use page.waitForTimeout instead of Thread.sleep for better stability
                page.waitForTimeout(2000); // 2 seconds delay between attempts
            } catch (TimeoutError te) {
                LOGGER.warn("Timeout while switching to frame {} on attempt {}: {}", 
                    frameNameOrId, attempt + 1, te.getMessage());
            } catch (PlaywrightException pe) {
                LOGGER.warn("Playwright error while switching to frame {} on attempt {}: {}", 
                    frameNameOrId, attempt + 1, pe.getMessage());
            }
        }
        
        LOGGER.error("Failed to switch to frame {} after {} attempts", frameNameOrId, RETRY_ATTEMPTS);
        return null;
    }

    /**
     * Waits for a frame to be present and verifies its content.
     * @param page The current page
     * @param frameNameOrId Frame name or ID
     * @param verificationSelector A selector that should exist in the frame
     * @return True if frame content verification succeeds
     */
    public static boolean verifyFrameContent(Page page, String frameNameOrId, String verificationSelector) {
        try {
            Frame frame = switchToFrame(page, frameNameOrId);
            if (frame != null) {
                frame.waitForSelector(verificationSelector,
                    new Frame.WaitForSelectorOptions().setTimeout(DEFAULT_TIMEOUT));
                PlaywrightAssertions.assertThat(frame.locator(verificationSelector)).isVisible();
                LOGGER.info("Successfully verified content in frame: {}", frameNameOrId);
                return true;
            }
        } catch (TimeoutError te) {
            LOGGER.error("Timeout while verifying frame content for {}: {}", frameNameOrId, te.getMessage());
        } catch (PlaywrightException pe) {
            LOGGER.error("Playwright error while verifying frame content for {}: {}", frameNameOrId, pe.getMessage());
        }
        return false;
    }
}