package com.fathzer.checkmysites.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.checkmysites.Configuration;
import com.fathzer.checkmysites.Configuration.AlerterPluginConfig;
import com.fathzer.checkmysites.Configuration.Service;
import com.fathzer.checkmysites.Configuration.ServiceInfo;
import com.fathzer.checkmysites.Configuration.TesterPluginConfig;
import com.fathzer.checkmysites.alerter.Alerter;
import com.fathzer.checkmysites.db.DB;
import com.fathzer.checkmysites.tester.Tester;

import lombok.Getter;
import lombok.ToString;

public class JSONParser {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	@Getter
	@ToString
	private static class Settings {
		private GenericPluginSetting database;
		private Map<String,GenericPluginSetting> alerters;
		private Map<String,GenericPluginSetting> testers;
		private List<ServiceSettings> services;
	}
	
	@Getter
	private static class BasicSetting {
		private String name;
		private Map<String, Object> parameters;
	}

	@Getter
	private static class GenericPluginSetting {
		@JsonProperty("class")
		private String className;
		private Map<String, Object> parameters;
	}

	@Getter
	private static class ServiceSettings {
		private List<ServiceSetting> services;
	}

	@Getter
	private static class ServiceSetting {
		private URI uri;
		private String app;
		private String env;
		private int frequencyMinutes = 5;
		private int timeOutSeconds = 30;
		private BasicSetting tester;
		private List<BasicSetting> alerters;
	}

	public Configuration parseConfiguration (InputStream stream) throws IOException {
		final Settings settings = MAPPER.readValue(stream, Settings.class);
		Configuration config = new Configuration();
		if (settings.getDatabase()!=null) {
			config.setDatabase(toDataBase(settings.getDatabase()));
		}
		config.setAlerters(settings.alerters.entrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey, e -> toPlugin(e.getValue(), Alerter.class))));
		config.setTesters(settings.testers.entrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey, e -> toPlugin(e.getValue(), Tester.class))));
		return config;
	}
	
	public List<Service> parseServices(InputStream stream, Configuration config) throws IOException {
		return MAPPER.readValue(stream, ServiceSettings.class).getServices().stream().map(s -> toService(s, config)).collect(Collectors.toList());
	}
	
	private Service toService(ServiceSetting setting, Configuration config) {
		//TODO null check of mandatory attributes => IllegalArgumentException
		Set<AlerterPluginConfig<?>> alerters = setting.getAlerters()==null ?
			Collections.emptySet() : setting.getAlerters().stream().map(e -> toAlerterConfig(config, e)).collect(Collectors.toSet());
		ServiceInfo id = new ServiceInfo(setting.getUri(), setting.getApp(), setting.getEnv());	
		return new Service(id, setting.getFrequencyMinutes(),
				setting.getTimeOutSeconds(), toTesterConfig(config, setting.tester), alerters);
	}

	private DB toDataBase(GenericPluginSetting database) {
		return toPlugin(database, DB.class);
	}

	private <T> AlerterPluginConfig<T> toAlerterConfig(Configuration config, BasicSetting setting) {
		@SuppressWarnings("unchecked")
		final Alerter<T> alerter = (Alerter<T>) config.getAlerters().get(setting.getName());
		if (alerter==null) {
			throw new IllegalArgumentException(String.format("No alerter named %s found", setting.getName()));
		}
		return new AlerterPluginConfig<T>(alerter, alerter.verify(setting.getParameters()));
	}

	private <T> TesterPluginConfig<T> toTesterConfig(Configuration config, BasicSetting setting) {
		if (setting==null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		final Tester<T> tester = (Tester<T>) config.getTesters().get(setting.getName());
		if (tester==null) {
			throw new IllegalArgumentException(String.format("No tester named %s found", setting.getName()));
		}
		return new TesterPluginConfig<T>(tester, tester.verify(setting.getParameters()));
	}

	@SuppressWarnings("unchecked")
	private <T> T toPlugin(GenericPluginSetting setting, Class<T> expectedClass) {
		try {
			final Class<?> theClass = Class.forName(setting.getClassName());
			if (!expectedClass.isAssignableFrom(theClass)) {
				throw new IllegalArgumentException(String.format("Class %s is not a subclass of %s", setting.getClassName(), expectedClass));
			}
			final Constructor<?> constructor = theClass.getConstructor(Map.class);
			return (T) constructor.newInstance(setting.getParameters());
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(String.format("Unable to load class %s", setting.getClassName()));
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(String.format("Class %s does not have a (Map)) constructor", setting.getClassName()));
		} catch (SecurityException | IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(String.format("Class %s can't be instantiated)", setting.getClassName()),e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(String.format("Constructor of class %s failed)", setting.getClassName()),e);
		}
	}
}