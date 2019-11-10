package com.fathzer.sitessupervisor;

import java.io.IOException;
import java.util.Collection;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import com.fathzer.sitessupervisor.db.DB;
import com.fathzer.sitessupervisor.Configuration.AlerterPluginConfig;
import com.fathzer.sitessupervisor.Configuration.Service;
import com.fathzer.sitessupervisor.Configuration.ServiceInfo;
import com.fathzer.sitessupervisor.Configuration.TesterPluginConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Check extends TimerTask {
	private InternalCheck internal;
	private ExecutorService workers;
	
	private static class InternalCheck implements Runnable {
		private Service service;
		private DB db;
		private boolean isOk;

		public InternalCheck(Service service, DB db) {
			super();
			this.service = service;
			this.db = db;
			if (db==null) {
				this.isOk = true;
			} else {
				try {
					this.isOk = db.isOk(service.getInfo());
				} catch (IOException e) {
					log.error(String.format("Unable to retrieve previous state of %s assuming it was ok", service.getInfo().getUri()),e);
				}
			}
		}

		@Override
		public void run() {
			try {
				final long now = System.currentTimeMillis();
				final TesterPluginConfig<?> testerConf = service.getTester();
				final String cause = doTest(testerConf, service.getInfo(), service.getTimeOutSeconds());
				final boolean ok = cause==null;
				if (db!=null) {
					final long responseTime = System.currentTimeMillis()-now;
					db.report(service.getInfo(), responseTime/1000.0, cause);
				}
				if (ok!=isOk) {
					isOk = ok;
					if (db!=null) {
						db.reportStateChange(service.getInfo(), cause);
					}
					alert(cause);
				}
			} catch (Throwable e) {
				log.error("An error occurred while testing "+service.getInfo().getUri(), e);
			}
		}

		private static <V> String doTest(TesterPluginConfig<V> tc, ServiceInfo info, int timeOutSeconds) {
			return tc.getPlugin().check(info.getUri(), timeOutSeconds, tc.getConfig());
		}

		private void alert(String cause) {
			final Collection<AlerterPluginConfig<?>> alerters = service.getAlerters();
			if (!alerters.isEmpty()) {
				alerters.forEach(e -> doAlert(e, service.getInfo(), cause));
			}
		}
		
		private static <V> void doAlert(AlerterPluginConfig<V> pc, ServiceInfo info, String cause) {
			pc.getPlugin().alert(info, pc.getConfig(), cause);
		}
	}
	
	public Check(Service service, DB db, ExecutorService workers) {
		super();
		this.workers = workers;
		this.internal = new InternalCheck(service, db);
	}
	
	public Service getService() {
		return internal.service;
	}

	@Override
	public final void run() {
		this.workers.execute(internal);
	}
}
