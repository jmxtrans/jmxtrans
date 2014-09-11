package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPool;
import com.googlecode.jmxtrans.jmx.ManagedObject;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.DatagramSocketFactory;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class DogStatsDWriter extends BaseOutputWriter
{
    private static final Logger log = LoggerFactory.getLogger(DogStatsDWriter.class);

    public static final String ROOT_PREFIX = "rootPrefix";
    private ByteBuffer sendBuffer;
    private String host;
    private Integer port;
    /** bucketType defaults to c == counter */
    private String bucketType = "c";
    private String rootPrefix = "servers";
    private SocketAddress address;
    private final DatagramChannel channel;

    private static final String BUCKET_TYPE = "bucketType";

    private KeyedObjectPool pool;
    private ManagedObject mbean;
    private DatagramChannel clientChannel;
    private String[] tags;

    /**
     * Uses JmxUtils.getDefaultPoolMap()
     * @throws java.io.IOException
     */
    public DogStatsDWriter() throws IOException
    {
        channel = DatagramChannel.open();
        setBufferSize((short) 1500);
    }

    /**
     * Generate a suffix conveying the given tag list to the client
     */
    static String tagString(final String[] tags, final String tagPrefix) {
        StringBuilder sb;
        if(tagPrefix != null) {
            if(tags == null || tags.length == 0) {
                return tagPrefix;
            }
            sb = new StringBuilder(tagPrefix);
            sb.append(",");
        } else {
            if(tags == null || tags.length == 0) {
                return "";
            }
            sb = new StringBuilder("|#");
        }

        for(int n=tags.length - 1; n>=0; n--) {
            sb.append(tags[n]);
            if(n > 0) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Generate a suffix conveying the given tag list to the client
     */
    String tagString(final String[] tags) {
        return tagString(tags, null);
    }

    public synchronized void setBufferSize(short packetBufferSize) {
        if(sendBuffer != null) {
            flush();
        }
        sendBuffer = ByteBuffer.allocate(packetBufferSize);
    }

    @Override
    public void start() throws LifecycleException {
        try {
            this.pool = JmxUtils.getObjectPool(new DatagramSocketFactory());
            this.mbean = new ManagedGenericKeyedObjectPool((GenericKeyedObjectPool) pool, Server.DATAGRAM_SOCKET_FACTORY_POOL);
            JmxUtils.registerJMX(this.mbean);
        } catch (Exception e) {
            throw new LifecycleException(e);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            if (this.mbean != null) {
                JmxUtils.unregisterJMX(this.mbean);
                this.mbean = null;
            }
            if (this.pool != null) {
                pool.close();
                this.pool = null;
            }
        } catch (Exception e) {
            throw new LifecycleException(e);
        }
    }

    /** */
    public void validateSetup(Query query) throws ValidationException {
        host = (String) this.getSettings().get(HOST);
        Object portObj = this.getSettings().get(PORT);
        if (portObj instanceof String) {
            port = Integer.parseInt((String) portObj);
        } else if (portObj instanceof Integer) {
            port = (Integer) portObj;
        }

        if (host == null || port == null) {
            throw new ValidationException("Host and port can't be null", query);
        }

        String rootPrefixTmp = (String) this.getSettings().get(ROOT_PREFIX);
        if (rootPrefixTmp != null) {
            rootPrefix = rootPrefixTmp;
        }

        this.address = new InetSocketAddress(host, port);

        if (this.getSettings().containsKey(BUCKET_TYPE)) {
            bucketType = (String) this.getSettings().get(BUCKET_TYPE);
        }

        tags = new String[]{
                format("service:%s", query.getServer().getAlias())
        };
    }

    public void doWrite(Query query) throws Exception {

        List<String> typeNames = this.getTypeNames();

        for (Result result : query.getResults()) {
            if (isDebugEnabled()) {
                log.debug(result.toString());
            }

            Map<String, Object> resultValues = result.getValues();
            if (resultValues != null) {
                for (Map.Entry<String, Object> values : resultValues.entrySet()) {
                    if (JmxUtils.isNumeric(values.getValue())) {
                        StringBuilder sb = new StringBuilder();

                        sb.append(JmxUtils.getKeyString(query, result, values, typeNames, rootPrefix));

                        sb.append(":");
                        sb.append(values.getValue().toString());
                        sb.append("|");
                        sb.append(bucketType);
                        sb.append(tagString(tags));
                        sb.append("\n");

                        String line = sb.toString();

                        if (isDebugEnabled()) {
                            log.debug("StatsD Message: " + line.trim());
                        }

                        doSend(line.trim());
                    }
                }
            }
        }
    }

    private synchronized boolean doSend(String stat) {
        try {
            final byte[] data = stat.getBytes("utf-8");

            // If we're going to go past the threshold of the buffer then flush.
            // the +1 is for the potential '\n' in multi_metrics below
            if (sendBuffer.remaining() < (data.length + 1)) {
                flush();
            }

            if (sendBuffer.position() > 0) { // multiple metrics are separated
                // by '\n'
                sendBuffer.put((byte) '\n');
            }

            sendBuffer.put(data); // append the data

            flush();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean flush() {
        try {
            final int sizeOfBuffer = sendBuffer.position();

            if (sizeOfBuffer <= 0) {
                return false;
            } // empty buffer

            // send and reset the buffer
            sendBuffer.flip();
            final int nbSentBytes = channel.send(sendBuffer, this.address);
            sendBuffer.limit(sendBuffer.capacity());
            sendBuffer.rewind();

            if (sizeOfBuffer == nbSentBytes) {
                return true;
            } else {
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
