package com.fathzer.sitessupervisor;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fathzer.sitessupervisor.db.DB;
import com.fathzer.sitessupervisor.Configuration.Service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Supervisor {
	// WARNING: This variable is not final for the tests to be able to hack it
	private static long MIN_TO_MS = 60*1000L;
	
	@Getter
	@Setter
	private Configuration settings;
	private Timer scheduler;
	private DB db;
	private ExecutorService workers;
	private Map<URI, Check> checks;

	/** Constructor.
	 * @param settings The supervisor's settings
	 */
	public Supervisor(Configuration settings) {
		this.settings = settings;
		this.checks = new HashMap<>();
	}

	/** Starts the supervisor.
	 * @throws IOException
	 */
	public void start() throws IOException {
		db = settings.getDatabase();
		if (db!=null) {
			db.connect();
		}
		scheduler = new Timer();
		workers = Executors.newCachedThreadPool();
		checks.values().forEach(this::schedule);
		log.info("Supervision started");
	}
	
	/** Sets the list of services supervised by this supervisor.
	 * @param services The new service list. All services previously set and absent from this collection will be removed.<br>
	 * To modify an individual service, prefer the {@link #addService(Service)} method.
	 * @return true if some changes was made, false if the new services list is equivalent to the current one.
	 * @throws IllegalArgumentException if two or more services share the same URL.
	 * @see #addService(Service)
	 */
	public synchronized boolean setServices(Collection<Service> services) {
		// 1 Test there's no duplicated URI.
		// 1.1 Build a map of URI -> number of occurrences in services
		final Map<URI, Long> map = services.stream().map(s -> s.getInfo().getUri()).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		// 1.2 Builds the list of duplicated URI.
		final List<URI> duplicated = map.entrySet().stream().filter(p -> p.getValue()>1).map(Map.Entry::getKey).collect(Collectors.toList());
		// 1.3 throw an exception if there's some duplicated URI
		if (!duplicated.isEmpty()) {
			throw new IllegalArgumentException("The following URI are duplicated: "+duplicated);
		}
		// 2 Remove URI that are no more in the services list.
		// The collect step in this streamed process is mandatory to prevent having concurrent modification on the keySet.
		List<URI> removed = checks.keySet().stream().filter(uri -> !map.containsKey(uri)).collect(Collectors.toList());
		removed.forEach(this::removeService);
		// 3 Updates the services.
		List<Boolean> updated = services.stream().map(this::addService).collect(Collectors.toList());
		
		return updated.stream().anyMatch(b -> b) || !removed.isEmpty();
	}

	/** Adds or updates a service.
	 * @param service The service to update.
	 * @return true if the passed service is different from the previous setting for that URI.
	 */
	public synchronized boolean addService(Service service) {
		// Verify that the service settings are different from previous ones.
		final Check previous = checks.get(service.getInfo().getUri());
		if (previous==null || !service.equals(previous.getService())) {
			final Check task = new Check(service, db, workers);
			checks.put(service.getInfo().getUri(), task);
			if (scheduler!=null) {
				stop(previous);
				schedule(task);
			}
			return true;
		} else {
			return false;
		}
	}
	
	/** Stops the supervision of a service. 
	 * @param uri The service URI
	 * @return true if there was a service with that uri.
	 */
	public synchronized boolean removeService(URI uri) {
		final Check check = checks.remove(uri);
		stop(check);
		return check == null;
	}

	private void stop(final Check check) {
		if (check!=null) {
			log.info("Stopping supervision on {}", check.getService().getInfo().getUri());
			check.cancel();
		}
	}
	
	private void schedule(Check task) {
		Service s = task.getService();
		scheduler.scheduleAtFixedRate(task, (long)(Math.random()*MIN_TO_MS*s.getFrequencyMinutes()), s.getFrequencyMinutes()*MIN_TO_MS);
		log.info("Supervision of {} is scheduled every {} minutes",s.getInfo().getUri(), s.getFrequencyMinutes());
	}

	/** Closes this supervisor.
	 * <br>Any subsequent calls to other methods of this instance may have unpredictable results. 
	 * @throws IOException if something went wrong during supervisor closing
	 */
	public synchronized void close() throws IOException {
		closeScheduler();
		if (db!=null) {
			db.close();
			db = null;
			log.info("Database is closed");
		}
	}

	private void closeScheduler() {
		if (scheduler != null) {
			log.info("Stopping supervisors threads ...");
			scheduler.cancel();
			workers.shutdown();
			try {
				workers.awaitTermination(60, TimeUnit.SECONDS);
				log.info("Supervisors threads are closed");
			} catch (InterruptedException e) {
				log.info("Unable to close supervisors threads in 60s",e);
				Thread.currentThread().interrupt();
			}
			scheduler = null;
		}
	}
}
