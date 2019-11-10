package com.fathzer.sitessupervisor;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import com.fathzer.sitessupervisor.alerter.Alerter;
import com.fathzer.sitessupervisor.db.DB;
import com.fathzer.sitessupervisor.tester.Tester;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Configuration {
	private DB database;
	private Map<String, Alerter<?>> alerters;
	private Map<String, Tester<?>> testers;
	
	@AllArgsConstructor
	@Getter
	@EqualsAndHashCode
	private static class PluginConfig<C,T> {
		private C plugin;
		private T config;
	}

	@ToString
	public static class TesterPluginConfig<T> extends PluginConfig<Tester<T>, T> {
		public TesterPluginConfig(Tester<T> plugin, T config) {
			super(plugin, config);
		}
	}

	@ToString
	public static class AlerterPluginConfig<T> extends PluginConfig<Alerter<T>, T> {
		public AlerterPluginConfig(Alerter<T> plugin, T config) {
			super(plugin, config);
		}
	}
	
	@Getter
	@ToString
	@EqualsAndHashCode
	public static class Service {
		private ServiceInfo info;
		private int frequencyMinutes;
		private int timeOutSeconds;
		private TesterPluginConfig<?> tester;
		private Set<AlerterPluginConfig<?>> alerters;

		Set<AlerterPluginConfig<?>> getAlerters() {
			return this.alerters;
		}

		public Service(ServiceInfo info, int frequencyMinutes, int timeOutSeconds, TesterPluginConfig<?> tester,
				Set<AlerterPluginConfig<?>> alerters) {
			super();
			if (info.getUri()==null) {
				throw new IllegalArgumentException("URI should be provided");
			}
			if (tester==null) {
				throw new IllegalArgumentException("tester is mandatory");
			}
			this.info = info;
			this.frequencyMinutes = frequencyMinutes;
			this.timeOutSeconds = timeOutSeconds;
			this.tester = tester;
			this.alerters = alerters;
		}
	}
	
	@AllArgsConstructor
	@Getter
	@EqualsAndHashCode
	public static class ServiceInfo {
		private URI uri;
		private String app;
		private String env;
	}
}
