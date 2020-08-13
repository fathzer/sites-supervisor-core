package com.fathzer.sitessupervisor.commons;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProxySettings {
	private static final String NOT_STRING_ERR = "%s used as %s attribute is not a String";
	private static final String PROXY_ATTRIBUTE = "proxy";
	private static final String NO_PROXY_ATTRIBUTE = "noProxy";

	private DefaultProxyRoutePlanner proxy;
	private List<String> noProxy;
	
	public static ProxySettings build(Map<String, Object> params) {
		final ProxySettings result = new ProxySettings();
		if (params!=null && params.containsKey(PROXY_ATTRIBUTE)) {
			try {
				String addressString = (String)params.get(PROXY_ATTRIBUTE);
				final InetSocketAddress address = new InetSocketAddress(addressString.substring(0, addressString.lastIndexOf(':')),
					  Integer.parseInt(addressString.substring(addressString.lastIndexOf(':')+1)));
				if (address.isUnresolved()) {
					throw new IllegalArgumentException(String.format("The proxy server address (%s) is unknown",address.getHostString()));
				}
				result.setProxy(new DefaultProxyRoutePlanner(new HttpHost(address.getAddress())));
			} catch (ClassCastException e) {
				throw new IllegalArgumentException(String.format(NOT_STRING_ERR, params.get(PROXY_ATTRIBUTE)));
			} catch (StringIndexOutOfBoundsException e) {
				throw new IllegalArgumentException(String.format("%s attribute (%s) does not comply with expected format (host:port)", PROXY_ATTRIBUTE, params.get(PROXY_ATTRIBUTE)));
			}
			if (params.containsKey(NO_PROXY_ATTRIBUTE)) {
				try {
					result.setNoProxy(Arrays.asList(((String)params.get(NO_PROXY_ATTRIBUTE)).split(",")));
				} catch (ClassCastException e) {
					throw new IllegalArgumentException(String.format(NOT_STRING_ERR, params.get(NO_PROXY_ATTRIBUTE)));
				}
			}
		}
		return result;
	}

	public boolean isProxyRequired(URI uri) {
		if (getProxy()==null) {
			return false;
		}
		if (getNoProxy()==null) {
			return true;
		}
		for (String suffix : getNoProxy()) {
			if (uri.getHost().endsWith(suffix)) {
				return false;
			}
		}
		return true;
	}
}