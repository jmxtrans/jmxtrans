Here is a commented version the config.json file

{
   "servers" : [ {
      "host" : "9999", # JMX port to connect to to gather metrics
      "port" : "localhost", # host to connect to to gather metrics
      "queries" : [ {
         "outputWriters" : [ {
            "@class" : "com.googlecode.jmxtrans.model.output.kafka.KafkaWriter",
            "typeNames" : [ "name" ],
            "booleanAsNumber" : false,
            "rootPrefix" : "myPrefix", 
            "debug" : true,
            "topics" : "topic", # this currently gets ignored
            "tags" : {
            } ,
            "settings" : {
               "zk.connect" : "zookeeperHost:2181",
               "metadata.broker.list" : "kafkaBrokerHost:9092",
               "serializer.class" : "kafka.serializer.StringEncoder",
               "topics" : "myTopic" # topic to write to
            }
         } ],
         "obj" : "java.lang:type=Memory,*",
         "attr" : [ "HeapMemoryUsage" ] # 'attr' can be left off to gather all attributes
      } ]
   } ]
}

The above file with generate kafka messages written to the kafka topic
"myTopic". The HeapMemoryUsage metric has 4 parts to it (init,
commited, max, and used) and so 4 messages will be written to the
topic that look like:

{"keyspace":"myPrefix.localhost_9999.sun_management_MemoryImpl.HeapMemoryUsage_init","value":"536870912","timestamp":1462947750,"tags":{}}
{"keyspace":"myPrefix.localhost_9999.sun_management_MemoryImpl.HeapMemoryUsage_committed","value":"536870912","timestamp":1462947750,"tags":{}}
{"keyspace":"myPrefix.localhost_9999.sun_management_MemoryImpl.HeapMemoryUsage_max","value":"536870912","timestamp":1462947750,"tags":{}}
{"keyspace":"myPrefix.localhost_9999.sun_management_MemoryImpl.HeapMemoryUsage_used","value":"22020096","timestamp":1462947750,"tags":{}}

FYI, the keyspace is a period-delimited string that consists of the
following parts:

1. rootPrefix
2. hostname_port
3. classname of the MBean
4. value of the field specified in "typeNames"
