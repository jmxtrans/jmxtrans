![jmxtranslogo](http://www.jmxtrans.org/assets/img/jmxtrans-logo.gif)

[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fjmxtrans%2Fjmxtrans.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2Fjmxtrans%2Fjmxtrans?ref=badge_shield)
[![Build Status](https://secure.travis-ci.org/jmxtrans/jmxtrans.png?branch=master)](http://travis-ci.org/jmxtrans/jmxtrans)
[![Build status](https://ci.appveyor.com/api/projects/status/7g88sgeglsm7st17?svg=true)](https://ci.appveyor.com/project/gquintana/jmxtrans)
[![Dependency Status](https://www.versioneye.com/user/projects/5421de9e3a8c2f2b8b000056/badge.svg?style=flat)](https://www.versioneye.com/user/projects/5421de9e3a8c2f2b8b000056)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jmxtrans/jmxtrans?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven site](https://img.shields.io/badge/Maven-site-blue.svg)](http://www.jmxtrans.org/jmxtrans/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jmxtrans/jmxtrans/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jmxtrans/jmxtrans)
[![sonarcloud.io](https://img.shields.io/badge/sonarcloud-quality-lightgrey.svg)](https://sonarcloud.io/dashboard?id=org.jmxtrans%3Ajmxtrans-parent)

This is the source code repository for the jmxtrans project.

This is effectively the missing connector between speaking to a JVM via JMX on one end and whatever logging / monitoring / graphing package that you can dream up on the other end.

jmxtrans is very powerful tool which uses easily generated JSON (or [YAML](https://github.com/jmxtrans/jmxtrans/blob/master/jmxtrans/tools/yaml2jmxtrans.py)) based configuration files and then outputs the data in whatever format you desire. It does this with a very efficient engine design that will scale to communicating with thousands of machines from a single jmxtrans instance.

The core engine is very solid and there are writers for [Graphite](http://graphite.wikidot.com/), [StatsD](https://github.com/etsy/statsd), [Ganglia](http://ganglia.sourceforge.net/), [cacti/rrdtool](http://www.cacti.net/), [OpenTSDB](http://opentsdb.net/), text files, and stdout. Feel free to suggest more on the discussion group or issue tracker.

  * [Download a recent stable build](https://repo1.maven.org/maven2/org/jmxtrans/jmxtrans/) (or a [SNAPSHOT one](https://oss.sonatype.org/content/repositories/snapshots/org/jmxtrans/jmxtrans/))
  * See the [Wiki](https://github.com/jmxtrans/jmxtrans/wiki) for full documentation.
  * Join the [Google Group](http://groups.google.com/group/jmxtrans) if you have anything to discuss or [follow the commits](http://groups.google.com/group/jmxtrans-commits). Please don't email Jon directly because he just doesn't have enough time to answer every question individually.
  * People are [talking - this is me! (skip to 21:45)](http://www.justin.tv/kctv88/b/290736874) and [talking](http://www.slideshare.net/cyrille.leclerc/paris-devops-monitoring-and-feature-toggle-pattern-with-jmx) and [talking (skip to 34:40)](http://www.justin.tv/kctv88/b/288229232) and [(french talking)](http://www.slideshare.net/henri.gomez/devops-retour-dexprience-marsjug-du-29-juin-2011) about it.
  * If you are seeing duplication of output data, look for 'typeNames' in the documentation.
  * If you like this project, please tell your friends, blog & tweet. I'd really love your help getting more publicity.

Coda Hale did [an excellent talk](http://pivotallabs.com/talks/139-metrics-metrics-everywhere) for [Pivotal Labs](http://pivotallabs.com/) on *why* metrics matter. Great justification for using a tool like jmxtrans.

![render](http://jmxtrans.googlecode.com/svn/wiki/render.png)

## Special thanks:

* [JetBrains](https://www.jetbrains.com/buy/opensource/) for providing us with IntelliJ licenses,
* [EJ Technologies](https://www.ej-technologies.com/) for providing us with licenses of their [Java profiler](https://www.ej-technologies.com/products/jprofiler/overview.html).


## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fjmxtrans%2Fjmxtrans.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2Fjmxtrans%2Fjmxtrans?ref=badge_large)
