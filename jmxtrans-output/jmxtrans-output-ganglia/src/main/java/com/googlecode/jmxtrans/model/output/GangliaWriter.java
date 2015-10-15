/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.results.CPrecisionValueTransformer;
import com.googlecode.jmxtrans.model.results.ValueTransformer;
import info.ganglia.gmetric4j.gmetric.GMetric;
import info.ganglia.gmetric4j.gmetric.GMetricSlope;
import info.ganglia.gmetric4j.gmetric.GMetricType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static info.ganglia.gmetric4j.gmetric.GMetric.UDPAddressingMode;

/**
 * {@link com.googlecode.jmxtrans.model.OutputWriter} for <a href="http://ganglia.sourceforge.net">Ganglia</a>.
 *
 * @author Julien Nicoulaud <http://github.com/nicoulaj>
 * @author jon
 */
public class GangliaWriter extends BaseOutputWriter {

	private static final Pattern PATTERN_HOST_IP = Pattern.compile("(.+):([^:]+)$");

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory.getLogger(GangliaWriter.class);

	/* Settings configuration keys. */
	public static final String ADDRESSING_MODE = "addressingMode";
	public static final String TTL = "ttl";
	public static final String V31 = "v3.1";
	public static final String UNITS = "units";
	public static final String SLOPE = "slope";
	public static final String TMAX = "tmax";
	public static final String DMAX = "dmax";
	public static final String GROUP_NAME = "groupName";
	public static final String SPOOF_NAME = "spoofedHostName";

	/* Settings default values. */
	public static final int DEFAULT_PORT = 8649;
	public static final UDPAddressingMode DEFAULT_ADDRESSING_MODE = UDPAddressingMode.UNICAST;
	public static final int DEFAULT_TTL = 5;
	public static final boolean DEFAULT_V31 = true;
	public static final String DEFAULT_UNITS = "";
	public static final GMetricSlope DEFAULT_SLOPE = GMetricSlope.BOTH;
	public static final int DEFAULT_DMAX = 0;
	public static final int DEFAULT_TMAX = 60;
	public static final String DEFAULT_GROUP_NAME = "JMX";

	/* Settings run-time values. */
	private final String host;
	private final int port;
	private final UDPAddressingMode addressingMode;
	private final int ttl;
	private final boolean v31;
	private final String units;
	private final GMetricSlope slope;
	private final int tmax;
	private final int dmax;
	private final String groupName;

	private String spoofedHostName = null;

	private final ValueTransformer valueTransformer = new CPrecisionValueTransformer();

	@JsonCreator
	public GangliaWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port,
			@JsonProperty("addressingMode") String addressingMode,
			@JsonProperty("ttl") Integer ttl,
			@JsonProperty("v31") Boolean v31,
			@JsonProperty("units") String units,
			@JsonProperty("slope") String slope,
			@JsonProperty("tmax") Integer tmax,
			@JsonProperty("dmax") Integer dmax,
			@JsonProperty("groupName") String groupName,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.host = MoreObjects.firstNonNull(host, (String) getSettings().get(HOST));
		this.port = MoreObjects.firstNonNull(
				port,
				Settings.getIntSetting(getSettings(), PORT, DEFAULT_PORT));
		this.addressingMode = computeAddressingMode(firstNonNull(
				addressingMode,
				(String) getSettings().get(ADDRESSING_MODE),
				""
		), this.host);
		this.ttl = MoreObjects.firstNonNull(ttl, Settings.getIntegerSetting(getSettings(), TTL, DEFAULT_TTL));
		this.v31 = MoreObjects.firstNonNull(v31, Settings.getBooleanSetting(getSettings(), V31, DEFAULT_V31));
		this.units = firstNonNull(units, (String) getSettings().get(UNITS), DEFAULT_UNITS);
		this.slope = GMetricSlope.valueOf(firstNonNull(
				slope,
				(String) getSettings().get(SLOPE),
				DEFAULT_SLOPE.name()
		));
		this.tmax = MoreObjects.firstNonNull(tmax, Settings.getIntegerSetting(getSettings(), TMAX, DEFAULT_TMAX));
		this.dmax = MoreObjects.firstNonNull(dmax, Settings.getIntegerSetting(getSettings(), DMAX, DEFAULT_DMAX));
		this.groupName = firstNonNull(
				groupName,
				(String) getSettings().get(GROUP_NAME),
				DEFAULT_GROUP_NAME
		);
	}

	/**
	 * Parse and validate settings.
	 */
	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
		// Determine the spoofed hostname
		spoofedHostName = getSpoofedHostName(server.getHost(), server.getAlias());

		log.debug("Validated Ganglia metric [" +
				HOST + ": " + host + ", " +
				PORT + ": " + port + ", " +
				ADDRESSING_MODE + ": " + addressingMode + ", " +
				TTL + ": " + ttl + ", " +
				V31 + ": " + v31 + ", " +
				UNITS + ": '" + units + "', " +
				SLOPE + ": " + slope + ", " +
				TMAX + ": " + tmax + ", " +
				DMAX + ": " + dmax + ", " +
				SPOOF_NAME + ": " + spoofedHostName + ", " +
				GROUP_NAME + ": '" + groupName + "']");
	}

	private UDPAddressingMode computeAddressingMode(String mode, String host) {
		// Parse and validate addressing mode setting
		try {
			return UDPAddressingMode.valueOf(mode);
		} catch (IllegalArgumentException iae) {
			try {
				return UDPAddressingMode.getModeForAddress(host);
			} catch (UnknownHostException uhe) {
				return DEFAULT_ADDRESSING_MODE;
			} catch (IOException ioe) {
				return DEFAULT_ADDRESSING_MODE;
			}
		}
	}

	/**
	 * Send query result values to Ganglia.
	 */
	@Override
	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		for (final Result result : results) {
			if (result.getValues() != null) {
				for (final Map.Entry<String, Object> resultValue : result.getValues().entrySet()) {
					final String name = KeyUtils.getKeyString(query, result, resultValue, getTypeNames());

					Object transformedValue = valueTransformer.apply(resultValue.getValue());

					GMetricType dataType = getType(resultValue.getValue());
					log.debug("Sending Ganglia metric {}={} [type={}]", name, transformedValue, dataType);
					new GMetric(host, port, addressingMode, ttl, v31, null, spoofedHostName)
							.announce(name, transformedValue.toString(), dataType, units, slope, tmax, dmax, groupName);
				}
			}
		}
	}

	/**
	 * Determines the spoofed host name to be used when emitting metrics to a
	 * gmond process. Spoofed host names are of the form IP:hostname.
	 *
	 * @param host  the host of the gmond (ganglia monitor) to which we are
	 *              connecting, not null
	 * @param alias the custom alias supplied, may be null
	 * @return the host name to use when emitting metrics, in the form of
	 * IP:hostname
	 */
	public static String getSpoofedHostName(String host, String alias) {
		// Determine the host name to use as the spoofed host name, this should
		// be of the format IP:hostname
		String spoofed = host;
		if (StringUtils.isNotEmpty(alias)) {
			// If the alias was supplied in the appropriate format, use it
			// directly
			Matcher hostIpMatcher = PATTERN_HOST_IP.matcher(alias);
			if (hostIpMatcher.matches())
				return alias;
			// Otherwise, use the alias as the host
			spoofed = alias;
		}
		// Attempt to find the IP of the given host (this may be an aliased
		// host)
		try {
			return InetAddress.getByName(spoofed).getHostAddress() + ":" + spoofed;
		} catch (UnknownHostException e) {
			// ignore failure to resolve spoofed host
		}
		// Attempt to return the local host IP with the spoofed host name
		try {
			return InetAddress.getLocalHost().getHostAddress() + ":" + spoofed;
		} catch (UnknownHostException e) {
			// ignore failure to resolve spoofed host
		}
		// We failed to resolve the spoofed host or our local host, return "x"
		// for the IP
		return "x:" + spoofed;
	}

	/**
	 * Guess the Ganglia gmetric type to use for a given object.
	 *
	 * @param obj the object to inspect
	 * @return an appropriate {@link GMetricType}, {@link GMetricType#STRING} by default
	 */
	private static GMetricType getType(final Object obj) {

		// FIXME This is far from covering all cases.
		// FIXME Wasteful use of high capacity types (eg Short => INT32)

		// Direct mapping when possible
		if (obj instanceof Long || obj instanceof Integer || obj instanceof Byte || obj instanceof Short)
			return GMetricType.INT32;
		if (obj instanceof Float)
			return GMetricType.FLOAT;
		if (obj instanceof Double)
			return GMetricType.DOUBLE;

		// Convert to double or int if possible
		try {
			Double.parseDouble(obj.toString());
			return GMetricType.DOUBLE;
		} catch (NumberFormatException e) {
			// Not a double
		}
		try {
			Integer.parseInt(obj.toString());
			return GMetricType.UINT32;
		} catch (NumberFormatException e) {
			// Not an int
		}

		return GMetricType.STRING;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getAddressingMode() {
		return addressingMode.name();
	}

	public int getTtl() {
		return ttl;
	}

	public boolean isV31() {
		return v31;
	}

	public String getUnits() {
		return units;
	}

	public GMetricSlope getSlope() {
		return slope;
	}

	public int getTmax() {
		return tmax;
	}

	public int getDmax() {
		return dmax;
	}

	public String getGroupName() {
		return groupName;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final ImmutableList.Builder<String> typeNames = ImmutableList.builder();
		private boolean booleanAsNumber;
		private Boolean debugEnabled;
		private String host;
		private Integer port;
		private String addressingMode;
		private Integer ttl;
		private Boolean v31;
		private String units;
		private String slope;
		private Integer tmax;
		private Integer dmax;
		private String groupName;

		private Builder() {
		}

		public Builder addTypeNames(List<String> typeNames) {
			this.typeNames.addAll(typeNames);
			return this;
		}

		public Builder addTypeName(String typeName) {
			typeNames.add(typeName);
			return this;
		}

		public Builder setBooleanAsNumber(boolean booleanAsNumber) {
			this.booleanAsNumber = booleanAsNumber;
			return this;
		}

		public Builder setDebugEnabled(boolean debugEnabled) {
			this.debugEnabled = debugEnabled;
			return this;
		}

		public Builder setHost(String host) {
			this.host = host;
			return this;
		}

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public Builder setAddressingMode(String addressingMode) {
			this.addressingMode = addressingMode;
			return this;
		}

		public Builder setTtl(Integer ttl) {
			this.ttl = ttl;
			return this;
		}

		public Builder setV31(Boolean v31) {
			this.v31 = v31;
			return this;
		}

		public Builder setUnits(String units) {
			this.units = units;
			return this;
		}

		public Builder setSlope(String slope) {
			this.slope = slope;
			return this;
		}

		public Builder setTmax(Integer tmax) {
			this.tmax = tmax;
			return this;
		}

		public Builder setDmax(Integer dmax) {
			this.dmax = dmax;
			return this;
		}

		public Builder setGroupName(String groupName) {
			this.groupName = groupName;
			return this;
		}

		public GangliaWriter build() {
			return new GangliaWriter(
					typeNames.build(),
					booleanAsNumber,
					debugEnabled,
					host,
					port,
					addressingMode,
					ttl,
					v31,
					units,
					slope,
					tmax,
					dmax,
					groupName,
					null);
		}

	}

}
