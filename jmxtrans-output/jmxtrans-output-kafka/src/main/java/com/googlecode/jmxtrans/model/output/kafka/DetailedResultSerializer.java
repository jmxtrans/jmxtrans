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
package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;
import static java.util.Collections.singletonList;

public class DetailedResultSerializer implements ResultSerializer {
    private final ObjectMapper objectMapper;
    private final boolean singleValue;

    @JsonCreator
    public DetailedResultSerializer(@JsonProperty(value = "singleValue", defaultValue = "false") boolean singleValue) {
        this.objectMapper = new ObjectMapper();
        this.singleValue = singleValue;
    }

    @Nonnull
    @Override
    public Collection<String> serialize(final Server server, final Query query, final Result result) throws IOException {
        if (singleValue) {
            List<String> messages = new ArrayList<>();
            for (String valueName : result.getValues().keySet()) {
                messages.add(objectMapper.writeValueAsString(new SingleValueResult(server, result, valueName)));
            }
            return messages;
        } else {
            return singletonList(objectMapper.writeValueAsString(new MultiValuesResult(server, result)));
        }
    }

    /**
     * DTO containing server and result information
     */
    @JsonSerialize(include = NON_NULL)
    @Immutable
    @ThreadSafe
    private static abstract class KResult {
        // Server
        @Getter
        private final String alias;
        @Getter
        private final String pid;
        @Getter
        private final String host;
        @Getter
        private final String port;
        // Result
        @Getter
        private final String attributeName;
        @Getter
        private final String className;
        @Getter
        private final String objDomain;
        @Getter
        private final String typeName;
        @Getter
        private final long epoch;
        @Getter
        private final String keyAlias;

        public KResult(Server server, Result result) {
            alias = server.getAlias();
            pid = server.getPid();
            host = server.getHost();
            port = server.getPort();
            attributeName = result.getAttributeName();
            className = result.getClassName();
            objDomain = result.getObjDomain();
            typeName = result.getTypeName();
            epoch = result.getEpoch();
            keyAlias = result.getKeyAlias();
        }
    }

    @JsonSerialize(include = NON_NULL)
    @Immutable
    @ThreadSafe
    private static class MultiValuesResult extends KResult {
        @Getter
        private final ImmutableMap<String, Object> values;

        public MultiValuesResult(Server server, Result result) {
            super(server, result);
            this.values = result.getValues();
        }
    }

    @JsonSerialize(include = NON_NULL)
    @Immutable
    @ThreadSafe
    private static class SingleValueResult extends KResult {
        @Getter
        private final String valueName;
        @Getter
        private final Object value;

        public SingleValueResult(Server server, Result result, String valueName) {
            super(server, result);
            this.valueName = valueName;
            this.value = result.getValues().get(valueName);
        }
    }
}
