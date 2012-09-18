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

import java.net.UnknownHostException;
import java.util.Map;

import static ganglia.gmetric.GMetric.UDPAddressingMode;

/**
 * {@link com.googlecode.jmxtrans.OutputWriter} for <a href="http://ganglia.sourceforge.net">Ganglia</a>.
 *
 * @author Julien Nicoulaud <http://github.com/nicoulaj>
 * @author jon
 */
public class GangliaWriter extends BaseOutputWriter {

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
                    log.debug("Sending Ganglia metric {}={}", host+": "+ name, value);
                    new GMetric(
                            host,
                            port,
                            addressingMode,
                            ttl,
                            v31,
                            null,
                            "muvpl018:muvpl018.eu.mscsoftware.com"
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
