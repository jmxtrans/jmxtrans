package com.googlecode.jmxtrans.model;

public final class ServerFixtures {
	private ServerFixtures() {}

	public static Server createServerWithOneQuery(String host, String port, String queryObject) {
		return Server.builder()
				.setHost(host)
				.setPort(port)
				.addQuery(Query.builder()
					.setObj(queryObject)
					.build())
				.build();
	}
}
