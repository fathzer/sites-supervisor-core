package com.fathzer.sitessupervisor;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import com.fathzer.sitessupervisor.parsing.JSONParser;
import com.fathzer.sitessupervisor.Configuration;
import com.fathzer.sitessupervisor.Supervisor;
import com.fathzer.sitessupervisor.Configuration.Service;

public class SupervisorTest {

	@Test
	public void test() throws IOException, InterruptedException {
		try {
			Field field = Arrays.stream(Supervisor.class.getDeclaredFields()).filter(f-> f.getName().equals("MIN_TO_MS")).
				iterator().next();
			field.setAccessible(true);
			field.setInt(null, 100);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			fail("Unable to hack min to seconds ratio");
		}
		
		final Configuration settings;
		try (InputStream stream=getClass().getResourceAsStream("/testConfiguration.json")) {
			settings = new JSONParser().parseConfiguration(stream);
		}
		final Collection<Service> services;
		try (InputStream stream=getClass().getResourceAsStream("/testServices.json")) {
			services = new JSONParser().parseServices(stream, settings);
		}
		final Supervisor tested = new Supervisor(settings);
		tested.start();
		try {
			tested.setServices(services);
			
			Thread.sleep(3000);
		} finally {
			tested.close();
		}
		
		//TODO Perform tests
	}

}
