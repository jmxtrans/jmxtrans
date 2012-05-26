#!/bin/bash

aptitude update
aptitude safe-upgrade
apt-get -y install devscripts debhelper build-essential git ant openjdk-6-jdk openssh-server