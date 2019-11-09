import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.Point.Builder;
import org.influxdb.impl.InfluxDBResultMapper;


import lombok.ToString;

public class ToTags {

	@ToString
	@Measurement(name = "responseTime")
	public static class ResponseTime {
    @Column(name = "time")
    private Instant time;
    @Column(name = "url")
    public String url;
    @Column(name = "app")
    public String app;
    @Column(name = "env")
    public String env;
    @Column(name = "responseTime")
    private double responseTime;
    @Column(name = "success")
		public int success;
    @Column(name = "message")
		public String mess;
	}

	@ToString
	@Measurement(name = "events")
	public static class Event {
    @Column(name = "time")
    private Instant time;
    @Column(name = "url")
    public String url;
    @Column(name = "app")
    public String app;
    @Column(name = "env")
    public String env;
    @Column(name = "up")
    private long up;
    @Column(name = "message")
		public String mess;
	}


	public static void main(String[] args) {
		final String url = "http://127.0.0.1:8086";
		try (final InfluxDB db = InfluxDBFactory.connect(url)) {
			db.setDatabase("check-my-sites");
			final QueryResult tables = db.query(new Query("SHOW SERIES"));
			db.query(new Query("DROP SERIES FROM responseTime2"));
			db.query(new Query("DROP SERIES FROM events2"));

			System.out.println(tables);
//			db.enableBatch();
//			update(db, "responseTime", ResponseTime.class, p -> report(db , "responseTime", p));
//			update(db, "events", Event.class, p -> report(db , "events", p));
//			db.flush();
		}
		System.out.println("done");
	}

	protected static <T> void update(final InfluxDB db, String table, Class<T> cl, Consumer<T> writer) {
		System.out.println(db.query(new Query("SHOW SERIES")).getResults().get(0).getSeries().get(0).getValues());
		QueryResult queryResult = db.query(new Query("Select * from "+table));
		if (queryResult.getError()!=null) {
			System.out.println(queryResult.getError());
		} else {
			InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
			List<T> pointList = resultMapper.toPOJO(queryResult, cl);
			System.out.println("Found "+pointList.size()+" records");
			db.query(new Query("DROP SERIES FROM "+table));
			System.out.println(db.query(new Query("SHOW SERIES")).getResults().get(0).getSeries().get(0).getValues());
			pointList.forEach(writer);
		}
	}
	
	public static void report(InfluxDB db, String table, ResponseTime p) {
		try {
			final Builder builder = Point.measurement(table)
				  .time(p.time.toEpochMilli(), TimeUnit.MILLISECONDS)
				  .tag("url", p.url)
				  .tag("app", p.app==null ? "?":p.app)
				  .tag("env", p.env==null ? "?":p.env)
				  .addField("success", p.success)
					.addField("responseTime", p.responseTime);
			if (p.mess!=null) {
				builder.addField("message", p.mess);
			}
			db.write(builder.build());
		} catch (NullPointerException e) {
			System.out.println(p);
			throw e;
		}
	}

	public static void report(InfluxDB db, String table, Event p) {
		try {
			final Builder builder = Point.measurement(table)
				  .time(p.time.toEpochMilli(), TimeUnit.MILLISECONDS)
				  .tag("url", p.url)
				  .tag("app", p.app==null ? "?":p.app)
				  .tag("env", p.env==null ? "?":p.env)
				  .addField("up", p.up);
			if (p.mess!=null) {
				builder.addField("message", p.mess);
			}
			db.write(builder.build());
		} catch (NullPointerException e) {
			System.out.println(p);
			throw e;
		}
	}
}
