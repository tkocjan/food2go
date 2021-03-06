package com.github.hippoom.food2go.features;

import org.junit.runner.RunWith;

import cucumber.api.junit.Cucumber;

/**
 * <pre>
 * All acceptance tests run against the web application 
 * through http requests rather than the view directly.
 * 
 * This testing strategy is base on:
 * 
 * a) Test fixture based on view is expensive and fragile 
 *    even if you use some handy framework such as WebDriver.
 * 
 * b) Few logics are placed in the view.
 * </pre>
 * 
 */
@RunWith(Cucumber.class)
@Cucumber.Options(features = { "classpath:features/." }, format = { "html:target/acceptance-cucumber-html-report" })
public class AcceptanceTests {

}
