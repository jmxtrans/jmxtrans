#!/bin/bash

FILENAME=$2

# Specify the commonly used configuration options below in a config file.
CONF_FILE=${CONF_FILE:-"jmxtrans.conf"}
if [ -e "$CONF_FILE" ]; then
	. "$CONF_FILE"
fi

JPS=${JPS:-"/usr/bin/jps"}
JAVA=${JAVA:-"/usr/bin/java"}
PSCMD="$JPS | grep -i jmxtrans | awk '{ print \$1 };'"
JAVA_OPTS=${JAVA_OPTS:-"-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true"}
NEW_SIZE=${NEW_SIZE:-"64M"}
NEW_RATIO=${NEW_RATIO:-"8"}
HEAP_SIZE=${HEAP_SIZE:-"512M"}
CPU_CORES=${CPU_CORES:-"1"}
MONITOR_OPTS=${MONITOR_OPTS:-"-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=2101"}
GC_OPTS=${GC_OPTS:-"-Xms${HEAP_SIZE}M -Xmx${HEAP_SIZE}M -XX:+UseConcMarkSweepGC -XX:NewRatio=${NEW_RATIO} -XX:NewSize=${NEW_SIZE}m -XX:MaxNewSize=${NEW_SIZE}m -XX:MaxTenuringThreshold=16 -XX:GCTimeRatio=9 -XX:PermSize=384m -XX:MaxPermSize=384m -XX:+UseTLAB -XX:CMSInitiatingOccupancyFraction=85 -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:ParallelGCThreads=$CPU_CORES -Dsun.rmi.dgc.server.gcInterval=28800000 -Dsun.rmi.dgc.client.gcInterval=28800000"}

JAR_FILE=${JAR_FILE:-"jmxtrans-all.jar"}
JSON_DIR=${JSON_DIR:-"."}
SECONDS_BETWEEN_RUNS=${SECONDS_BETWEEN_RUNS:-"60"}

JPS_RUNNABLE=`$JPS 2>&1`
if [ $? != 0 ]; then
    echo "Cannot execute $JPS!"
    exit 1
fi

JAVA_VERSION=`$JAVA -version 2>&1`
if [ $? != 0 ]; then
    echo "Cannot execute $JAVA!"
    exit 1
fi

start() {
	PID=$(eval $PSCMD)
	if [ ! -z "$PID" ]; then
		echo "jmxtrans appears to already be running @ pid: $PID"
		exit 1
	fi

	if [ -z "$FILENAME" ]; then
		EXEC=${EXEC:-"-jar $JAR_FILE -e -j $JSON_DIR -s $SECONDS_BETWEEN_RUNS"}
	else
		EXEC=${EXEC:-"-jar $JAR_FILE -e -f $FILENAME -s $SECONDS_BETWEEN_RUNS"}
	fi

	$JAVA -server $JAVA_OPTS $GC_OPTS $MONITOR_OPTS $EXEC 2>&1 &
}

stop() {
	PID=$(eval $PSCMD)
	if [ ! -z "$PID" ]; then
		kill -1 "$PID"
		echo -n "Stopping jmxtrans"
		while (true); do
			PID=$(eval $PSCMD)
			if [ ! -z "$PID" ]; then
				echo -n "."
				sleep 1
			else
				echo ""
				break
			fi
		done
	else
		echo "jmxtrans was not running"
	fi
}

restart() {
	stop
	start
}

status() {
	PID=$(eval $PSCMD)
	if [ ! -z "$PID" ]; then
		echo "jmxtrans appears to be running at pid: $PID"
	else
		echo "jmxtrans is not running."
	fi
}

case $1 in
	start)
		start
	;;
	stop)
		stop
	;;
	restart)
		restart
	;;
	status)
		status
	;;
	*)
		echo $"Usage: $0 {start|stop|restart|status} [filename.json]"
	;;
esac

exit 0
