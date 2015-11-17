#!/bin/bash
#
# The MIT License
# Copyright (c) 2010 JmxTrans team
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#


FILENAME=$2

# If CONF_FILE not defined but /etc/sysconfig/jmxtrans found, use it (servicectl/initd)
if [ -z "$CONF_FILE" ]; then
    if [ -f /etc/sysconfig/jmxtrans ]; then
       CONF_FILE=/etc/sysconfig/jmxtrans
    fi
fi

# Specify the commonly used configuration options below in a config file.
CONF_FILE=${CONF_FILE:-"jmxtrans.conf"}
if [ -e "$CONF_FILE" ]; then
    . "$CONF_FILE"
fi

JAVA_HOME=${JAVA_HOME:-"/usr"}
LOG_DIR=${LOG_DIR:-"."}
LOG_FILE=${LOG_FILE:-"/dev/null"}

JAR_FILE=${JAR_FILE:-"jmxtrans-all.jar"}
ADDITIONAL_JARS=${ADDITIONAL_JARS:-""}
JSON_DIR=${JSON_DIR:-"."}
SECONDS_BETWEEN_RUNS=${SECONDS_BETWEEN_RUNS:-"60"}
HARDKILL_THRESHOLD=${HARDKILL_THRESHOLD:-60}

JPS=${JPS:-"${JAVA_HOME}/bin/jps -l"}
USE_JPS=${USE_JPS:-"true"}
JAVA=${JAVA:-"${JAVA_HOME}/bin/java"}
CHECK_JAVA=${CHECK_JAVA:-"true"}
PSJAVA=${PSJAVA:-"ps aux | grep [j]ava"} 
PSCMD="$JPS | grep -i jmxtrans | awk '{ print \$1 };'"
JAVA_OPTS=${JAVA_OPTS:-"-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true"}
HEAP_SIZE=${HEAP_SIZE:-"512"}
PERM_SIZE=${PERM_SIZE:-"384"}
MAX_PERM_SIZE=${MAX_PERM_SIZE:-"384"}
JMX_PORT=${JMX_PORT:-"2101"}
LOG_LEVEL=${LOG_LEVEL:-"debug"}
CONTINUE_ON_ERROR=${CONTINUE_ON_ERROR:-"false"}
JMXTRANS_OPTS="$JMXTRANS_OPTS -Djmxtrans.log.level=${LOG_LEVEL} -Djmxtrans.log.dir=$LOG_DIR"


MONITOR_OPTS=${MONITOR_OPTS:-"-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=${JMX_PORT}"}
GC_OPTS=${GC_OPTS:-"-Xms${HEAP_SIZE}m -Xmx${HEAP_SIZE}m -XX:PermSize=${PERM_SIZE}m -XX:MaxPermSize=${MAX_PERM_SIZE}m"}

if [ "${ADDITIONAL_JARS}" == "" ]; then
  ADDITIONAL_JARS_OPTS=""
else
  ADDITIONAL_JARS_OPTS="-a ${ADDITIONAL_JARS}"
fi

if [ "$USE_JPS" == "true" ]; then
  JPS_RUNNABLE=`$JPS 2>&1`
  if [ $? != 0 ]; then
    echo "Cannot execute $JPS!, switching to stock ps"
    PSCMD="$PSJAVA | grep -i jmxtrans | awk '{ print \$2 };'"
  fi
else
  PSCMD="$PSJAVA | grep -i jmxtrans | awk '{ print \$2 };'"
fi

if [ "$CHECK_JAVA" == "true" ]; then
    JAVA_VERSION=`$JAVA -version 2>&1`
    if [ $? != 0 ]; then
        echo "Cannot execute $JAVA!"
        exit 1
    fi
fi

if [ ! -f $JAR_FILE ]; then
  echo "File not found - $JAR_FILE"
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
        EXEC=${EXEC:-"-jar $JAR_FILE -e -j $JSON_DIR -s $SECONDS_BETWEEN_RUNS -c $CONTINUE_ON_ERROR $ADDITIONAL_JARS_OPTS"}
    else
        EXEC=${EXEC:-"-jar $JAR_FILE -e -f $FILENAME -s $SECONDS_BETWEEN_RUNS -c $CONTINUE_ON_ERROR $ADDITIONAL_JARS_OPTS"}
    fi

    nohup $JAVA -server $JAVA_OPTS $JMXTRANS_OPTS $GC_OPTS $MONITOR_OPTS $EXEC >>$LOG_FILE 2>&1 &

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
        cnt=0
        echo -n "Stopping jmxtrans"
        while (true); do
            ps -p $PID >/dev/null 2>&1
            if [ $? -eq 0 ] ; then
                cnt=$((cnt+=1))

                if [ $cnt -lt $HARDKILL_THRESHOLD ]; then
                  echo -n "."
                  sleep 1
                else
                  echo "Reached HARDKILL_THRESHOLD(=${HARDKILL_THRESHOLD}). Sending SIGKILL to process ${PID}"
                  kill -9 "$PID"
                fi
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
