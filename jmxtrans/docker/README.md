# JmxTrans Docker image

<img src="http://www.jmxtrans.org/assets/img/jmxtrans-logo.gif"/>

Connecting the outside world to the JVM.

This is a fully functional JmxTrans application instance, based on the last releaase.
[http://www.jmxtrans.org/).


# Usage

```
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans -P jmxtrans/jmxtrans 		 
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans -P jmxtrans/jmxtrans start-without-jmx
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans -p 9999:9999 jmxtrans/jmxtrans
```

You have two commands available :
* start-with-jmx (default value)
* start-without-jmx without jmx if you think there is an extra runtime cost (we don't think so)

Example: ```docker run -d -v `pwd`/json-files:/var/lib/jmxtrans -P jmxtrans/jmxtrans ```

This will automatically create a 'json-files' volume on docker host, that will survive container stop/restart/deletion. 

# Passing JMXTRANS launcher parameters

The arguments as environment variable are :
* SECONDS_BETWEEN_RUNS
* HEAP_SIZE
* PERM_SIZE
* MAX_PERM_SIZE
* CONTINUE_ON_ERROR
* JMXTRANS_OPTS
* JAVA_OPTS

You might need to customize the JVM running JMXTRANS, typically to pass system properties or tweak heap memory settings. 
Use JAVA_OPTS environment variable for this purpose :

```
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans   
       --env JAVA_OPTS="-Dkey1=value1 -Dkey2=value2" 
       jmxtrans/jmxtrans
```
 
or change the timing :

```
docker run -d   -v `pwd`/json-files:/var/lib/jmxtrans 
                --env SECONDS_BETWEEN_RUNS=5
                --env HEAP_SIZE=1024
                --env PROXY_HOST=192.168.50.4
                jmxtrans/jmxtrans 
```

If you log into the container and exec `ps -ef | grep java`, you will see :
```
jmxtrans     9     1 25 13:09 ?        00:00:01 java -server 
-Dlogback.configurationFile=file:////usr/share/jmxtrans/conf/logback.xml 
-Xms1024m -Xmx1024m -XX:PermSize=384m -XX:MaxPermSize=384m -Dcom.sun.management.jmxremote 
-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false 
-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.rmi.port=9999 
-Djava.rmi.server.hostname=192.168.50.4 -jar /usr/share/jmxtrans/lib/jmxtrans-all.jar 
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
docker build -t jmxtrans/jmxtrans:260 --build-arg JMXTRANS_VERSION=260 .
```

# Monitor JMXTrans with JMX (aka Inception) 

Make sure to publish JMX container’s port 9999 as the Docker host port 9999 when starting the Docker container.
**PROXY_HOST** is the IP where is present the JMX client (example jvisualvm). 
It is a mandatory parameter for jmx because you need a remote access to jvm instance.
 
Example with docker engine into a vagrantbox (static ip as 192.168.50.4)
```
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans -p 9999:9999 --env PROXY_HOST=192.168.50.4 jmxtrans/jmxtrans
```

Example with native docker MacOSX engine. 
It is tricky despite the hidden virtualmachine (xhive) into Mac, we need to use *localhost* and not 172.16.123.1
Docker for Mac is awesome but maybe hard to understand...
```
docker run -d -v `pwd`/json-files:/var/lib/jmxtrans -p 9999:9999 --env PROXY_HOST=localhost jmxtrans/jmxtrans
```


# Questions ?

Jump on https://groups.google.com/forum/#!forum/jmxtrans and ask!
