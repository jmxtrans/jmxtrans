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

import com.google.inject.Injector;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.guice.JmxTransModule;

import java.io.File;
import java.net.URISyntaxException;

public class JmxTransDummyApp {
    private final String jsonFile;
    private JmxTransformer jmxTransformer;

    public JmxTransDummyApp(String jsonFile) {
        this.jsonFile = jsonFile;
    }

    public void start() throws LifecycleException, URISyntaxException {
        JmxTransConfiguration configuration = new JmxTransConfiguration();
        configuration.setRunPeriod(1);
        File file = file(jsonFile);
        configuration.setProcessConfigFile(file);
        Injector injector = JmxTransModule.createInjector(configuration);
        jmxTransformer = injector.getInstance(JmxTransformer.class);
        jmxTransformer.start();
    }

    private File file(String filename) throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(filename).toURI());
    }

    public void stop() throws LifecycleException {
        jmxTransformer.stop();
    }

    public static void main(String[] args) throws LifecycleException, URISyntaxException, InterruptedException {
        new JmxTransDummyApp(args[0]).start();
        Thread.sleep(Long.valueOf(args[1])*1000L);
    }
}
