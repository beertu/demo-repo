package com.dtas.pages;

import static org.testng.Assert.assertEquals;
import org.apache.commons.codec.binary.Base64;
import com.dtas.commons.BasePage;
import com.dtas.commons.FunctionLibrary;
import com.dtas.driverfactory.BaseTest;
import com.dtas.report.ExtentTestManager;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * This class consists of login methods
 * @author Jan Robert
 */ 
public class LoginPage extends BasePage {
	String flag = "NA";
	private Locator userName;
	private Locator password;
	private Locator loginButton;
	private Locator addtoCart1;
	
	
	public LoginPage(Page page) {
		super(page);
		// TODO Auto-generated constructor stub
		this.userName = page.locator("//input[@id='user-name']");
		this.password = page.locator("//input[@id='password']");
		this.loginButton = page.locator("//input[@id=\"login-button\"]");
		this.addtoCart1 = page.locator("//button[@id=\"add-to-cart-sauce-labs-backpack\"]");
	}

    /**
     * This method is to login user
     */ 
	public void loginUser (){
		try {
			FunctionLibrary.syncTill(page, "visibilityOfElement", userName);
			FunctionLibrary.enterValueInTextBox(page, userName, "standard_user");
			
			FunctionLibrary.syncTill(page, "visibilityOfElement", password);
			FunctionLibrary.enterValueInTextBox(page, password, "secret_sauce");
			
			FunctionLibrary.clickElement(page, loginButton);
						
			ExtentTestManager.logTestResult("PASS", "User Successfully Logged-in");
			ExtentTestManager.takeScreenshot(page, "Pass", "User is now logged in");
			
		}catch(Exception e) {
			ExtentTestManager.logTestResult("INFO", org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			ExtentTestManager.logTestResult("FAILED", "wrong username or password");
		}	
	}
	
	
	
	
}