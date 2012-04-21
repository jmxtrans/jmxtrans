#!/bin/bash

FILENAME=$2

# Specify the commonly used configuration options below in a config file.
CONF_FILE=${CONF_FILE:-"jmxtrans.conf"}
if [ -e "$CONF_FILE" ]; then
	. "$CONF_FILE"
fi

LOG_DIR=${LOG_DIR:-"."}
JAR_FILE=${JAR_FILE:-"jmxtrans-all.jar"}
JSON_DIR=${JSON_DIR:-"."}
SECONDS_BETWEEN_RUNS=${SECONDS_BETWEEN_RUNS:-"60"}

JPS=${JPS:-"/usr/bin/jps -l"}
JAVA=${JAVA:-"/usr/bin/java"}
PSCMD="$JPS | grep -i jmxtrans | awk '{ print \$1 };'"
JAVA_OPTS=${JAVA_OPTS:-"-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true"}
NEW_SIZE=${NEW_SIZE:-"64"}
NEW_RATIO=${NEW_RATIO:-"8"}
HEAP_SIZE=${HEAP_SIZE:-"512"}
PERM_SIZE=${PERM_SIZE:-"384"}
MAX_PERM_SIZE=${MAX_PERM_SIZE:-"384"}
CPU_CORES=${CPU_CORES:-"1"}
IO_FRACTION=${IO_FRACTION:-"85"}
JMX_PORT=${JMX_PORT:-"2101"}
LOG_LEVEL=${LOG_LEVEL:-"debug"}
JMXTRANS_OPTS="$JMXTRANS_OPTS -Djmxtrans.log.level=${LOG_LEVEL} -Djmxtrans.log.dir=$LOG_DIR"


MONITOR_OPTS=${MONITOR_OPTS:-"-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=${JMX_PORT}"}
GC_OPTS=${GC_OPTS:-"-Xms${HEAP_SIZE}M -Xmx${HEAP_SIZE}M -XX:+UseConcMarkSweepGC -XX:NewRatio=${NEW_RATIO} -XX:NewSize=${NEW_SIZE}m -XX:MaxNewSize=${NEW_SIZE}m -XX:MaxTenuringThreshold=16 -XX:GCTimeRatio=9 -XX:PermSize=${PERM_SIZE}m -XX:MaxPermSize=${MAX_PERM_SIZE}m -XX:+UseTLAB -XX:CMSInitiatingOccupancyFraction=${IO_FRACTION} -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:ParallelGCThreads=${CPU_CORES} -Dsun.rmi.dgc.server.gcInterval=28800000 -Dsun.rmi.dgc.client.gcInterval=28800000"}

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
	if [ ! -z "$PIDFILE" ]; then
		if [ -r "$PIDFILE" ]; then
			PID=$(cat $PIDFILE)
		fi
	else
		PID=$(eval $PSCMD)
	fi

	if [ ! -z "$PID" ]; then
		echo "jmxtrans appears to already be running @ pid: $PID"
		exit 1
	fi

	if [ -z "$FILENAME" ]; then
		EXEC=${EXEC:-"-jar $JAR_FILE -e -j $JSON_DIR -s $SECONDS_BETWEEN_RUNS"}
	else
		EXEC=${EXEC:-"-jar $JAR_FILE -e -f $FILENAME -s $SECONDS_BETWEEN_RUNS"}
	fi

	$JAVA -server $JAVA_OPTS $JMXTRANS_OPTS $GC_OPTS $MONITOR_OPTS $EXEC 2>&1 &

	if [ ! -z "$PIDFILE" ]; then
		echo $! > "$PIDFILE"
	fi

}

stop() {
	if [ ! -z "$PIDFILE" ]; then
		if [ -r "$PIDFILE" ]; then
			PID=$(cat $PIDFILE)
		fi
	else
		PID=$(eval $PSCMD)
	fi
	if [ ! -z "$PID" ]; then
		kill -15 "$PID"
		echo -n "Stopping jmxtrans"
		while (true); do
	          ps -p $PID >/dev/null 2>&1
	          if [ $? -eq 0 ] ; then
				echo -n "."
				sleep 1
			else
				echo ""
	        	echo "jmxtrans stopped"
				if [ ! -z "$PIDFILE" ]; then
					rm -f $PIDFILE
				fi

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
	if [ ! -z "$PIDFILE" ]; then
		if [ -r "$PIDFILE" ]; then
			PID=$(cat $PIDFILE)
		fi
	else
		PID=$(eval $PSCMD)
	fi
	if [ ! -z "$PID" ]; then
		echo "jmxtrans appears to be running at pid: $PID"
		exit 0
	else
		echo "jmxtrans is not running."
		exit 3
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
