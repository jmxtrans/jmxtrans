package com.googlecode.jmxtrans.model.output;

import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;

/**
 * This output writer sends data to a host/port combination
 * in the Graphite format.
 * 
 * @see <a href="http://graphite.wikidot.com/getting-your-data-into-graphite">http://graphite.wikidot.com/getting-your-data-into-graphite</a>
 * @author jon
 */
public class GraphiteWriter extends BaseOutputWriter {

    private static final Logger log = LoggerFactory.getLogger(GraphiteWriter.class);

    private String host = null;
    private Integer port = null;
    
    public GraphiteWriter() {
    }
    
    public void validateSetup() throws Exception {
        host = (String) this.getSettings().get(HOST);
        port = (Integer) this.getSettings().get(PORT);
        
        if (host == null || port == null) {
            throw new RuntimeException("Host and port can't be null");
        }
    }

    public void doWrite(Query query) throws Exception {
        Writer writer = connect();
        try {
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
                            sb.append("servers.");
                            sb.append(alias);
                            sb.append(".");
                            sb.append(cleanupStr(r.getClassName()));
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
                        }
                    }
                }
            }
            writer.flush();
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private Writer connect() throws Exception {
        Socket socket = new Socket(host, port);
        return new PrintWriter(socket.getOutputStream(), true);
    }

}
