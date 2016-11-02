#!/usr/bin/env bash

set -e

chown -R jmxtrans "$JMXTRANS_HOME"
EXEC="-jar $JAR_FILE -e -j $JSON_DIR -s $SECONDS_BETWEEN_RUNS -c $CONTINUE_ON_ERROR $ADDITIONAL_JARS_OPTS"

if [ "$1" = 'start' ]; then
	set -- gosu jmxtrans tini -- java -server $JAVA_OPTS $JMXTRANS_OPTS $GC_OPTS $EXEC
elif [ "$1" = 'jmx' ]; then
    set -- gosu jmxtrans tini -- java -server $JAVA_OPTS $JMXTRANS_OPTS $GC_OPTS $MONITOR_OPTS $EXEC
fi

exec "$@"