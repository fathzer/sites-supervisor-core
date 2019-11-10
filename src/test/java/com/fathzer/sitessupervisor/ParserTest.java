package com.fathzer.sitessupervisor;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.junit.Test;

import com.fathzer.sitessupervisor.alerter.FakeAlerter;
import com.fathzer.sitessupervisor.db.FakeDB;
import com.fathzer.sitessupervisor.parsing.JSONParser;
import com.fathzer.sitessupervisor.tester.FakeTester;
import com.fathzer.sitessupervisor.Configuration;
import com.fathzer.sitessupervisor.Configuration.Service;

public class ParserTest {
	private static final Configuration CONF;
	
	static {
		try (InputStream stream=ParserTest.class.getResourceAsStream("/testConfiguration.json")) {
			CONF = new JSONParser().parseConfiguration(stream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void test() throws IOException {
		assertEquals(FakeDB.class, CONF.getDatabase().getClass());
		assertEquals(1, CONF.getAlerters().size());
		assertEquals(FakeAlerter.class, CONF.getAlerters().get("mail").getClass());
		assertEquals(1, CONF.getTesters().size());
		assertEquals(FakeTester.class, CONF.getTesters().get("tester").getClass());
		try (InputStream stream=ParserTest.class.getResourceAsStream("/testServices.json")) {
			final Collection<Service> services = new JSONParser().parseServices(stream, CONF);
			System.out.println(services);
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNoURIInService() throws IOException {
		final String json = "{\"services\":[{\"tester\":{\"name\":\"tester\"}}]}";
		new JSONParser().parseServices(new ByteArrayInputStream(json.getBytes()) , CONF);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNoTesterService() throws IOException {
		final String json = "{\"services\":[{\"uri\":\"http://www.example.com\"}]}";
		new JSONParser().parseServices(new ByteArrayInputStream(json.getBytes()) , CONF);
	}
}

