package com.googlecode.jmxtrans.fixtures;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;

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
