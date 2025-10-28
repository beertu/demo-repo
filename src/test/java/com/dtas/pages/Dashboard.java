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
 * This class is for the methods used in Dashboard
 * @author Jan Robert
 */ 
public class Dashboard extends BasePage {
	String flag = "NA";

	
	private Locator addtoCart1;
	private Locator addtoCart2;
	private Locator Cart;
	
	public Dashboard(Page page) {
		super(page);
		// TODO Auto-generated constructor stub
		this.addtoCart1 = page.locator("//button[@id=\"add-to-cart-sauce-labs-backpack\"]");
		this.addtoCart2 = page.locator("//button[@name=\"add-to-cart-sauce-labs-bike-light\"]");
		this.Cart = page.locator("//div[@id=\"shopping_cart_container\"]");
	}

    /**
     * This method is to click on Add to Cart Button
     */ 
	public void clickOnAddToCart1(){
		try {
			
			FunctionLibrary.syncTill(page, "visibilityOfElement", addtoCart1);			
			FunctionLibrary.clickElement(page, addtoCart1);			
			
			ExtentTestManager.logTestResult("PASS", "Passed to click on 1st Add to Cart Button");
//			ExtentTestManager.takeScreenshot(page, "Pass", "User is now logged in");
			
		}catch(Exception e) {
			ExtentTestManager.logTestResult("INFO", org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			ExtentTestManager.logTestResult("FAILED", "Failed to click on 1st Add to Cart Button");
		}	
	}
	
    /**
     * This method is to click on Add to Cart Button
     */ 
	public void clickOnAddToCart2(){
		try {
			
			FunctionLibrary.syncTill(page, "visibilityOfElement", addtoCart2);			
			FunctionLibrary.clickElement(page, addtoCart2);
			
			ExtentTestManager.logTestResult("PASS", "Passed to click on 2nd Add to Cart Button");
//			ExtentTestManager.takeScreenshot(page, "Pass", "User is now logged in");
			
		}catch(Exception e) {
			ExtentTestManager.logTestResult("INFO", org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			ExtentTestManager.logTestResult("FAILED", "Failed to click on 2nd Add to Cart Button");
		}		
	}
	
    /**
     * This method is to click on Cart Button
     */ 
	public void clickOnCart() {
		try {		
			FunctionLibrary.syncTill(page, "visibilityOfElement", Cart);			
			FunctionLibrary.clickElement(page, Cart);			
			
			ExtentTestManager.logTestResult("PASS", "Passed to click on Cart Button");
//			ExtentTestManager.takeScreenshot(page, "Pass", "User is now logged in");
			
		}catch(Exception e) {
			ExtentTestManager.logTestResult("INFO", org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			ExtentTestManager.logTestResult("FAILED", "Failed to click on Cart Button");
		}		
	}
	
	
	
	
}