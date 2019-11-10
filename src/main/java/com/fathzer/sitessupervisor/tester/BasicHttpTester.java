package com.fathzer.sitessupervisor.tester;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasicHttpTester extends Tester<BasicHttpTester.ServiceParams> {
	private static final String NOT_STRING_ERR = "%s used as %s attribute is not a String";
	private static final String PROXY_ATTRIBUTE = "proxy";
	private static final String NO_PROXY_ATTRIBUTE = "noProxy";
	
	private static final Timer TIME_OUT_MANAGER = new Timer(true);
	private final Params parameters;
	
	@Getter
	@EqualsAndHashCode
	public static class ServiceParams {
		private Map<String,String> headers;
		private Boolean useProxy;
	}
	
	@Getter
	private static class Params {
		private DefaultProxyRoutePlanner proxy;
		private List<String> noProxy;
	}
	
	public BasicHttpTester(Map<String,Object> params) {
		super(params);
		this.parameters = new Params();
		if (params!=null && params.containsKey(PROXY_ATTRIBUTE)) {
			try {
				String addressString = (String)params.get(PROXY_ATTRIBUTE);
				final InetSocketAddress address = new InetSocketAddress(addressString.substring(0, addressString.lastIndexOf(":")),
					  Integer.parseInt(addressString.substring(addressString.lastIndexOf(":")+1)));
				// Will throw IllegalArgumentException if address is unresolved ... this is what we want :-)
				this.parameters.proxy = new DefaultProxyRoutePlanner(new HttpHost(address.getAddress()));
			} catch (ClassCastException e) {
				throw new IllegalArgumentException(String.format(NOT_STRING_ERR, params.get(PROXY_ATTRIBUTE)));
			} catch (StringIndexOutOfBoundsException e) {
				throw new IllegalArgumentException(String.format("%s attribute (%s) does not comply with expected format (host:port)", params.get(PROXY_ATTRIBUTE)));
			}
			if (params.containsKey(NO_PROXY_ATTRIBUTE)) {
				try {
					this.parameters.noProxy = Arrays.asList(((String)params.get(NO_PROXY_ATTRIBUTE)).split(","));
				} catch (ClassCastException e) {
					throw new IllegalArgumentException(String.format(NOT_STRING_ERR, params.get(NO_PROXY_ATTRIBUTE)));
				}
			}
		}
	}

	@Override
	public ServiceParams verify(Map<String, Object> serviceParameters) {
		if (serviceParameters==null) {
			return null;
		}
		return new ObjectMapper().convertValue(serviceParameters, ServiceParams.class);
	}

	@Override
	public String check(URI uri, int timeOutSeconds, ServiceParams parameters) {
		final HttpClientBuilder builder = HttpClients.custom();
		if (this.parameters.proxy!=null && isProxyRequired(uri, parameters)) {
			builder.setRoutePlanner(this.parameters.proxy);
		}
		CloseableHttpClient client = builder.build();
		try {
			final HttpUriRequest req = buildRequest(uri, parameters);
			final AtomicBoolean timedOut = new AtomicBoolean();
			final TimerTask task = new TimerTask() {
				@Override
				public void run() {
					if (req != null) {
						timedOut.set(true);
						req.abort();
					}
				}
			};
			TIME_OUT_MANAGER.schedule(task, timeOutSeconds * 1000);
			try {
				HttpResponse response = client.execute(req);
				task.cancel();
				if (timedOut.get()) {
					return "Timed out";
				}
				return isValid(response);
			} catch (IOException e) {
				log.trace("Error while testing URL", e);
				return e.getMessage();
			}
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				log.warn("Error while closing http client",e);
			}
		}
	}

	public boolean isProxyRequired(URI uri, ServiceParams params) {
		if (params!=null && params.getUseProxy()!=null) {
			return params.getUseProxy();
		}
		for (String suffix : this.parameters.noProxy) {
			if (uri.getHost().endsWith(suffix)) {
				return false;
			}
		}
		return true;
	}

	/** Builds the http request that will be tested.
	 * <br>You can override this method to hack the http request (let's say to request an authentication token and include it the headers ...).
	 * @param uri The URI to test
	 * @param params The parameters of the test
	 * @return The HTTPRequest to test.
	 */
	protected HttpUriRequest buildRequest(URI uri, ServiceParams params) {
		final RequestBuilder builder = RequestBuilder.get().setUri(uri);
		builder.addHeader("Cache-Control","no-cache, no-store, must-revalidate");
		if (params != null && params.getHeaders()!=null) {
			params.getHeaders().forEach((k,v) -> builder.setHeader(k, v));
		}
		return builder.build();
	}

	/** Tests whether the response is valid or not.
	 * <br>This default implementation asserts the response is valid if it status is 200.
	 * <br>You can override this method to implement more complex validation scheme.
	 * @param response The response to test
	 * @return true if the response is valid.
	 */
	protected String isValid(HttpResponse response) {
		final StatusLine statusLine = response.getStatusLine();
		return statusLine.getStatusCode()==200 ? null : String.format("%s (%d)", statusLine.getReasonPhrase(), statusLine.getStatusCode());
	}
}
