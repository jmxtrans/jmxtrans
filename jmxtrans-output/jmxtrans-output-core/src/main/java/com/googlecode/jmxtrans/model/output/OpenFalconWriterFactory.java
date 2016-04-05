package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.output.support.HttpOutputWriter;
import com.googlecode.jmxtrans.model.output.support.HttpUrlConnectionConfigurer;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.Nonnull;
import java.net.URL;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created: 2016/4/5.
 * Author: Qiannan Lu
 */
@EqualsAndHashCode
@ToString
public class OpenFalconWriterFactory implements OutputWriterFactory {
    private final boolean booleanAsNumber;
    @Nonnull
    private final ImmutableList<String> typeNames;
    @Nonnull
    private final URL url;

    private final int readTimeoutInMillis;

    private final String endpoint;
    private final String tags;

    public OpenFalconWriterFactory(
            @JsonProperty("typeNames") ImmutableList<String> typeNames,
            @JsonProperty("booleanAsNumber") boolean booleanAsNumber,
            @JsonProperty("url") URL url,
            @JsonProperty("readTimeoutInMillis") Integer readTimeoutInMillis,
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("tags") String tags) {
        this.booleanAsNumber = booleanAsNumber;
        this.typeNames = firstNonNull(typeNames, ImmutableList.<String>of());
        this.url = checkNotNull(url);
        this.readTimeoutInMillis = firstNonNull(readTimeoutInMillis, 0);
        this.endpoint = endpoint;
        this.tags = tags;
    }

    @Override
    @Nonnull
    public ResultTransformerOutputWriter<HttpOutputWriter<OpenFalconWriter>> create() {
        return ResultTransformerOutputWriter.booleanToNumber(
                booleanAsNumber,
                new HttpOutputWriter<>(
                        new OpenFalconWriter(
                                new JsonFactory(),
                                typeNames,
                                endpoint,
                                tags),
                        url,
                        null,
                        new HttpUrlConnectionConfigurer(
                                "POST",
                                readTimeoutInMillis,
                                null,
                                "application/json; charset=utf-8"
                        ),
                        UTF_8
                ));
    }
}
