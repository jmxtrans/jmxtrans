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

LOGFILE=/var/log/jmxtrans/jmxtrans.log

while [ ! -f "${LOGFILE}" ]; do
  echo "$(date) - Waiting for log file..."
  sleep 1
done

echo "$(date) 0 - Found log file, looking for log message..."
attempt=1
until grep -q "value=Java Virtual Machine Specification" ${LOGFILE}; do
  if (( attempt < 60 )); then
  	echo "$(date) ${attempt} - Waiting for log message."
  else
    # After one minute, print the log contents because it smells bad
    echo "$(date) ${attempt} - Waiting for log message. Log contents:"
    cat ${LOGFILE}
    echo "----"
  fi
  sleep 1s
  attempt=$((attempt+1))
done
found_log=$(grep "value=Java Virtual Machine Specification" ${LOGFILE})
echo "$(date) ${attempt} - Found log message ${found_log}"
echo "${verification.message}"
