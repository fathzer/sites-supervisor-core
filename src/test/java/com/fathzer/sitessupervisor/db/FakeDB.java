package com.fathzer.sitessupervisor.db;

import java.io.IOException;
import java.util.Map;

import com.fathzer.sitessupervisor.Configuration.ServiceInfo;

public class FakeDB implements DB {
	public FakeDB(Map<String, Object> params) {
		super();
	}
	
	@Override
	public void connect() throws IOException {
	}

	@Override
	public void close() {
	}

	@Override
	public void report(ServiceInfo info, double responseTime, String cause) {
		// TODO Auto-generated method stub
		System.out.println(String.format("%s;%s;%s;%f;%s",info.getUri(), info.getApp(), info.getEnv(), responseTime, cause));
	}

	@Override
	public boolean isOk(ServiceInfo info) throws IOException {
		return true;
	}

	@Override
	public void reportStateChange(ServiceInfo info, String errorMessage) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
