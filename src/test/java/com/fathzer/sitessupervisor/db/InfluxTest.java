package com.fathzer.sitessupervisor.db;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.sitessupervisor.db.DB;
import com.fathzer.sitessupervisor.db.Influx;

public class InfluxTest {
	@Test
	public void test() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> map = mapper.readValue("{'host':'toto.com','port':1000}".replace('\'', '"'), Map.class);
		try (Influx db = new Influx(map)) {
			//TODO Test settings
		}
	}

	@Test (expected=IllegalArgumentException.class)
	public void testWrongParam1() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> map = mapper.readValue("{'host':'toto.com','portw':'1000'}".replace('\'', '"'), Map.class);
		try (final DB db = new Influx(map)) {
		}
	}
	@Test (expected=IllegalArgumentException.class)
	public void testWrongParam2() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> map = mapper.readValue("{'host':'toto.com','port':-1}".replace('\'', '"'), Map.class);
		try (final DB db = new Influx(map)) {
		}
	}
	@Test (expected=IllegalArgumentException.class)
	public void testWrongParam3() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> map = mapper.readValue("{'user':'xxx'}".replace('\'', '"'), Map.class);
		try (final DB db = new Influx(map)) {
		}
	}
}
