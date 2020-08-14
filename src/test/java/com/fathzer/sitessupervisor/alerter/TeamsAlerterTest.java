package com.fathzer.sitessupervisor.alerter;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.sitessupervisor.alerter.TeamsAlerter.ServiceParams;

public class TeamsAlerterTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final TeamsAlerter alerter = new TeamsAlerter(Collections.emptyMap());
	
	@Test
	public void test() throws IOException {
		final Map<String,Object> map = MAPPER.readValue("{'hook':'http://example.com'}".replace('\'', '"'), Map.class);
		final ServiceParams obj = alerter.verify(map);
		assertEquals(URI.create("http://example.com"), obj.getWebhook());
	}
	
	public void testNoArg() {
		new TeamsAlerter(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoServiceArg() {
		alerter.verify(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoHook() {
		alerter.verify(Collections.emptyMap());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongServiceArg() throws IOException {
		final Map<String,Object> map = MAPPER.readValue("{'hooks':'http://example.com'}".replace('\'', '"'), Map.class);
		alerter.verify(map);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testWrongServiceArg2() throws IOException {
		final Map<String,Object> map = MAPPER.readValue("{'hook':['http://example.com']}".replace('\'', '"'), Map.class);
		alerter.verify(map);
	}
	
	@Test
	public void testEquality() throws IOException {
		Map<String,Object> map = MAPPER.readValue("{'hook':'http://example.com'}".replace('\'', '"'), Map.class);
		final ServiceParams p1 = alerter.verify(map);
		map = MAPPER.readValue("{'hook':'http://example.com'}".replace('\'', '"'), Map.class);
		final ServiceParams p2 = alerter.verify(map);
		assertEquals(p1,p2);
	}
}
