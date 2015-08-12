package com.googlecode.jmxtrans.model.output.elastic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.StringUtils;
import com.googlecode.jmxtrans.model.output.BaseOutputWriter;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.fasterxml.jackson.core.JsonEncoding.UTF8;
import static com.googlecode.jmxtrans.model.PropertyResolver.resolveProps;
import static com.googlecode.jmxtrans.model.naming.KeyUtils.getKeyString;
import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

/**
 * Feed data directly into elastic.
 *
 * @author Peter Paul Bakker - pp@stokpop.nl
 */

@NotThreadSafe
public class ElasticWriter extends BaseOutputWriter {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticWriter.class);
	
	private static final String DEFAULT_ROOT_PREFIX = "jmxtrans";
    private static final Object CREATE_MAPPING_LOCK = new Object();

    private final JsonFactory jsonFactory;

	private JestClient jestClient = null;

	private final String rootPrefix;
	private final String connectionUrl;
    private static final String TYPE_NAME = "jmx-entry";
    private String indexName;

    @JsonCreator
	public ElasticWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("rootPrefix") String rootPrefix,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("connectionUrl") String connectionUrl,
			@JsonProperty("settings") Map<String, Object> settings) {

		super(typeNames, booleanAsNumber, debugEnabled, settings);

		this.rootPrefix = resolveProps(
				firstNonNull(
						rootPrefix,
						(String) getSettings().get("rootPrefix"),
						DEFAULT_ROOT_PREFIX));		

		this.jsonFactory = new JsonFactory();
		this.connectionUrl = connectionUrl;
        this.indexName = this.rootPrefix + "_jmx-entries";

	}

	@Override
	protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		List<String> typeNames = this.getTypeNames();

		for (Result result : results) {
			log.debug("Query result: [{}]", result);
			Map<String, Object> resultValues = result.getValues();
			for (Entry<String, Object> values : resultValues.entrySet()) {
				Object value = values.getValue();
				if (isNumeric(value)) {
					String message = createJsonMessage(server, query, typeNames, result, values, value);
                    log.debug("Insert into Elastic: Index: [{}] Type: [{}] Message: [{}]", indexName, TYPE_NAME, message);
					Index index = new Index.Builder(message).index(indexName).type(TYPE_NAME).build();
					jestClient.execute(index);
				} else {
					log.warn("Unable to submit non-numeric value to Elastic: [{}] from result [{}]", value, result);
				}
			}
		}
	}

	private String createJsonMessage(Server server, Query query, List<String> typeNames, Result result, Entry<String, Object> values, Object value) throws IOException {

		String keyString = getKeyString(server, query, result, values, typeNames, this.rootPrefix);

        String alias;
        if (server.getAlias() != null) {
            alias = server.getAlias();
        } else {
            alias = server.getHost() + "_" + server.getPort();
            alias = StringUtils.cleanupStr(alias);
        }

        Closer closer = Closer.create();
		try {
			ByteArrayOutputStream out = closer.register(new ByteArrayOutputStream());
			JsonGenerator generator = closer.register(jsonFactory.createGenerator(out, UTF8));
			generator.writeStartObject();
			generator.writeStringField("server", alias);
			generator.writeStringField("metric", keyString);
			generator.writeNumberField("value", Double.parseDouble(value.toString()));
			generator.writeStringField("resultAlias", result.getKeyAlias());
			generator.writeStringField("attributeName", result.getAttributeName());
			generator.writeStringField("key", values.getKey());
			generator.writeNumberField("timestamp", result.getEpoch());
			generator.writeEndObject();
			generator.close();
			return out.toString("UTF-8");
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	@VisibleForTesting
	void setJestClient(JestClient jestClient) {
		this.jestClient = jestClient;
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
		// no validations
	}

	@Override
    public void start() throws LifecycleException {
        super.start();

		if (jestClient == null) {
			log.info("Create a jest elastic search client for connection url [{}]", connectionUrl);
			JestClientFactory factory = new JestClientFactory();
			factory.setHttpClientConfig(
					new HttpClientConfig.Builder(connectionUrl)
							.multiThreaded(true)
							.build());
			jestClient = factory.getObject();

        }
		else {
			log.info("Note: using injected jestClient instead of creating a new one: [{}]", jestClient);
		}

        createMapping(jestClient, indexName, TYPE_NAME);

    }

    private static void createMapping(JestClient jestClient, String indexName, String typeName) {
        synchronized (CREATE_MAPPING_LOCK) {
            try {
                IndicesExists indicesExists = new IndicesExists.Builder(indexName).build();
                boolean indexExists = jestClient.execute(indicesExists).isSucceeded();

                if (!indexExists) {

                    CreateIndex createIndex = new CreateIndex.Builder(indexName).build();
                    jestClient.execute(createIndex);

                    PutMapping putMapping = new PutMapping.Builder(indexName, typeName,
                            "{\n" +
                                    "  \"jmx-entry\": {\n" +
                                    "    \"properties\": {\n" +
                                    "      \"attributeName\": {\n" +
                                    "        \"type\": \"string\",\n" +
                                    "        \"index\": \"not_analyzed\"\n" +
                                    "      },\n" +
                                    "      \"key\": {\n" +
                                    "        \"type\": \"string\",\n" +
                                    "        \"index\": \"not_analyzed\"\n" +
                                    "      },\n" +
                                    "      \"metric\": {\n" +
                                    "        \"type\": \"string\",\n" +
                                    "        \"index\": \"not_analyzed\"\n" +
                                    "      },\n" +
                                    "      \"server\": {\n" +
                                    "        \"type\": \"string\",\n" +
                                    "        \"index\": \"not_analyzed\"\n" +
                                    "      },\n" +
                                    "      \"timestamp\": {\n" +
                                    "        \"type\": \"date\"\n" +
                                    "      },\n" +
                                    "      \"resultAlias\": {\n" +
                                    "        \"type\": \"string\",\n" +
                                    "        \"index\": \"not_analyzed\"\n" +
                                    "      },\n" +
                                    "      \"value\": {\n" +
                                    "        \"type\": \"float\"\n" +
                                    "      }\n" +
                                    "    }\n" +
                                    "  }\n" +
                                    "}").build();

                    JestResult result = jestClient.execute(putMapping);
                    if (!result.isSucceeded()) {
                        log.warn("Failed to create mapping: {}", result.getErrorMessage());
                    }
                    else {
                        log.info("Created mapping for index {}", indexName);
                    }
                }
            } catch (IOException e) {
                log.warn("Cannot create mapping for elastic search database.", e);
            }
        }
    }

    @Override
	public void stop() throws LifecycleException {
		super.stop();
		jestClient.shutdownClient();
	}
}
