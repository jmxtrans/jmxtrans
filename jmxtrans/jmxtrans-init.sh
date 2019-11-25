#!/bin/bash
#
# The MIT License
# Copyright © 2010 JmxTrans team
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

usage() {
	echo $"Usage: jmxtrans-init.sh {start|run|stop|restart|status} [filename.json|jsondir]"
}

if [ $# -eq 0 ]; then
	usage
	exit 1
fi

# First argument is command
CMD_ARG=${1}
shift

# Second optional argument is json config
if [ $# -eq 0 ]; then
	JSON_ARG="${JSON_DIR}";
else
	JSON_ARG=${1}
fi

# If CONF_FILE not defined but /etc/sysconfig/jmxtrans found, use it (servicectl/initd)
if [ -z "${CONF_FILE}" ]; then
    if [ -f /etc/sysconfig/jmxtrans ]; then
       CONF_FILE=/etc/sysconfig/jmxtrans
    fi
fi

# Specify the commonly used configuration options below in a config file.
CONF_FILE=${CONF_FILE:-"jmxtrans.conf"}
if [ -e "${CONF_FILE}" ]; then
  source "${CONF_FILE}"
fi

JAVA_HOME=${JAVA_HOME:-"/usr"}
STDOUT_FILE=${STDOUT_FILE:-"/dev/null"}

HARDKILL_THRESHOLD=${HARDKILL_THRESHOLD:-60}

JPS=${JPS:-"${JAVA_HOME}/bin/jps -l"}
USE_JPS=${USE_JPS:-"true"}
PSJAVA=${PSJAVA:-"ps aux | grep [j]ava"}
PSCMD="${JPS} | grep -i jmxtrans | awk '{ print \$1 };'"

if [ "${USE_JPS}" == "true" ]; then
  JPS_RUNNABLE=`${JPS} 2>&1`
  if [ $? != 0 ]; then
    echo "Cannot execute ${JPS}!, switching to stock ps"
    PSCMD="${PSJAVA} | grep -i jmxtrans | awk '{ print \$2 };'"
  fi
else
  PSCMD="${PSJAVA} | grep -i jmxtrans | awk '{ print \$2 };'"
fi
JMXTRANS_RUN="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )/jmxtrans.sh"

start() {
    if [ ! -z "${PIDFILE}" ]; then
        if [ -r "${PIDFILE}" ]; then
            PID=$(cat ${PIDFILE})
        fi
    else
        PID=$(eval ${PSCMD})
    fi

    if [ ! -z "${PID}" ]; then
        echo "jmxtrans appears to already be running @ pid: ${PID}"
        exit 1
    fi


    nohup ${JMXTRANS_RUN} ${JSON_ARG}>>${STDOUT_FILE} 2>&1 &

    if [ ! -z "${PIDFILE}" ]; then
        echo $! > "${PIDFILE}"
    fi

}

run() {
	${JMXTRANS_RUN} ${JSON_ARG}
}

stop() {
    if [ ! -z "${PIDFILE}" ]; then
        if [ -r "${PIDFILE}" ]; then
            PID=$(cat ${PIDFILE})
        fi
    else
        PID=$(eval ${PSCMD})
    fi
    if [ ! -z "${PID}" ]; then
        kill -15 "${PID}"
        cnt=0
        echo -n "Stopping jmxtrans"
        while (true); do
            ps -p ${PID} >/dev/null 2>&1
            if [ $? -eq 0 ] ; then
                cnt=$((cnt+=1))

                if [ ${cnt} -lt ${HARDKILL_THRESHOLD} ]; then
                  echo -n "."
                  sleep 1
                else
                  echo "Reached HARDKILL_THRESHOLD(=${HARDKILL_THRESHOLD}). Sending SIGKILL to process ${PID}"
                  kill -9 "${PID}"
                fi
            else
                echo ""
                echo "jmxtrans stopped"
                if [ ! -z "${PIDFILE}" ]; then
                    rm -f ${PIDFILE}
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
    if [ ! -z "${PIDFILE}" ]; then
        if [ -r "${PIDFILE}" ]; then
            PID=$(cat ${PIDFILE})
        fi
    else
        PID=$(eval ${PSCMD})
    fi
    if [ ! -z "${PID}" ]; then
        echo "jmxtrans appears to be running at pid: ${PID}"
        exit 0
    else
        echo "jmxtrans is not running."
        exit 3
    fi
}

case ${CMD_ARG} in
    run)
        run
    ;;
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
    	usage
    	exit 1
    ;;
esac

exit 0
