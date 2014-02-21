package com.googlecode.jmxtrans.model.output;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import com.googlecode.jmxtrans.util.LifecycleException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * {@link com.googlecode.jmxtrans.OutputWriter} for the <a href="https://github.com/OpenTSDB/tcollector/blob/master/collectors/0/udp_bridge.py">TCollector udp_bridge</a>.
 * Largely based on StatsDWriter and OpenTSDBWriter
 *
 * @author kieren
 */
public class TCollectorUDPWriter extends OpenTSDBGenericWriter {
    private static final Logger log = LoggerFactory.getLogger(TCollectorUDPWriter.class);

    protected SocketAddress     address;
    protected Socket            socket;
    protected DataOutputStream  out;

    protected DatagramChannel   channel;
    protected ByteBuffer        sendBuffer;

    /**
     * Prepare for sending metrics.
     */
    @Override
    protected void  prepareSender() throws LifecycleException {
        try {
            channel = DatagramChannel.open();
            setBufferSize((short) 1500);

            this.address = new InetSocketAddress(this.host, this.port);
            tagName = this.getStringSetting("tagName", "");
        } catch(IOException e) {
            log.error("error opening socket to TCollector", e);
            throw new LifecycleException(e);
        }
    }

    /**
     * Shutdown the sender as it will no longer be used to send metrics.
     */
    @Override
    protected void  shutdownSender() throws LifecycleException {
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
    protected void  sendOutput(String metricLine) throws IOException {
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
    protected void  finishOutput() throws IOException {
        try {
            this.out.flush();
        } catch (IOException e) {
            log.error("flush failed");
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

    public synchronized void setBufferSize(short packetBufferSize) {
        if(sendBuffer != null) {
            flush();
        }
        sendBuffer = ByteBuffer.allocate(packetBufferSize);
    }

    protected void  doSend(String stat) {
        try {
            final byte[] data = stat.getBytes("utf-8");

            // If we're going to go past the threshold of the buffer then flush.
            // the +1 is for the potential '\n' in multi_metrics below
            if (sendBuffer.remaining() < (data.length + 1)) {
                flush();
            }

            if (sendBuffer.position() > 0) { // multiple metrics are separated by '\n'
                sendBuffer.put((byte) '\n');
            }

            sendBuffer.put(data); // append the data

            flush();
        } catch (IOException e) {
            log.error("failed to send metrics to TCollector", e);
        }
    }

    public boolean  flush() {
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
