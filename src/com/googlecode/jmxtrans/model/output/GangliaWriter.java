package com.googlecode.jmxtrans.model.output;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.KeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.ValidationException;

/**
 * A cool writer for Ganglia. This is heavily inspired and somewhat copied
 * from code in Hadoop (GangliaContext).
 *
 * @author jon
 */
public class GangliaWriter extends BaseOutputWriter {

    private static final Logger log = LoggerFactory.getLogger(GangliaWriter.class);

    private static final String DEFAULT_UNITS = "";
    private static final int DEFAULT_TMAX = 60;
    private static final int DEFAULT_DMAX = 0;
    private static final int DEFAULT_PORT = 8649;
    private static final int BUFFER_SIZE = 1500;       // as per libgmond.c
    public static final String GROUP_NAME = "groupName";

    protected byte[] buffer = new byte[BUFFER_SIZE];
    protected int offset;

    private Map<String, KeyedObjectPool> poolMap;
    private KeyedObjectPool pool;
    private InetSocketAddress address;
    private String groupName;

    /** */
    public GangliaWriter() {
        this.poolMap = JmxUtils.getDefaultPoolMap();
    }

    /** */
    public GangliaWriter(Map<String, KeyedObjectPool> poolMap) {
        this.poolMap = poolMap;
    }

    /**
     * Allows one to set the object pool for socket connections to graphite
     */
    @Override
    public void setObjectPoolMap(Map<String, KeyedObjectPool> poolMap) {
        this.poolMap = poolMap;
    }

    /**
     * Validates the host/port and does some other setup.
     */
    public void validateSetup(Query query) throws ValidationException {
        Integer port = DEFAULT_PORT;
        String host = (String) this.getSettings().get(HOST);
        Object portObj = this.getSettings().get(PORT);
        if (portObj instanceof String) {
            port = Integer.parseInt((String)portObj);
        } else if (portObj instanceof Integer) {
            port = (Integer) portObj;
        }

        if (host == null) {
            throw new ValidationException("Host can't be null", query);
        }

        groupName = (String) this.getSettings().get("groupName");

        address = new InetSocketAddress(host, port);

        pool = this.poolMap.get(Server.DATAGRAM_SOCKET_FACTORY_POOL);
    }

    public void doWrite(Query query) throws Exception {
        DatagramSocket socket = (DatagramSocket)pool.borrowObject(address);
        try {

            List<String> typeNames = this.getTypeNames();

            String spoofedHostname = null;
            if (StringUtils.isNotEmpty(query.getServer().getAlias())) {
                spoofedHostname = query.getServer().getAlias();
            } else {
                spoofedHostname = query.getServer().getHost();
            }

            for (Result result : query.getResults()) {
                if (isDebugEnabled()) {
                    log.debug(result.toString());
                }
                Map<String, Object> resultValues = result.getValues();
                if (resultValues != null) {
                    for (Entry<String, Object> values : resultValues.entrySet()) {
                        if (JmxUtils.isNumeric(values.getValue())) {

                            emitMetric(socket, spoofedHostname,
                                    JmxUtils.getKeyString2(query, result, values, typeNames, null),
                                    "int32",
                                    values.getValue().toString());
                        }
                    }
                }
            }

        } finally {
            pool.returnObject(address, socket);
        }
    }

    protected void emitMetric(DatagramSocket socket, String hostName, String name, String type, String value)
            throws IOException
    {
        if (name == null) {
            log.warn("Metric was emitted with no name.");
            return;
        } else if (value == null) {
            log.warn("Metric name " + name + " was emitted with a null value.");
            return;
        } else if (type == null) {
            log.warn("Metric name " + name + ", value " + value + " has no type.");
            return;
        }

        // Not sure why this is necessary, but the hostname needs to be after a : for it to
        // show up correctly in the ui.
        hostName = "x:" + hostName;

        // The following XDR recipe was done through a careful reading of
        // gm_protocol.x in Ganglia 3.1 and carefully examining the output of
        // the gmetric utility with strace.

        // First we send out a metadata message
        offset = 0;
        xdr_int(128); // metric_id = metadata_msg
        xdr_string(hostName); // hostname
        xdr_string(name); // metric name
        xdr_int(1); // spoof = True
        xdr_string(type); // metric type
        xdr_string(name); // metric name
        xdr_string(DEFAULT_UNITS); // units
        xdr_int(3); // slope see gmetric.c
        xdr_int(DEFAULT_TMAX); // tmax, the maximum time between metrics
        xdr_int(DEFAULT_DMAX); // dmax, the maximum data value

        /*
         * Num of the entries in extra_value field for
         * Ganglia 3.1.x
         */
        if (StringUtils.isNotEmpty(groupName)) {
            xdr_int(1);
            xdr_string("GROUP"); /* Group attribute */
            xdr_string(groupName); /* Group value */
        } else {
            xdr_int(0);
        }

        DatagramPacket packet = new DatagramPacket(buffer, offset, address);
        socket.send(packet);

        // Now we send out a message with the actual value.
        // Technically, we only need to send out the metadata message once for
        // each metric, but I don't want to have to record which metrics we did and
        // did not send.
        offset = 0;
        xdr_int(133); // we are sending a string value
        xdr_string(hostName); // hostName
        xdr_string(name); // metric name
        xdr_int(1); // spoof = True
        xdr_string("%s"); // format field
        xdr_string(value); // metric value

        packet = new DatagramPacket(buffer, offset, address);
        socket.send(packet);

        log.debug("Emitting metric " + name + ", type " + type + ", value " + value + " for host: " + hostName);
    }

    /**
     * Puts a string into the buffer by first writing the size of the string
     * as an int, followed by the bytes of the string, padded if necessary to
     * a multiple of 4.
     */
    protected void xdr_string(String s) {
        byte[] bytes = s.getBytes();
        int len = bytes.length;
        xdr_int(len);
        System.arraycopy(bytes, 0, buffer, offset, len);
        offset += len;
        pad();
    }

    /**
     * Pads the buffer with zero bytes up to the nearest multiple of 4.
     */
    private void pad() {
        int newOffset = ((offset + 3) / 4) * 4;
        while (offset < newOffset) {
            buffer[offset++] = 0;
        }
    }

    /**
     * Puts an integer into the buffer as 4 bytes, big-endian.
     */
    protected void xdr_int(int i) {
        buffer[offset++] = (byte) ((i >> 24) & 0xff);
        buffer[offset++] = (byte) ((i >> 16) & 0xff);
        buffer[offset++] = (byte) ((i >> 8) & 0xff);
        buffer[offset++] = (byte) (i & 0xff);
    }

}
