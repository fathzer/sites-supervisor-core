package com.fathzer.sitessupervisor.tester;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.sitessupervisor.commons.ProxySettings;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasicHttpTester extends Tester<BasicHttpTester.ServiceParams> {
	private static final Timer TIME_OUT_MANAGER = new Timer(true);
	private final ProxySettings parameters;
	
	@Getter
	@EqualsAndHashCode
	public static class ServiceParams {
		private Map<String,String> headers;
		private Boolean useProxy;
	}
	
	public BasicHttpTester(Map<String,Object> params) {
		super(params);
		this.parameters = ProxySettings.build(params);
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
		if (this.parameters.getProxy()!=null && isProxyRequired(uri, parameters)) {
			builder.setRoutePlanner(this.parameters.getProxy());
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
			TIME_OUT_MANAGER.schedule(task, timeOutSeconds * 1000L);
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
		return this.parameters.isProxyRequired(uri);
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
