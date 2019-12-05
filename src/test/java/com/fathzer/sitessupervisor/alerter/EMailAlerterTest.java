package com.fathzer.sitessupervisor.alerter;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.sitessupervisor.alerter.EMailAlerter.ServiceParams;

public class EMailAlerterTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final EMailAlerter alerter;
	
	static {
		try {
			Map<String,Object> map = MAPPER.readValue("{'host':'smtp.gmail.com','from':'no-reply@example.com'}".replace('\'', '"'), Map.class);
			alerter = new EMailAlerter(map);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Test
	public void test() throws IOException {
		final Map<String,Object> map = MAPPER.readValue("{'to':['jean-marc.astesana@example.com']}".replace('\'', '"'), Map.class);
		final ServiceParams obj = alerter.verify(map);
		assertEquals(1, obj.getTo().length);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNoArg() throws IOException {
		new EMailAlerter(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoServiceArg() throws IOException {
		alerter.verify(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongServiceArg() throws IOException {
		final Map<String,Object> map = MAPPER.readValue("{'tos':['jean-marc.astesana@example.com']}".replace('\'', '"'), Map.class);
		alerter.verify(map);
	}
	
	@Test
	public void testEquality() throws IOException {
		Map<String,Object> map = MAPPER.readValue("{'to':['jean-marc.astesana@example.com','toto@titi.com']}".replace('\'', '"'), Map.class);
		final ServiceParams p1 = alerter.verify(map);
		assertEquals(2, p1.getTo().length);
		map = MAPPER.readValue("{'to':['toto@titi.com','jean-marc.astesana@example.com']}".replace('\'', '"'), Map.class);
		final ServiceParams p2 = alerter.verify(map);
		assertEquals(2, p2.getTo().length);
		assertEquals(p1,p2);
	}
}
