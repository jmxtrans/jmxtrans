# JmxTrans Docker image

<img src="http://www.jmxtrans.org/assets/img/jmxtrans-logo.gif"/>

Connecting the outside world to the JVM.

This is a fully functional JmxTrans application instance, based on the last releaase.
[http://www.jmxtrans.org/).


# Usage

```
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans jmxtrans/jmxtrans 
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans jmxtrans/jmxtrans start 
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans -P jmxtrans/jmxtrans jmx
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans -p 9999:2101 jmxtrans/jmxtrans jmx
```

You have two commands available :
* start (default value)  
* jmx to start the jvm with jmx debug mode

This will automatically create a 'json-files' volume on docker host, that will survive container stop/restart/deletion. 

# Passing JMXTRANS launcher parameters

The arguments as environment variable are :
* LOG_DIR
* SECONDS_BETWEEN_RUNS
* HEAP_SIZE
* CONTINUE_ON_ERROR
* JMXTRANS_OPTS
* JAVA_OPTS

You might need to customize the JVM running JMXTRANS, typically to pass system properties or tweak heap memory settings. 
Use JAVA_OPTS environment variable for this purpose :

```
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans \  
       --env JAVA_OPTS="-Dkey1=value1 -Dkey2=value2" \
       jmxtrans/jmxtrans:latest 
```
 
or change the timing :

```
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans --env SECONDS_BETWEEN_RUNS=5 jmxtrans/jmxtrans 
```

If you log into the container and exec `ps -ef | grep java`, you will see :
```
jmxtrans    12     1  7 09:15 ?        00:00:01 java -server -Djmxtrans.log.level=debug 
-Djmxtrans.log.dir=/var/log/jmxtrans -Xms512m -Xmx512m -jar /usr/share/jmxtrans/lib/jmxtrans-all.jar 
-e -j /var/lib/jmxtrans -s 5 -c false
```

# Building

Build with the usual : 

```
docker build -t jmxtrans/jmxtrans .
```

Or with more arguments for a special release :

```
docker build -t jmxtrans/jmxtrans:256 --build-arg JMXTRANS_VERSION=256 .
docker build -t jmxtrans/jmxtrans:259 --build-arg JMXTRANS_VERSION=259 .
```

# Questions ?

Jump on https://groups.google.com/forum/#!forum/jmxtrans and ask!