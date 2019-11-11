package com.fathzer.sitessupervisor.tester;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fathzer.sitessupervisor.tester.BasicHttpTester.ServiceParams;

public class BasicHttpTesterTest {

	@Test
	public void testEquality() {
		Tester<ServiceParams> tester = new BasicHttpTester(null);
		Map<String, Object> params = new HashMap<>();
		Map<String, Object> headers = new HashMap<>();
		headers.put("apikey", "toto");
		headers.put("bla", "titi");
		params.put("headers", headers);
		final ServiceParams p1 = tester.verify(params);
		params.clear();
		headers.clear();
		assertNotNull(p1);
		assertNotNull(p1.getHeaders());
		assertEquals(2,p1.getHeaders().size());
		assertEquals("toto", p1.getHeaders().get("apikey"));

		headers.put("bla", "titi");
		params.put("headers", headers);
		headers.put("apikey", "toto");
		final ServiceParams p2 = tester.verify(params);
		assertEquals(p1, p2);
}

	@Test
	public void test() {
		final Map<String, Object> map = new HashMap<>();
		map.put("proxy", "127.0.0.1:3218");
		map.put("noProxy",".example.com");
		BasicHttpTester tester = new BasicHttpTester(map);
		final Map<String, Object> params = new HashMap<>();
		final Map<String, Object> headers = new HashMap<>();
		headers.put("apikey", "toto");
		params.put("headers", headers);
		final ServiceParams p = tester.verify(params);
		assertNotNull(p);
		assertNotNull(p.getHeaders());
		assertEquals(1,p.getHeaders().size());
		assertEquals("toto", p.getHeaders().get("apikey"));
		assertFalse(tester.isProxyRequired(URI.create("http://www.example2.com"), p));
		assertTrue(tester.isProxyRequired(URI.create("http://www.example.com"), p));
		
		params.put("useProxy", true);
		assertTrue(tester.isProxyRequired(URI.create("http://www.example2.com"), tester.verify(params)));
		
		params.put("useProxy", false);
		assertFalse(tester.isProxyRequired(URI.create("http://www.example.com"), tester.verify(params)));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testWrongHeader1() {
		Tester<ServiceParams> tester = new BasicHttpTester(null);
		final Map<String, Object> params = new HashMap<>();
		params.put("headers", 0);
		tester.verify(params);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUnknownProxy() {
		final Map<String, Object> map = Collections.singletonMap("proxy", "myTsrangeaddfklqdsls.com:3218");
		new BasicHttpTester(map);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testWrongProxy1() {
		final Map<String, Object> map = Collections.singletonMap("proxy", 3128);
		new BasicHttpTester(map);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testWrongProxy2() {
		final Map<String, Object> map = Collections.singletonMap("proxy", "127.0.0.1");
		new BasicHttpTester(map);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testWrongProxy3() {
		final Map<String, Object> map = Collections.singletonMap("proxy", "127.0.0.1:");
		new BasicHttpTester(map);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testWrongProxy4() {
		final Map<String, Object> map = Collections.singletonMap("proxy", "127.0.0.1:65536");
		new BasicHttpTester(map);
	}
}
