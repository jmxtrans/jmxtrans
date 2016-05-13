```yaml2jmxtrans_new.py``` is for new config for jmxtrans<br>
with the help of ```yaml2jmxtrans_new.py```, ```kafka_test.yml``` convert to ```kafka_metrics.json``` .<br>
the yaml config file include four segments below:<br>
* global config, can ignore<br>
* outputWriters: output writer list,  include KeyOutWriter, OpenFalconWriterFactory and so on.
* queries: kafka metric for querying, detail metrics can be search in **"jconsole"**
* sets:  combine queries and monitor kakfa jmx address, like **"localhost:9998"**, which must be set ```JMX_PORT=9998``` when kafka startup.

