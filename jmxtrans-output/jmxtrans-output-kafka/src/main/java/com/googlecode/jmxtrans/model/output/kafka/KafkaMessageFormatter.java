package com.googlecode.jmxtrans.model.output.kafka;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.apache.kafka.clients.producer.KafkaProducer;

/**
 * Created by elludo, seuf on 25/07/2016.
 */
public interface KafkaMessageFormatter {


    void write(Server server, Query query, ImmutableList<Result> results) throws Exception;

    void setProducer(KafkaProducer<String, String> producer);
}
