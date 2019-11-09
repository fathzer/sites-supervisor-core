package com.fathzer.checkmysites.db;

import java.io.Closeable;
import java.io.IOException;

import com.fathzer.checkmysites.Configuration.ServiceInfo;

public interface DB extends Closeable {
	void connect() throws IOException;
	boolean isOk(ServiceInfo info) throws IOException;
	void report(ServiceInfo info, double responseTime, String cause) throws IOException;
	void reportStateChange(ServiceInfo info, String errorMessage) throws IOException;
}
