package com.fathzer.checkmysites;

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

import com.fathzer.checkmysites.Configuration.Service;
import com.fathzer.checkmysites.parsing.JSONParser;

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
		path.toAbsolutePath().getParent().register(spy, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
		try {
			WatchKey key;
			while ((key=spy.take()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					final Path context = (Path) event.context();
					if (Files.isSameFile(context, path)) {
						log.info("Change detected on services file");
						doUpdate(supervisor, path);
					}
				}
				key.reset();
			}
		} catch (InterruptedException e) {
			log.error("Listener on services file was interrupted",e);
		} catch (ClosedWatchServiceException e) {
			log.info("Listener on services file is stopped with exception");
		}
	}

	private static void doUpdate(Supervisor supervisor, Path path) {
		try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
			List<Service> services = new JSONParser().parseServices(in, supervisor.getSettings());
			supervisor.setServices(services);
		} catch (Exception e) {
			log.error("Error while updating service configuration", e);
		}
	}
}
