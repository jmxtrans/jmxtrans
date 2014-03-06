package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

/**
 * OpenTSDBWriter which directly sends
 *
 * Created from sources originally written by Balazs Kossovics <bko@witbe.net>
 */

public class OpenTSDBWriter extends OpenTSDBGenericWriter {
    private static final Logger log = LoggerFactory.getLogger(OpenTSDBWriter.class);

    protected Socket            socket;
    protected DataOutputStream  out;

    /**
     * Add the hostname tag "host" with the name of the host by default since OpenTSDB otherwise does not have this
     * information.
     */
    @Override
    protected boolean   getAddHostnameTagDefault() {
        return  true;
    }

    /**
     * Prepare for sending metrics.
     */
    @Override
    protected void  prepareSender() throws LifecycleException {
        try {
            socket = new Socket(host, port);
        } catch(UnknownHostException e) {
            log.error("error opening socket to OpenTSDB", e);
            throw new LifecycleException(e);
        } catch(IOException e) {
            log.error("error opening socket to OpenTSDB", e);
            throw new LifecycleException(e);
        }
    }

    /**
     * Shutdown the sender as it will no longer be used to send metrics.
     */
    @Override
    protected void  shutdownSender() throws LifecycleException {
        try {
            socket.close();
        } catch (IOException e) {
            log.error("error closing socket to OpenTSDB", e);
            throw new LifecycleException(e);
        }
    }

    /**
     * Start the output for the results of a Query to OpenTSDB.
     */
    @Override
    protected void  startOutput() throws IOException {
        try {
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            log.error("error getting the output stream", e);
            throw e;
        }
    }

    /**
     * Send a single metric to the server.
     *
     * @param   metricLine - the line containing the metric name, value, and tags for a single metric; excludes the
     *          "put" keyword expected by OpenTSDB and the trailing newline character.
     */
    @Override
    protected void  sendOutput (String metricLine) throws IOException {
        try {
            this.out.writeBytes("put " + metricLine + "\n"); 
        } catch (IOException e) {
            log.error("error writing result to the output stream", e);
            throw e;
        }
    }

    /**
     * Finish the output for a single Query, flushing all data to the server and logging the server's response.
     */
    @Override
    protected void  finishOutput () throws IOException {
        try {
            this.out.flush();
        } catch (IOException e) {
            log.error("flush failed", e);
            throw e;
        }

            // Read and log the response from the server for diagnostic purposes.

        InputStreamReader socketInputStream = new InputStreamReader(socket.getInputStream());
        BufferedReader bufferedSocketInputStream = new BufferedReader(socketInputStream);
        String line;
        while (socketInputStream.ready() && (line = bufferedSocketInputStream.readLine()) != null) {
            log.warn("OpenTSDB says: " + line); 
        }
    }
}
