package com.fathzer.sitessupervisor.alerter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.sitessupervisor.Configuration.ServiceInfo;
import com.fathzer.sitessupervisor.commons.ProxySettings;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TeamsAlerter extends Alerter<TeamsAlerter.ServiceParams> {
	private static final String HOOK_ATTRIBUTE = "hook";
	
	private ProxySettings proxy;
	
	@Getter
	private static class TeamsMessage {
		@JsonProperty("@type")
		private String type="MessageCard";
		@JsonProperty("@context")
		private String context="http://schema.org/extensions";
		private String themeColor;
		private String summary;
		private List<TeamsSection> sections;
		
		private TeamsMessage(String app, String env, URI uri, String cause) {
			this.themeColor = cause==null ? "00FF00" : "FF0000";
			this.summary = String.format("%s %s is %s", app, env, cause==null?"up":"down");
			this.sections = Collections.singletonList(new TeamsSection(app, env, uri, cause));
		}
	}
	
	@Getter
	private static class TeamsSection {
		private String activityTitle;
		@JsonInclude(Include.NON_NULL)
		private String activityImage;
		private List<TeamsFact> facts;
		@JsonProperty("mardown")
		private boolean mardown = true;
		
		public TeamsSection(String app, String env, URI uri, String cause) {
			final String appName = app==null ? "an application" : app;
			final String id = env==null ? appName : appName+" "+env;
			this.activityTitle=String.format("State of %s changed", id);
			this.facts = new ArrayList<>();
			if (app!=null) {
				facts.add(new TeamsFact("Application",app));
			}
			if (env!=null) {
				facts.add(new TeamsFact("Environment",env));
			}
			facts.add(new TeamsFact("URI",String.format("[%s](%s)",uri.toString(),uri.toString())));
			facts.add(new TeamsFact("Status",cause==null?"UP":"DOWN"));
			if (cause!=null) {
				facts.add(new TeamsFact("Cause",cause));
			}
		}
	}
	
	@AllArgsConstructor
	@Getter
	private static class TeamsFact {
		private String name;
		private String value;
	}

	@Getter
	@EqualsAndHashCode
	@AllArgsConstructor
	public static class ServiceParams {
		private URI webhook;
	}

	public TeamsAlerter(Map<String, Object> parameters) {
		super(parameters);
		this.proxy = ProxySettings.build(parameters);
	}

	@Override
	public ServiceParams verify(Map<String, Object> serviceParameters) {
		if (serviceParameters==null) {
			throw new IllegalArgumentException("Parameters can't be null");
		}
		final Object hook = serviceParameters.get(HOOK_ATTRIBUTE);
		if (hook==null) {
			throw new IllegalArgumentException("'"+HOOK_ATTRIBUTE+"' attribute is missing");
		}
		if (hook instanceof String) {
			try {
				return new ServiceParams(new URI((String)hook));
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(String.format("%s is not a valid hook url", hook), e);
			}
		} else {
			throw new IllegalArgumentException(String.format("%s attribute should be a string", HOOK_ATTRIBUTE));
		}
	}

	@Override
	public void alert(ServiceInfo info, ServiceParams config, String cause) {
		final TeamsMessage mess = new TeamsMessage(info.getApp(), info.getEnv(), info.getUri(), cause);
		final HttpClientBuilder builder = HttpClients.custom();
		if (this.proxy.isProxyRequired(config.webhook)) {
			builder.setRoutePlanner(this.proxy.getProxy());
		}
		CloseableHttpClient client = builder.build();
		try {
			final RequestBuilder reqBuilder = RequestBuilder.post(config.webhook);
			reqBuilder.addHeader("Content-Type","application/json");
			reqBuilder.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(mess)));
			HttpUriRequest req = reqBuilder.build();
			int code = client.execute(req).getStatusLine().getStatusCode();
			if (code!=200) {
				throw new IOException("Status code is "+code);
			}
		} catch (IOException e) {
			log.error(String.format("Error while posting Teams message for %s %s to %s",info.getApp(), info.getEnv(), config.webhook), e);
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				log.warn("Error while closing http client",e);
			}
		}
	}
}
