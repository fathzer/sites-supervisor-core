package com.fathzer.sitessupervisor.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import com.fathzer.sitessupervisor.Configuration.ServiceInfo;

/**
 * An abstract connector to a database.
 */
public abstract class DB implements Closeable {
	/** Constructor.
	 * <br>Please note all subclasses should have a constructor with the same arguments.
	 * @param parameters The database connector's parameters (could be null).
	 * @throws IllegalArgumentException if mandatory parameters are missing in the map or contains invalid values.
	 */
	protected DB(Map<String, Object> parameters) {
		super();
	}

	/** Establishes the connection to the database.
	 * @throws IOException if something went wrong.
	 */
	public abstract void connect() throws IOException;

	/** Tests if a service is ok.
	 * <br>This method is used when starting a service supervision in order to know its previous state to not send alert if the service was already down. 
	 * @param info The service concerned.
	 * @return true if the service was ok.
	 * @throws IOException if something went wrong.
	 */
	public abstract boolean isOk(ServiceInfo info) throws IOException;

	/** Writes the results of a test in the database.
	 * @param info The service concerned.
	 * @param responseTime The time the service took to answer (in ms).
	 * @param cause The cause of the failure (null, if the test succeeded).
	 * @throws IOException if something went wrong.
	 */
	public abstract void write(ServiceInfo info, double responseTime, String cause) throws IOException;
	
	/** Writes a status change in the database.
	 * @param info The service concerned.
	 * @param cause The cause of the failure (null, if the service is now up).
	 * @throws IOException if something went wrong.
	 */
	public abstract void writeStateChange(ServiceInfo info, String cause) throws IOException;
}
