package com.fathzer.checkmysites.tester;

import java.net.URI;
import java.util.Map;

import lombok.Getter;

@Getter
public abstract class Tester<T> {
	/** Constructor.
	 * @param parameters The tester's parameters (could be null). For instance, for an http tester, the proxy server's configuration.
	 * @throws IllegalArgumentException if mandatory parameters are missing in the map or contains invalid values.
	 */
	protected Tester(Map<String, Object> parameters) {
		super();
	}

	/** Verifies the parameters declared in a service.
	 * @param serviceParameters The service's parameters. For instance, for an http tester, a http header list.
	 * @return An object that represents the serviceParameter in a ready to use representation.
	 * The returned object will be passed to the {@link #check(com.fathzer.checkmysites.Configuration.Service, Object, String)} method.
	 */
	public abstract T verify(Map<String, Object> serviceParameters);

	/**
	 * Tests whether the site is up or not
	 * @param uri The uri to test
	 * @param timeoutSeconds The test timeOut in seconds.
	 * <br>Note: it's the responsibility of this tester to guaranty the timeout is respected at the method return a non null result if the timeout is reached
	 * @param parameters The test's parameters (could be null)
	 * @return null if the site is up, a cause if the site replied an unexpected answer or the connection reached the timeOut
	 */
	public abstract String check(URI uri, int timeoutSeconds, T parameters);
}
