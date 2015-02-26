#!/bin/sh

wget http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
rpm -Uvh epel-release-6*.rpm

wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo

yum -y install apache-maven rpm-build
alternatives --set java /usr/lib/jvm/jre-1.7.0-openjdk.x86_64/bin/java

cd /jmxtrans_src
mvn clean install -Prpm
