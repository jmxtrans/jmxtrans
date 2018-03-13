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

FROM openjdk:8-alpine

MAINTAINER Nicolas Muller <n.muller@treeptik.fr>

RUN apk update \
   && apk add curl \
   && apk add gnupg \
   && apk add tini \
   && apk add bash

## grab gosu for easy step-down from root
ENV GOSU_VERSION 1.7

RUN curl -L -o /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-amd64" \
    && curl -L -o /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-amd64.asc" \
    && export GNUPGHOME="$(mktemp -d)" \
    && gpg --keyserver ha.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
    && gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
    && rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
    && chmod +x /usr/local/bin/gosu \
    && gosu nobody true


ENV JMXTRANS_HOME /usr/share/jmxtrans
ENV PATH $JMXTRANS_HOME/bin:$PATH
ENV JAR_FILE $JMXTRANS_HOME/lib/jmxtrans-all.jar
ENV HEAP_SIZE 512
ENV PERM_SIZE 384
ENV MAX_PERM_SIZE 384
ENV SECONDS_BETWEEN_RUNS 60
ENV CONTINUE_ON_ERROR false
ENV JSON_DIR /var/lib/jmxtrans

# Install jmxtrans
RUN addgroup jmxtrans \
   && adduser jmxtrans -s /bin/bash -h /usr/share/jmxtrans -S -D -G jmxtrans

WORKDIR ${JMXTRANS_HOME}
RUN mkdir -p ${JMXTRANS_HOME}/conf

COPY logback.xml ${JMXTRANS_HOME}/conf/logback.xml

RUN mkdir -p /usr/share/jmxtrans/lib/ \
    && JMXTRANS_VERSION=`curl http://central.maven.org/maven2/org/jmxtrans/jmxtrans/maven-metadata.xml | sed -n 's:.*<release>\(.*\)</release>.*:\1:p'` \
    && mkdir -p /var/log/jmxtrans \
    && wget -q http://central.maven.org/maven2/org/jmxtrans/jmxtrans/${JMXTRANS_VERSION}/jmxtrans-${JMXTRANS_VERSION}-all.jar \
    && mv jmxtrans-${JMXTRANS_VERSION}-all.jar ${JAR_FILE}

COPY docker-entrypoint.sh /

VOLUME ${JSON_DIR}

# JMX PORT
EXPOSE 9999

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["start-with-jmx"]
