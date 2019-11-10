package com.fathzer.sitessupervisor.db;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BoundParameterQuery.QueryBuilder;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.sitessupervisor.Configuration.ServiceInfo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Influx implements DB {
	private static final String EVENTS_TABLE = "events";
	private static final String RESPONSE_TIME_TABLE = "responseTime";
	
	private InfluxDB db;
	private Database settings;
	
	@Getter
	public static class Database {
		private String host = "127.0.0.1";
		private int port = 8086;
		private String database = "check-my-sites";
		private String user = null;
		private String password = null;
	}

	public Influx(Map<String, Object> params) {
		if (params!=null) {
			this.settings = new ObjectMapper().convertValue(params, Database.class);
			if (settings.port<0 || settings.port>65535) {
				throw new IllegalArgumentException();
			}
			if ((settings.user!=null && settings.password==null) || (settings.user==null && settings.password!=null)) {
				throw new IllegalArgumentException("Both or none of user and password should be set");
			}
		} else {
			this.settings = new Database();
		}
	}
	
	@Override
	public void connect() throws IOException {
		if (db==null) {
			final String url = "http://"+settings.getHost()+":"+settings.getPort();
			this.db = settings.user!=null ? InfluxDBFactory.connect(url, settings.user, settings.password) : InfluxDBFactory.connect(url);
			Pong response = this.db.ping();
			if (response.getVersion().equalsIgnoreCase("unknown")) {
			    log.error("Error pinging server.");
			    return;
			} else {
				log.info("Connection to influxDB is ok, version is {}", response.getVersion());
				if (!databaseExists()) {
			    // Create data base
					this.db.createDatabase(settings.getDatabase());
					this.db.createRetentionPolicy("defaultPolicy", settings.getDatabase(), "30d", 1, true);
					log.info("Database {} created", settings.getDatabase());
				}
				this.db.enableBatch(100, 500, TimeUnit.MILLISECONDS);
				this.db.setRetentionPolicy("defaultPolicy");
				this.db.setDatabase(settings.getDatabase());
			}
		}
	}
	

	private boolean databaseExists() {
		QueryResult result = this.db.query(new Query("SHOW DATABASES"));
		// {"results":[{"series":[{"name":"databases","columns":["name"],"values":[["mydb"]]}]}]}
		// Series [name=databases, columns=[name], values=[[mydb], [unittest_1433605300968]]]
		List<List<Object>> databaseNames = result.getResults().get(0).getSeries().get(0).getValues();
		if (databaseNames != null) {
		  for (List<Object> db : databaseNames) {
		    String name = db.get(0).toString();
		    if (name.equals(settings.getDatabase())) {
					log.info("Database {} found", name);
		    	return true;
		    }
		  }
		}
		return false;
	}

	@Override
	public void close() {
		if (db!=null) {
			this.db.flush();
			this.db.close();
			this.db = null;
			log.info("Connection to InfluxDB is closed");
		}
	}

	@Override
	public boolean isOk(ServiceInfo info) throws IOException {
		Query q = QueryBuilder.newQuery("SELECT up FROM "+EVENTS_TABLE+" WHERE url = $url ORDER BY time DESC LIMIT 1")
        .bind("url", info.getUri().toString()).create();
		QueryResult queryResult = this.db.query(q);
		final Result results = queryResult.getResults().get(0);
		if (results.getSeries()==null) {
			return true;
		} else {
			Double up = (Double) results.getSeries().get(0).getValues().get(0).get(1);
			return Double.compare(up, 0.5)>0;
		}
	}

	@Override
	public void report(ServiceInfo info, double responseTime, String cause) throws IOException {
		final Builder builder = Point.measurement(RESPONSE_TIME_TABLE)
			  .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
			  .tag("url", info.getUri().toString())
			  .tag("app", info.getApp())
			  .tag("env", info.getEnv())
			  .addField("success", cause==null ? 1 : 0)
				.addField("responseTime", cause == null ? responseTime : 0);
		if (cause!=null) {
			builder.addField("message", cause);
		}
		db.write(builder.build());
	}

	@Override
	public void reportStateChange(ServiceInfo info, String errorMessage) throws IOException {
		final Builder builder = Point.measurement(EVENTS_TABLE)
			  .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
			  .tag("url", info.getUri().toString())
			  .tag("app", info.getApp())
			  .tag("env", info.getEnv())
			  .addField("up", errorMessage==null ? 1 : 0);
		if (errorMessage!=null) {
			builder.addField("message", errorMessage);
		}
		db.write(builder.build());
	}
	
	/** Deletes all records about an uri.
	 * @param uri The URI to delete
	 * @return true if some records where deleted, false if none we found
	 * @throws IOException If something went wrong
	 */
	public boolean delete(URI uri) throws IOException {
		return removeFromTable(RESPONSE_TIME_TABLE, uri.toString()) || removeFromTable(EVENTS_TABLE, uri.toString());
	}
	
	private boolean removeFromTable(String tableName, String uri) {
		Query q = QueryBuilder.newQuery("SELECT * FROM "+tableName+" WHERE url = $url")
        .bind("url", uri).create();
		final QueryResult r = db.query(q);
		List<Series> queryResult = r.getResults().get(0).getSeries();
		// If null, no record with uri is present
		if (queryResult==null) {
			log.info("No record found with url {} from {}",uri, tableName);
			return false;
		} else {
			System.out.println(queryResult);
			final List<Object> results = queryResult.get(0).getValues().stream().map(v->v.get(0)).collect(Collectors.toList());
			System.out.println(results);
			for (Object time : results) {
				q = QueryBuilder.newQuery("DELETE FROM "+tableName+" WHERE time = $time")
						.bind("time", time).create();
				db.query(q);
			}
			log.info("{} records with url {} deleted from {}", results.size(), uri, tableName);
			return true;
		}
	}
}
