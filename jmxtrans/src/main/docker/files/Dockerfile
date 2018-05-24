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

FROM openjdk:8-jre-alpine

# Install required packages
RUN apk add --no-cache \
    bash \
    su-exec

ENV JMXTRANS_HOME_DIR=/jmxtrans \
	JMXTRANS_USER=jmxtrans \
    JMXTRANS_CONF_DIR=/jmxtrans/conf \
    JMXTRANS_LOG_DIR=/jmxtrans/log

COPY maven $JMXTRANS_HOME_DIR

RUN set -ex; \
    mv $JMXTRANS_HOME_DIR/docker-entrypoint.sh / ; \
    adduser -D "$JMXTRANS_USER"; \
    mkdir -p "$JMXTRANS_LOG_DIR"; \
    chown -R "$JMXTRANS_USER:$JMXTRANS_USER" "$JMXTRANS_HOME_DIR"

WORKDIR $JMXTRANS_HOME_DIR
VOLUME ["$JMXTRANS_CONF_DIR", "$JMXTRANS_LOG_DIR"]


ENV PATH=$PATH:$JMXTRANS_HOME_DIR/bin

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["jmxtrans.sh", "run"]
