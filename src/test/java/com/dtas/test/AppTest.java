package com.dtas.test;

import com.dtas.commons.FunctionLibrary;
import com.dtas.driverfactory.BaseTest;
import com.dtas.pages.Dashboard;
import com.dtas.pages.LoginPage;
import com.microsoft.playwright.Page;


/**
 * This is a Fleet specific BaseTest class.
 */
public class AppTest extends BaseTest {

	Dashboard dashboard;
	LoginPage loginPage;
    
    public AppTest() {
        super();
    }

    /**
     * Used to pre-create instances of pages our tests need.
     * 
     * @param driver web driver we are using
     */
    public void setupPages(Page page) {

    	dashboard = new Dashboard(page);
    	loginPage = new LoginPage(page);
    	
    }

    /**
     * Login method that allows you to use data keys.
     * 
     * @param usernameKey : User name value
     * @param passwordKey : Password Value
     */
//    public void login(String usernameKey, String passwordKey) {
//    	aicLogin.login(usernameKey, passwordKey);
//    	FunctionLibrary.syncTillTimePeriod(10);
//    	
//    }
 
//    /**
//     * Method is to go to Main Window/Tab
//     */
//    public void mainWindow1() {
//    	page = FunctionLibrary.goToPage(page, 1);
//    	BaseTest.setPage(page);
//    }

}
