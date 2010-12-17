package com.googlecode.jmxtrans.model.output;

import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;

/**
 * This output writer sends data to a host/port combination
 * in the Graphite format.
 * 
 * @see <a href="http://graphite.wikidot.com/getting-your-data-into-graphite">http://graphite.wikidot.com/getting-your-data-into-graphite</a>
 * @author jon
 */
public class GraphiteWriter extends BaseOutputWriter {

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
                for (Entry<String, Object> values : r.getValues().entrySet()) {
                    String fullPath = "servers." + query.getServer().getHost().replace('.', '_') + "." + query.getServer().getPort() + "." + r.getDescription() + "." + r.getAttributeName() + "." + values.getKey();
                    String line = fullPath + " " + values.getValue() + " " + r.getEpoch() / 1000 + "\n";
                    writer.write(line);
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
