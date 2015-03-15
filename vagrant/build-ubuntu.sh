#!/bin/sh

apt-get update
apt-get dist-upgrade -y
apt-get install -y maven openjdk-7-jdk

cd /jmxtrans_src
mvn clean install -Pdeb
