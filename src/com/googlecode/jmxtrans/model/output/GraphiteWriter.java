package com.googlecode.jmxtrans.model.output;

import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
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
    
    public static final String TYPE_NAMES = "typeNames";
    private List<String> typeNames = null;
    
    public GraphiteWriter() {
    }
    
    public void validateSetup() throws Exception {
        host = (String) this.getSettings().get(HOST);
        port = (Integer) this.getSettings().get(PORT);
        
        if (host == null || port == null) {
            throw new RuntimeException("Host and port can't be null");
        }
    }

    public String cleanupName(String name) {
        if (name == null) {
            return null;
        }
        String clean = name.replace('.', '_');
        clean = clean.replace(" ", "");
        return clean;
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
                            }
                            alias = cleanupName(alias);

                            StringBuilder sb = new StringBuilder();
                            sb.append("servers.");
                            sb.append(alias);
                            sb.append(".");
                            sb.append(cleanupName(r.getClassName()));
                            sb.append(".");

                            String typeName = cleanupName(handleTypeName(r.getTypeName()));
                            if (typeName != null) {
                                sb.append(typeName);
                                sb.append(".");
                            }
                            sb.append(cleanupName(keyStr));

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

    private String handleTypeName(String typeName) {
        if (getTypeNames() == null && getTypeNames().size() == 0) {
            return null;
        }
        String[] tokens = typeName.split(",");
        boolean foundIt = false;
        for (String token : tokens) {
            String[] keys = token.split("=");
            for (String key : keys) {
                // we want the next item in the array.
                if (foundIt) {
                    return key;
                }
                if (getTypeNames().contains(key)) {
                    foundIt = true;
                }
            }
        }
        return null;
    }

    private Writer connect() throws Exception {
        Socket socket = new Socket(host, port);
        return new PrintWriter(socket.getOutputStream(), true);
    }

    public void setTypeNames(List<String> typeNames) {
        this.getSettings().put(TYPE_NAMES, typeNames);
        this.typeNames = typeNames;
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public List<String> getTypeNames() {
        if (this.typeNames == null) {
            this.typeNames = (List<String>) this.getSettings().get(TYPE_NAMES);
        }
        return this.typeNames;
    }
}
