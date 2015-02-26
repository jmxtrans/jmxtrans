#!/bin/sh

apt-get install -y maven openjdk-7-jdk

cd /jmxtrans_src
mvn clean install -Pdeb
