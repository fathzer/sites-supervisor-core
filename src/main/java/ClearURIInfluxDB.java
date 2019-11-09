import java.io.IOException;
import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.checkmysites.db.Influx;

public class ClearURIInfluxDB {

	public static void main(String[] args) throws IOException {
		try (Influx db = new Influx(new ObjectMapper().readValue("{\"database\":\""+args[0]+"\"}", Map.class))) {
			db.connect();
			db.delete(URI.create(args[1]));
		}
	}
}
