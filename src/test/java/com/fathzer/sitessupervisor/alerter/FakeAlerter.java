package com.fathzer.sitessupervisor.alerter;

import java.util.Map;

import com.fathzer.sitessupervisor.Configuration.ServiceInfo;

public class FakeAlerter extends Alerter<Void> {
	
	public FakeAlerter(Map<String,Object> parameters) {
		super(parameters);
	}

	@Override
	public Void verify(Map<String, Object> serviceParameters) {
		return null;
	}

	@Override
	public void alert(ServiceInfo info, Void config, String cause) {
		System.out.println(String.format("%s state has changed to %s", info.getUri(), (cause==null?"ok":"ko"), cause));
	}
}
