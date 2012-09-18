package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.ValidationException;
import ganglia.gmetric.GMetric;
import ganglia.gmetric.GMetricSlope;
import ganglia.gmetric.GMetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ganglia.gmetric.GMetric.UDPAddressingMode;

/**
 * {@link com.googlecode.jmxtrans.OutputWriter} for <a href="http://ganglia.sourceforge.net">Ganglia</a>.
 *
 * @author Julien Nicoulaud <http://github.com/nicoulaj>
 * @author jon
 */
public class GangliaWriter extends BaseOutputWriter {

	private static final Pattern PATTERN_HOST_IP = Pattern.compile("(.+):([^:]+)$");
	
	/** Logger. */
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
    public static final String DEFAULT_HOST = null;
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
    protected String host = DEFAULT_HOST;
    protected int port = DEFAULT_PORT;
    protected UDPAddressingMode addressingMode = DEFAULT_ADDRESSING_MODE;
    protected int ttl = DEFAULT_TTL;
    protected boolean v31 = DEFAULT_V31;
    protected String units = DEFAULT_UNITS;
    protected GMetricSlope slope = DEFAULT_SLOPE;
    protected int tmax = DEFAULT_TMAX;
    protected int dmax = DEFAULT_DMAX;
    protected String groupName = DEFAULT_GROUP_NAME;
    protected String spoofedHostName = null;

    /** Parse and validate settings. */
    @Override
    public void validateSetup(Query query) throws ValidationException {

        // Parse and validate host setting
        host = getStringSetting(HOST, DEFAULT_HOST);
        if (host == null) throw new ValidationException("Host can't be null", query);

        // Parse and validate port setting
        port = getIntegerSetting(PORT, DEFAULT_PORT);

        // Parse and validate addressing mode setting
        try {
            addressingMode = UDPAddressingMode.valueOf(getStringSetting(ADDRESSING_MODE, ""));
        } catch (IllegalArgumentException iae) {
            try {
                addressingMode = UDPAddressingMode.getModeForAddress(host);
            } catch (UnknownHostException uhe) {
                addressingMode = DEFAULT_ADDRESSING_MODE;
            }
        }

        // Parse and validate TTL setting
        ttl = getIntegerSetting(TTL, DEFAULT_TTL);

        // Parse and validate protocol version setting
        v31 = getBooleanSetting(V31, DEFAULT_V31);

        // Parse and validate unit setting
        units = getStringSetting(UNITS, DEFAULT_UNITS);

        // Parse and validate slope setting
        slope = GMetricSlope.valueOf(getStringSetting(SLOPE, DEFAULT_SLOPE.name()));

        // Parse and validate tmax setting
        tmax = getIntegerSetting(TMAX, DEFAULT_TMAX);

        // Parse and validate dmax setting
        dmax = getIntegerSetting(DMAX, DEFAULT_DMAX);

        // Parse and validate group name setting
        groupName = getStringSetting(GROUP_NAME, DEFAULT_GROUP_NAME);

    	// Determine the spoofed hostname
        spoofedHostName = getSpoofedHostName(query.getServer().getHost(), query.getServer().getAlias());
        
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

    /** Send query result values to Ganglia. */
    @Override
    public void doWrite(Query query) throws Exception {
        for (final Result result : query.getResults()) {
            if (result.getValues() != null) {
                for (final Map.Entry<String, Object> resultValue : result.getValues().entrySet()) {
                    final String name = JmxUtils.getKeyString2(query, result, resultValue, getTypeNames(), null);
                    final String value = resultValue.getValue().toString();
                    log.debug("Sending Ganglia metric {}={}", name, value);
                    new GMetric(
                            host,
                            port,
                            addressingMode,
                            ttl,
                            v31,
                            null,
                            spoofedHostName
                    ).announce(
                            name,
                            value,
                            getType(resultValue.getValue()),
                            units,
                            slope,
                            tmax,
                            dmax,
                            groupName
                    );
                }
            }
        }
    }

    /**
    * Determines the spoofed host name to be used when emitting metrics to a
    * gmond process. Spoofed host names are of the form IP:hostname.
    *
    * @param host
    * the host of the gmond (ganglia monitor) to which we are
    * connecting, not null
    * @param alias
    * the custom alias supplied, may be null
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
}
