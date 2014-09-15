package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.util.ValidationException;
import org.junit.Test;

/**
 * Tests for {@link com.googlecode.jmxtrans.model.output.CloudWatchWriter}.
 *
 * @author Sascha Moellering <moelleri@amazon.de>
 */
public class CloudWatchWriterTests {

    /** Test validation when no parameter is set. */
    @Test(expected = ValidationException.class)
    public void testValidationWithoutSettings() throws ValidationException {
        new CloudWatchWriter().validateSetup(new Query("test"));
    }


}
