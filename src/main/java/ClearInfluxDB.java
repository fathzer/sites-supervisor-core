import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

public class ClearInfluxDB {

	public static void main(String[] args) {
		final String url = "http://127.0.0.1:8086";
		InfluxDB db = InfluxDBFactory.connect(url);
		db.enableBatch();
		db.deleteDatabase("check-my-sites");
		db.flush();
		db.close();
	}
}
