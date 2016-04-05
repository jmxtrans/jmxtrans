package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.StringWriter;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

/**
 * Created: 2016/4/5.
 * Author: Qiannan Lu
 */
public class OpenFalconWriterTest {

    @Test
    public void testWrite() throws Exception {
        OpenFalconWriter openFalconWriter = new OpenFalconWriter(new JsonFactory(), ImmutableList.<String>of(), "localhost", "project=falcon,module=judge");

        StringWriter writer = new StringWriter();

        openFalconWriter.write(writer, dummyServer(), dummyQuery(), dummyResults());

        String json = writer.toString();

        assertThatJson(json).isArray().ofLength(1);
    }
}