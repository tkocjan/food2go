package com.github.hippoom.food2go.features;

import static com.github.hippoom.test.dbunit.DatabaseOperationBuilder.flatXml;
import static org.dbunit.operation.DatabaseOperation.DELETE_ALL;
import static org.dbunit.operation.DatabaseOperation.INSERT;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.sql.DataSource;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.github.hippoom.food2go.domain.model.order.Address;
import com.github.hippoom.food2go.domain.model.order.PendingOrderFixture;
import com.github.hippoom.test.dbunit.DatabaseOperationBuilder;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@Slf4j
public class PlaceOrderSteps implements ApplicationContextAware {
	@Setter
	private ApplicationContext applicationContext;
	@Autowired
	private DataSource dataSource;

	private Address deliveryAddress = PendingOrderFixture
			.defaultDeliveryAddressFixture().build();
	private String deliveryTime = nextSaturday(9, 00);
	private Content content;
	private String trackingId;

	@Given("^restaurants ready$")
	public void restaurants_ready() throws Throwable {
		refreshAvailableRestaurants();
	}

	@When("^I submit delivery address and delivery time$")
	public void I_submit_delivery_address_and_delivery_time() throws Throwable {
		/*
		 * see
		 * http://stackoverflow.com/questions/18256423/how-to-handle-redirect
		 * -by- httpclient-fluent/18271327?noredirect=1#18271327
		 */
		DefaultHttpClient client = new DefaultHttpClient();
		client.setRedirectStrategy(new LaxRedirectStrategy());
		Executor exec = Executor.newInstance(client);
		Response response = exec.execute(Request.Post(
				"http://localhost:9999/food2go/booking/placeOrder").bodyForm(
				param("deliveryAddressStreet1", deliveryAddress.getStreet1()),
				param("deliveryAddressStreet2", deliveryAddress.getStreet2()),
				param("deliveryTime", deliveryTime)));
		content = response.returnContent();
		log.debug(content.toString());
	}

	private BasicNameValuePair param(String name, String value) {
		return new BasicNameValuePair(name, value);
	}

	@Then("^an pending order is placed$")
	public void an_pending_order_is_placed() throws Throwable {
		String contentString = content.asString();
		assertThat(contentString, containsString("TrackingId(value="));
		trackingId = contentString.substring(
				contentString.indexOf("TrackingId(value=") + 17,
				contentString.indexOf(")") - 1);

		System.err.println("trackingId=" + trackingId);
	}

	@Then("^all available restaurants are listed$")
	public void all_available_restaurants_are_listed() throws Throwable {
		assertThat(content.asString(), containsString("Available restaurants:"));
	}

	@Given("^my delivery address is not in service area of any restaurant$")
	public void my_delivery_address_is_not_in_service_area_of_any_restaurant()
			throws Throwable {
		this.deliveryAddress = new Address("Mars", "Mercury");
	}

	@Then("^an pending order is not placed$")
	public void an_pending_order_is_not_placed() throws Throwable {
		assertThat(content.asString(),
				containsString("no restaurant available"));
	}

	@Given("^my delivery time is available of any restaurant$")
	public void my_delivery_time_is_available_of_any_restaurant()
			throws Throwable {
		this.deliveryTime = tomorrow(21, 00);
	}

	private String tomorrow(int hour, int min) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, 1);
		set(c, hour, min);
		return format(c);
	}

	private String format(Calendar c) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(c.getTime());
	}

	private String nextSaturday(int hour, int min) {
		Calendar c = Calendar.getInstance();
		int weekday = c.get(Calendar.DAY_OF_WEEK);
		if (weekday != Calendar.SATURDAY) {
			// calculate how much to add
			// the 2 is the difference between Saturday and Monday
			int days = (Calendar.SATURDAY - weekday) % 7;
			c.add(Calendar.DAY_OF_YEAR, days);
		}
		set(c, hour, min);
		return format(c);
	}

	private void set(Calendar c, int hour, int min) {
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, min);
	}

	private void refreshAvailableRestaurants() throws Exception {
		// It's convenient to manipulate the database directly, but it is
		// also vulnerable if the schema changes.
		// But I haven't develop restaurant admin feature yet, so it's the only
		// option in the meantime
		String file = "classpath:t_f2g_restaurant_place_order.xml";
		new DatabaseOperationBuilder(dataSource)
				.to(DELETE_ALL, flatXml(file(file)))
				.to(INSERT, flatXml(file(file))).execute();
	}

	private File file(String file) throws IOException {
		return applicationContext.getResource(file).getFile();
	}

	@When("^I pick desired menu items from a restaurant$")
	public void I_pick_desired_menu_items_from_a_restaurant() throws Throwable {
		assertThat(
				Request.Get(
						"http://localhost:9999/food2go/booking/restaurant/3")
						.execute().returnContent().asString()
						.contains("menuItems"), is(true));
		DefaultHttpClient client = new DefaultHttpClient();
		client.setRedirectStrategy(new LaxRedirectStrategy());
		Executor exec = Executor.newInstance(client);
		Response response = exec.execute(Request.Post(
				"http://localhost:9999/food2go/booking/order/" + trackingId
						+ "/updateOrderLines").bodyForm(
				param("restaurantId", "3"),
				param("orderLines[0].name", "Grilled beef"),
				param("orderLines[0].price", "20.00"),
				param("orderLines[0].quantity", "1")));
		content = response.returnContent();
	}

	@Then("^order items are updated$")
	public void order_items_are_updated() throws Throwable {
		String contentString = content.asString();
		assertThat(contentString, containsString("TrackingId(value="));
	}
}
