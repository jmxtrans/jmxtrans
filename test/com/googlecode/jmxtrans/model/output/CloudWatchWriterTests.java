package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import org.junit.Test;

/**
 * Tests for {@link com.googlecode.jmxtrans.model.output.CloudWatchWriter}.
 *
 * @author Sascha Moellering <moelleri@amazon.de>
 */
public class CloudWatchWriterTests {

    /** Test validation when no parameter is set. */
    @Test(expected = NullPointerException.class)
    public void testValidationWithoutSettings() throws ValidationException {

        //new CloudWatchWriter().validateSetup(null, new Query("test"));

        CloudWatchWriter writer = CloudWatchWriter.builder().setNamespace("testNS").build();
        Query test = Query.builder()
                .setObj("test")
                .build();
        Server server = Server.builder().setHost("localhost").setPort("123").build();
        writer.validateSetup(server, test);
    }


}
