package com.fathzer.sitessupervisor.tester;

import java.net.URI;
import java.util.Map;

public class FakeTester extends Tester<Void> {

	public FakeTester(Map<String,Object> parameters) {
		super(parameters);
	}

	@Override
	public Void verify(Map<String, Object> serviceParameters) {
		return null;
	}

	@Override
	public String check(URI uri, int timeOutSeconds, Void parameters) {
		try {
			Thread.sleep((long)(100*Math.random()));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return Math.random()<0.9 ? null : "did not reply";
	}
}
