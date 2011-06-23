package com.googlecode.jmxtrans.model.output;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.pool.KeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.SocketDetails;
import com.googlecode.jmxtrans.util.ValidationException;

/**
 * This output writer sends data to a host/port combination
 * in the Graphite format.
 *
 * @see <a href="http://graphite.wikidot.com/getting-your-data-into-graphite">http://graphite.wikidot.com/getting-your-data-into-graphite</a>
 *
 * @author jon
 */
public class GraphiteWriter extends BaseOutputWriter {

    private static final Logger log = LoggerFactory.getLogger(GraphiteWriter.class);
    public static final String ROOT_PREFIX = "rootPrefix";

    private String host;
    private Integer port;
    private String rootPrefix = "servers";

    private Map<String, KeyedObjectPool> poolMap;
    private KeyedObjectPool pool;
    private SocketDetails details;

    /**
     * Uses JmxUtils.getDefaultPoolMap()
     */
    public GraphiteWriter() {
        this.poolMap = JmxUtils.getDefaultPoolMap();
    }

    /** */
    public GraphiteWriter(Map<String, KeyedObjectPool> poolMap) {
        this.poolMap = poolMap;
    }

    /** */
    public void validateSetup(Query query) throws ValidationException {
        host = (String) this.getSettings().get(HOST);
        Object portObj = this.getSettings().get(PORT);
        if (portObj instanceof String) {
            port = Integer.parseInt((String)portObj);
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

        this.details = new SocketDetails(host, port);
        this.pool = poolMap.get(Server.SOCKET_FACTORY_POOL);
    }

    /** */
    public void doWrite(Query query) throws Exception {

        Socket socket = (Socket) pool.borrowObject(details);
        try {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            for (Result r : query.getResults()) {
                if (isDebugEnabled()) {
                    log.debug(r.toString());
                }
                Map<String, Object> resultValues = r.getValues();
                if (resultValues != null) {
                    for (Entry<String, Object> values : resultValues.entrySet()) {
                        if (JmxUtils.isNumeric(values.getValue())) {
                            String keyStr = null;
                            if (values.getKey().startsWith(r.getAttributeName())) {
                                keyStr = values.getKey();
                            } else {
                                keyStr = r.getAttributeName() + "." + values.getKey();
                            }

                            String alias = null;
                            if (query.getServer().getAlias() != null) {
                                alias = query.getServer().getAlias();
                            } else {
                                alias = query.getServer().getHost() + "_" + query.getServer().getPort();
                                alias = cleanupStr(alias);
                            }

                            StringBuilder sb = new StringBuilder();
                            sb.append(rootPrefix);
                            sb.append(".");
                            sb.append(alias);
                            sb.append(".");

                            // Allow people to use something other than the classname as the output.
                            if (r.getClassNameAlias() != null) {
                                sb.append(r.getClassNameAlias());
                            } else {
                                sb.append(cleanupStr(r.getClassName()));
                            }

                            sb.append(".");

                            String typeName = cleanupStr(getConcatedTypeNameValues(r.getTypeName()));
                            if (typeName != null) {
                                sb.append(typeName);
                                sb.append(".");
                            }
                            sb.append(cleanupStr(keyStr));

                            sb.append(" ");
                            sb.append(values.getValue());
                            sb.append(" ");
                            sb.append(r.getEpoch() / 1000);
                            sb.append("\n");

                            String line = sb.toString();
                            if (isDebugEnabled()) {
                                log.debug("Graphite Message: " + line.trim());
                            }
                            writer.write(line);
                            writer.flush();
                        }
                    }
                }
            }
        } finally {
            pool.returnObject(details, socket);
        }
    }

    /**
     * Allows one to set the object pool for socket connections to graphite
     */
    @Override
    public void setObjectPoolMap(Map<String, KeyedObjectPool> poolMap) {
        this.poolMap = poolMap;
    }
}
