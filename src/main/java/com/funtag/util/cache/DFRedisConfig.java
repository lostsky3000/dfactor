package com.funtag.util.cache;

public final class DFRedisConfig {

	public final String host;
	public final int port;
	public final String auth;
	
	public DFRedisConfig(String host, int port, String auth) {
		this.host = host;
		this.port = port;
		this.auth = auth;
	}
}
