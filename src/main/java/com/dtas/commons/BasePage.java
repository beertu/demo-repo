package com.dtas.commons;

import com.microsoft.playwright.Page;

/**
 * Lightweight base class for page objects.
 *
 * <p>Holds a reference to the Playwright {@link Page} instance. Concrete page
 * object classes should extend this so they can access the underlying page to
 * perform actions (click, type, navigate, etc.).</p>
 */
public class BasePage {
    
    /** The Playwright Page used by this page object. */
    public Page page;
    
    /**
     * Create a new BasePage bound to the given Playwright {@link Page}.
     *
     * @param page Playwright Page instance for browser interactions
     */
    public BasePage(Page page) {
        this.page = page;
    }
    
    /**
     * Return the Playwright {@link Page} tied to this page object.
     *
     * @return current Playwright Page
     */
    public Page getPage() {
        return page;
    }

    /**
     * Replace the Playwright Page instance for this page object.
     * Useful when tests reuse page objects across runs or threads.
     *
     * @param page new Playwright Page instance
     */
    public void setPage(Page page) {
        this.page = page;
    }
}
