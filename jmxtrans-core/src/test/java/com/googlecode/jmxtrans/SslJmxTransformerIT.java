/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.googlecode.jmxtrans.test.DummyApp;
import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.ExternalApp;
import com.googlecode.jmxtrans.test.RequiresIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.util.concurrent.Callable;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@Category({IntegrationTest.class, RequiresIO.class})
public class SslJmxTransformerIT {
    private final File jmxTransAppStdOut = new File("target" + File.separator + "ssl-jmxtrans-out.log");
    @Rule
    public ExternalApp app = new ExternalApp(DummyApp.class).enableJmx(12346)
            .enableSsl("localhost.jks", "localhost");
    @Rule
    public ExternalApp jmxTransApp = new ExternalApp(JmxTransDummyApp.class, "ssl-integration-test.json", "5")
            .enableSsl("localhost.jks", "localhost")
            .redirectOutputToFile(jmxTransAppStdOut);

    @Test
    public void metricsAreSentToStdout() throws Exception {
        await().atMost(5, SECONDS).until(new JmxTransStdoutContains("value=1"));
        await().atMost(5, SECONDS).until(new JmxTransStdoutContains("value=2"));
    }

    private class JmxTransStdoutContains implements Callable<Boolean> {
        private final String value;

        public JmxTransStdoutContains(String value) {
            this.value = value;
        }

        @Override
        public Boolean call() throws Exception {
            return Files.toString(jmxTransAppStdOut, Charsets.UTF_8).contains(value);
        }
    }
}
