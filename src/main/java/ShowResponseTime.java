import java.time.Instant;
import java.util.List;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;

import lombok.ToString;

public class ShowResponseTime {

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

	public static void main(String[] args) {
		final String url = "http://127.0.0.1:8086";
		InfluxDB db = InfluxDBFactory.connect(url);
		db.setDatabase("check-my-sites");
		System.out.println (db.query(new Query("SHOW TAG VALUES WITH KEY = url")));
//		QueryResult queryResult = db.query(new Query("Select url from responseTime"));
//		if (queryResult.getError()!=null) {
//			System.out.println(queryResult.getError());
//		} else {
//			InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
//			List<ResponseTime> pointList = resultMapper.toPOJO(queryResult, ResponseTime.class);
//			pointList.forEach(p -> System.out.println(p));
//		}
	}
}
