package com.fathzer.sitessupervisor.alerter;

import java.util.Map;

import com.fathzer.sitessupervisor.Configuration.ServiceInfo;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogAlerter extends Alerter<LogAlerter.ServiceParams> {

	@Getter
	@EqualsAndHashCode
	@AllArgsConstructor
	public static class ServiceParams {
	}

	public LogAlerter(Map<String, Object> parameters) {
		super(parameters);
	}

	@Override
	public ServiceParams verify(Map<String, Object> serviceParameters) {
		if (serviceParameters!=null) {
			throw new IllegalArgumentException("This alerter accept no service parameters");
		}
		return new ServiceParams();
	}

	@Override
	public void alert(ServiceInfo info, ServiceParams config, String cause) {
		if (cause==null) {
			log.info("{} ({}, {}) is up", info.getUri(), info.getApp(), info.getEnv());
		} else {
			log.warn("{} ({}, {}) is down with message {}", info.getUri(), info.getApp(), info.getEnv(), cause);
		}
	}
}
