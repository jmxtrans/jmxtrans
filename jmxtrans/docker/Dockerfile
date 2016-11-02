FROM java:8u102-jdk

MAINTAINER Nicolas Muller <n.muller@treeptik.fr>

RUN apt-get update && apt-get install -y \
		ca-certificates \
		vim \
		wget \
	--no-install-recommends && rm -rf /var/lib/apt/lists/*

# grab gosu for easy step-down from root
ENV GOSU_VERSION 1.7
RUN set -x \
	&& wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture)" \
	&& wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture).asc" \
	&& export GNUPGHOME="$(mktemp -d)" \
	&& gpg --keyserver ha.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
	&& gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
	&& rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
	&& chmod +x /usr/local/bin/gosu \
	&& gosu nobody true

# grab tini for signal processing and zombie killing
ENV TINI_VERSION v0.9.0
RUN set -x \
	&& wget -O /usr/local/bin/tini "https://github.com/krallin/tini/releases/download/$TINI_VERSION/tini" \
	&& wget -O /usr/local/bin/tini.asc "https://github.com/krallin/tini/releases/download/$TINI_VERSION/tini.asc" \
	&& export GNUPGHOME="$(mktemp -d)" \
	&& gpg --keyserver ha.pool.sks-keyservers.net --recv-keys 6380DC428747F6C393FEACA59A84159D7001A4E5 \
	&& gpg --batch --verify /usr/local/bin/tini.asc /usr/local/bin/tini \
	&& rm -r "$GNUPGHOME" /usr/local/bin/tini.asc \
	&& chmod +x /usr/local/bin/tini \
	&& tini -h

ARG JMXTRANS_VERSION
ENV JMXTRANS_VERSION ${JMXTRANS_VERSION:-259}

ENV JMXTRANS_HOME /usr/share/jmxtrans
ENV PATH $JMXTRANS_HOME/bin:$PATH
ENV JAR_FILE $JMXTRANS_HOME/lib/jmxtrans-all.jar
ENV LOG_LEVEL debug
ENV HEAP_SIZE 512
ENV PERM_SIZE 384
ENV MAX_PERM_SIZE 384
ENV SECONDS_BETWEEN_RUNS 60
ENV CONTINUE_ON_ERROR false
ENV LOG_DIR /var/log/jmxtrans
ENV JSON_DIR /var/lib/jmxtrans
ENV GC_OPTS=${GC_OPTS:-"-Xms${HEAP_SIZE}m -Xmx${HEAP_SIZE}m"}
ENV JMXTRANS_OPTS="$JMXTRANS_OPTS -Djmxtrans.log.level=${LOG_LEVEL} -Djmxtrans.log.dir=${LOG_DIR}"
ENV MONITOR_OPTS=" -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=2101  -Dcom.sun.management.jmxremote.rmi.port=2101 "

# Install jmxtrans
RUN echo "debconf debconf/frontend select noninteractive" | debconf-set-selections
RUN wget http://central.maven.org/maven2/org/jmxtrans/jmxtrans/${JMXTRANS_VERSION}/jmxtrans-${JMXTRANS_VERSION}.deb
RUN dpkg -i jmxtrans-${JMXTRANS_VERSION}.deb

COPY docker-entrypoint.sh /

VOLUME ${JSON_DIR}
VOLUME ${LOG_DIR}

# JMX PORT
EXPOSE 2101

RUN ln -sf /dev/stdout ${LOG_DIR}/jmxtrans.log
RUN ln -sf /dev/stderr ${LOG_DIR}/jmxtrans.log

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["start"]
