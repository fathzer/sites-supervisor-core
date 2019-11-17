package com.fathzer.sitessupervisor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fathzer.sitessupervisor.parsing.JSONParser;
import com.fathzer.sitessupervisor.Configuration.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SupervisorCommand extends AbstractSupervisorCommand<WatchService> {

	private static final String AUTORELOAD_OPTION = "autoreload";
	
	public static void main(String[] args) {
		final SupervisorCommand sc = new SupervisorCommand();
		final Optional<CommandLine> cmd = sc.toCommandLine(args);
		if (cmd.isPresent()) {
			try {
				sc.launch(cmd.get());
			} catch (IOException e) {
				log.error("Unable to read configuration files",e);
			}
		}
	}

	@Override
	protected Optional<CommandLine> toCommandLine(String[] args) {
		Options options = new Options();
		options.addOption(AUTORELOAD_OPTION, "The services configuration is automatically reloaded when services file is updated.");
		try {
			final CommandLine cmd = new DefaultParser().parse(options, args);
			if (cmd.getArgList().size()!=2) {
				throw new ParseException("Wrong number of arguments");
			}
			return Optional.of(cmd);
		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			String header = "";
			formatter.printHelp(160, String.format("java %s [options] configFile servicesFile\nOptions:\n", SupervisorCommand.class.getName()), header, options, null, false);
			return Optional.empty();
		}
	}

	@Override
	protected WatchService getUpdateSpy(CommandLine cmd) throws IOException {
		final WatchService spy;
		if (cmd.hasOption(AUTORELOAD_OPTION)) {
			spy = FileSystems.getDefault().newWatchService();
		} else {
			spy = null;
		}
		return spy;
	}

	@Override
	protected Collection<Service> getServices(CommandLine cmd, final Configuration settings) throws IOException {
		final Collection<Service> services;
		final Path path = Paths.get(cmd.getArgs()[1]);
		try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
			services = new JSONParser().parseServices(in, settings);
		}
		return services;
	}

	@Override
	protected Configuration getConfiguration(CommandLine cmd) throws IOException {
		final Configuration settings;
		Path path = Paths.get(cmd.getArgs()[0]);
		try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
			settings = new JSONParser().parseConfiguration(in);
		}
		return settings;
	}

	@Override
	protected void startSpy(final Supervisor supervisor, final Path path, final WatchService spy) throws IOException {
		log.info("Start listening services file changes");
		path.toAbsolutePath().getParent().register(spy, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
		try {
			WatchKey key;
			while ((key=spy.take()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					try {
						final Path context = (Path) event.context();
						if (context.endsWith(path.getFileName())) {
							if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind()) || Files.size(path)==0) {
								// Warning: Some editors (for instance when a file is edited by FileZilla), generate two events when modifying a file
								// One with a 0 length file, and one when the file is updated.
								// A 0 length file could signify we want to stop supervision ... but in that case, we also could stop the supervisor ...
								// So, to keep things simple, we will ignore the 0 length file events.
								log.info("Ignoring services file was cleared or deleted");
							} else {
								log.info("Change detected on services file");
								doUpdate(supervisor, path);
							}
						}
					} catch (Exception e) {
						log.warn("exception while updating services", e);
					}
				}
				key.reset();
			}
		} catch (InterruptedException e) {
			log.error("Listener on services file was interrupted",e);
			Thread.currentThread().interrupt();
		} catch (ClosedWatchServiceException e) {
			log.info("Listener on services file is stopped with exception");
		}
	}

	private static void doUpdate(Supervisor supervisor, Path path) {
		try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
			List<Service> services = new JSONParser().parseServices(in, supervisor.getSettings());
			if (!supervisor.setServices(services)) {
				log.info("Service configuration contains no service update");
			}
		} catch (Exception e) {
			log.error("Error while updating service configuration", e);
		}
	}
}
