package com.fathzer.sitessupervisor.alerter;

import java.util.Map;

import com.fathzer.sitessupervisor.Configuration.ServiceInfo;

import lombok.Getter;

@Getter
public abstract class Alerter<T> {
	private String name;
	
	/** Constructor.
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
	 */
	public abstract T verify(Map<String, Object> serviceParameters);

	public abstract void alert(ServiceInfo info, T config, String cause);
}
