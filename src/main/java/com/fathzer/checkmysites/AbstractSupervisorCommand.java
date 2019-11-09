package com.fathzer.checkmysites;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;

import com.fathzer.checkmysites.Configuration.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractSupervisorCommand<T extends Closeable> {

	protected abstract Optional<CommandLine> toCommandLine(String[] args);

	protected void launch(CommandLine cmd) throws IOException {
		final Configuration settings = getConfiguration(cmd);
		final Collection<Service> services = getServices(cmd, settings);
		final Supervisor supervisor = new Supervisor(settings);
		final T spy = getUpdateSpy(cmd);
		supervisor.start();
		Runtime.getRuntime().addShutdownHook(new Thread(getShutdownHook(supervisor, spy)));
		supervisor.setServices(services);
		if (spy!=null) {
			startSpy(supervisor, Paths.get(cmd.getArgs()[1]), spy);
		}
	}
	protected abstract Configuration getConfiguration(CommandLine cmd) throws IOException;
	protected abstract Collection<Service> getServices(CommandLine cmd, final Configuration settings) throws IOException;
	protected abstract T getUpdateSpy(CommandLine cmd) throws IOException;
	protected abstract void startSpy(final Supervisor supervisor, final Path path, final T spy) throws IOException;

	protected Runnable getShutdownHook(final Supervisor supervisor, final T spy) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					if (spy!=null) {
						spy.close();
					}
					supervisor.close();
				} catch (IOException e) {
					log.warn("A, error occured while closing", e);
				}
			}
		};
	}

}
