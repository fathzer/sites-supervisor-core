package com.fathzer.checkmysites.alerter;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.checkmysites.Configuration.ServiceInfo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EMailAlerter extends Alerter<EMailAlerter.ServiceParams> {
	private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

	private Params settings;
	private InternalSettings internal;
	
	@Getter
	@EqualsAndHashCode
	public static class ServiceParams {
		private InternetAddress[] to;

		public ServiceParams(Collection<InternetAddress> tos) {
			this.to = tos.stream().sorted(new Comparator<InternetAddress>() {
				@Override
				public int compare(InternetAddress o1, InternetAddress o2) {
					return o1.toString().compareTo(o2.toString());
				}
			}).toArray(InternetAddress[]::new);
		}
	}
	
	@Getter
	private static class Params {
		private String host;
		private int port;
		private Boolean secured;
		private String user;
		private String password;
		private String from;
	}
	
	private static class InternalSettings {
		private static final int SECURED_PORT = 587;
		private static final int DEFAULT_PORT = 25;
		
		private Properties props;
		private boolean isSecured;
		private String user;
		private String password;
		private InternetAddress from;
		
		private InternalSettings(Params params) {
			if (params.getHost()==null) {
				throw new IllegalArgumentException("EMailAlerter requires host to be defined");
			}
			this.isSecured = (params.getSecured()!=null && params.getSecured()) ||
					(params.getSecured()==null && params.getPort()==SECURED_PORT);
			int port = params.getPort();
			if (port==0) {
				port = isSecured ? SECURED_PORT : DEFAULT_PORT;
			}
			if (port<=0 || port>65535) {
				throw new IllegalArgumentException("port should be in the range 1 to 65535");
			}
			InetSocketAddress address = new InetSocketAddress(params.getHost(), port);
			if (address.isUnresolved()) {
				throw new IllegalArgumentException(String.format("host %s is unreachable", params.getHost()));
			}
			if (isSecured && (params.getUser()==null || params.getPassword()==null)) {
				throw new IllegalArgumentException("user and password should be defined when secured is true");
			}
			if (params.getFrom()==null) {
				throw new IllegalArgumentException("EMailAlerter requires from address to be defined");
			}
			try {
				this.from = new InternetAddress(params.getFrom());
			} catch (AddressException e) {
				throw new IllegalArgumentException("Illegal from address",e);
			}
			this.props = new Properties();
			props.put("mail.smtp.host", params.getHost());
			props.put("mail.smtp.port", Integer.toString(port));
			if (isSecured) {
				props.put("mail.smtp.starttls.enable","true");
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.socketFactory.class", SSL_FACTORY);
			}
			this.user = params.getUser();
			this.password = params.getPassword();
		}
	}

	public EMailAlerter(Map<String, Object> parameters) {
		super(parameters);
		if (parameters==null) {
			throw new IllegalArgumentException("EmailAlerter requires parameters");
		}
		this.settings = new ObjectMapper().convertValue(parameters, Params.class);
		this.internal = new InternalSettings(settings);
	}

	@Override
	public ServiceParams verify(Map<String, Object> serviceParameters) {
		try {
			Collection<?> tos = (Collection<?>) serviceParameters.get("to");
			final Collection<InternetAddress> to = tos.stream().map(add -> {
				try {
					return new InternetAddress(add.toString());
				} catch (AddressException e) {
					throw new IllegalArgumentException(String.format("%s is not a valid eMail address", add));
				}
			}).collect(Collectors.toList());
			return new ServiceParams(to);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("to is not a collection of email address");
		}
	}

	@Override
	public void alert(ServiceInfo info, ServiceParams config, String cause) {
		log.info(String.format("%s state has changed to %s", info.getUri(), (cause==null?"ok":"ko"), cause));
		Session session = Session.getDefaultInstance(internal.props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(internal.user, internal.password);
			}
		});

		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(internal.from);
			msg.setSubject(String.format("%s alert from %s/%s %s",cause==null?"Positive":"Negative",info.getApp(),info.getEnv(),info.getUri()));
			String body;
			if (cause==null) {
				body = String.format("The service at %s is up again :-)",info.getUri());
			} else {
				body = String.format("The service at %s is down.\n It returned %s",info.getUri(), cause);
			}
			msg.setContent(body, "text/plain; charset=UTF-8"); //TODO
	
			msg.setRecipients(Message.RecipientType.TO, config.getTo());
	
			Transport.send(msg);
		} catch (MessagingException e) {
			log.error("Error while sending mail", e);
		}
	}
}
