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

if (( $# != 1 )); then
  echo "Usage: jmxtrans.sh <json file or dir>"
  exit 1
fi

if [[ -f "${1}" ]]; then
  JSON_CONFIG_OPT="-f ${1}"
elif [[ -d "${1}" ]]; then
  JSON_CONFIG_OPT="-j ${1}"
else
  echo "${1} is not a file nor a directory"
  exit 1
fi

JAVA_HOME=${JAVA_HOME:-"/usr"}
LOG_DIR=${LOG_DIR:-"."}

if [[ "${JAR_FILE}" == "" ]]; then
  JAR_FILE="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../lib" >/dev/null && pwd )/jmxtrans-all.jar"
fi
ADDITIONAL_JARS=${ADDITIONAL_JARS:-""}
SECONDS_BETWEEN_RUNS=${SECONDS_BETWEEN_RUNS:-"60"}

JAVA=${JAVA:-"${JAVA_HOME}/bin/java"}
CHECK_JAVA=${CHECK_JAVA:-"true"}
JAVA_OPTS=${JAVA_OPTS:-"-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true"}
HEAP_SIZE=${HEAP_SIZE:-"512"}
PERM_SIZE=${PERM_SIZE:-"384"}
MAX_PERM_SIZE=${MAX_PERM_SIZE:-"384"}
JMX_PORT=${JMX_PORT:-"2101"}
LOG_LEVEL=${LOG_LEVEL:-"info"}
CONTINUE_ON_ERROR=${CONTINUE_ON_ERROR:-"false"}
JMXTRANS_OPTS="${JMXTRANS_OPTS} -Djmxtrans.log.level=${LOG_LEVEL} -Djmxtrans.log.dir=${LOG_DIR}"


MONITOR_OPTS=${MONITOR_OPTS:-"-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=${JMX_PORT}"}
GC_OPTS=${GC_OPTS:-"-Xms${HEAP_SIZE}m -Xmx${HEAP_SIZE}m -XX:PermSize=${PERM_SIZE}m -XX:MaxPermSize=${MAX_PERM_SIZE}m"}
SSL_OPTS=${SSL_OPTS:-""}
if [[ "${SSL_TRUSTSTORE}" != "" ]]; then
    SSL_OPTS="${SSL_OPTS} -Djavax.net.ssl.trustStore=${SSL_TRUSTSTORE}"
    if [[ "${SSL_TRUSTSTORE_PASSWORD}" != "" ]]; then
        SSL_OPTS="${SSL_OPTS} -Djavax.net.ssl.trustStorePassword=${SSL_TRUSTSTORE_PASSWORD}"
    fi
fi

if [[ "${ADDITIONAL_JARS}" == "" ]]; then
  ADDITIONAL_JARS_OPTS=""
else
  ADDITIONAL_JARS_OPTS="-a ${ADDITIONAL_JARS}"
fi

if [[ "${CHECK_JAVA}" == "true" ]]; then
    JAVA_VERSION=`${JAVA} -version 2>&1`
    if [[ $? != 0 ]]; then
        echo "Cannot execute ${JAVA}!"
        exit 1
    fi
fi

if [[ ! -f ${JAR_FILE} ]]; then
  echo "File not found - ${JAR_FILE}"
  exit 1
fi

EXEC=${EXEC:-"-jar ${JAR_FILE} -e ${JSON_CONFIG_OPT} -s ${SECONDS_BETWEEN_RUNS} -c ${CONTINUE_ON_ERROR} ${ADDITIONAL_JARS_OPTS} ${ADDITIONAL_OPTS}"}

${JAVA} -server ${JAVA_OPTS} ${JMXTRANS_OPTS} ${GC_OPTS} ${MONITOR_OPTS} ${SSL_OPTS} ${EXEC}
