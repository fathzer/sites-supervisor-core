import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Timer;
import java.util.TimerTask;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasicTest {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		final WatchService spy = FileSystems.getDefault().newWatchService();

		new Timer(true).schedule(new TimerTask() {
			@Override
			public void run() {
				log.info("Closing listener on services file");
				try {
					spy.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, 3600000);

		Path path = Paths.get(args[0]);
		path.toAbsolutePath().getParent().register(spy, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
		try {
			WatchKey key;
			while ((key=spy.take()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					final Path context = (Path) event.context();
					if (Files.isSameFile(context, path)) {
						log.info("Change detected on services file "+context.getClass()+" "+context);
						//TODO
					}
				}
				key.reset();
			}
			log.info("Listener on services file is stopped");
		} catch (InterruptedException e) {
			log.error("Listener on services file was interrupted",e);
		} catch (ClosedWatchServiceException e) {
			log.info("Listener on services file is stopped with exception");
		}
	}
}
