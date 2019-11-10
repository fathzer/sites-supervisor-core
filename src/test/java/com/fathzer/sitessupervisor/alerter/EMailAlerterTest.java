package com.fathzer.sitessupervisor.alerter;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.sitessupervisor.alerter.EMailAlerter.ServiceParams;
import com.fathzer.sitessupervisor.Configuration.ServiceInfo;

public class EMailAlerterTest {
	@Test
	public void test() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> map = mapper.readValue("{'host':'smtp','from':'no-reply@renault.com'}".replace('\'', '"'), Map.class);
		EMailAlerter alerter = new EMailAlerter(map);
		map = mapper.readValue("{'to':['jean-marc.astesana@renault.com']}".replace('\'', '"'), Map.class);
		final ServiceParams obj = alerter.verify(map);
		alerter.alert(new ServiceInfo(URI.create("http://example.com"), "app", "env"), obj, "everything is broken");
	}
	
	@Test
	public void testEquality() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> map = mapper.readValue("{'host':'smtp','from':'no-reply@renault.com'}".replace('\'', '"'), Map.class);
		EMailAlerter alerter = new EMailAlerter(map);
		map = mapper.readValue("{'to':['jean-marc.astesana@renault.com','toto@titi.com']}".replace('\'', '"'), Map.class);
		final ServiceParams p1 = alerter.verify(map);
		assertEquals(2, p1.getTo().length);
		map = mapper.readValue("{'to':['toto@titi.com','jean-marc.astesana@renault.com']}".replace('\'', '"'), Map.class);
		final ServiceParams p2 = alerter.verify(map);
		assertEquals(2, p2.getTo().length);
		assertEquals(p1,p2);
	}


}
