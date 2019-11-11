package com.fathzer.sitessupervisor.alerter;

import java.util.Map;

import com.fathzer.sitessupervisor.Configuration.ServiceInfo;

/**
 * An abstract alerter.
 * @param <T> The type of the parameters dedicated to each service. 
 */
public abstract class Alerter<T> {
	/** Constructor.
	 * <br>Please note all subclasses should have a constructor with the same arguments.
	 * @param parameters The alerter's parameters. For instance, for an email alerter, the smtp server's host name.
	 * @throws IllegalArgumentException if mandatory parameters are missing in the map or contains invalid values.
	 */
	protected Alerter(Map<String, Object> parameters) {
		super();
	}
	
	/** Verifies the parameters declared in a service.
	 * @param serviceParameters The service's parameters. For instance, for an email alerter, the list of email recipients.
	 * @return An object that represents the serviceParameter in a ready to use representation (for instance, for an email alerter, the email addresses ready to by used by javax.mail package).
	 * The returned object will be passed to the {@link #alert(ServiceInfo, Object, String)} method.
	 * It should implement an equals method that guarantees two objects created with equivalent parameters will be equals (This guarantees that only services changed are impacted when configuration file changes)
	 */
	public abstract T verify(Map<String, Object> serviceParameters);

	/** Sends an alert.
	 * @param info The service concerned by the alert.
	 * @param config The object returned by the verify method.
	 * @param cause The cause of the alert, null if the service status is changed from down to up.
	 */
	public abstract void alert(ServiceInfo info, T config, String cause);
}
