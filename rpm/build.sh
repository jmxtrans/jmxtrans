#!/bin/sh

if [ $# = 0 ]; then
  echo "Usage: build.sh jmxtrans-xxx.zip [VERSION] [RELEASE]"
  exit 1
fi

# Copy source
cp $1 SOURCES

SOURCE=`basename $1`

if [ $# -ge 2 ]; then
  VERSION=$2
else
  VERSION=`echo $SOURCE | cut -d '-' -f2 | cut -d '.' -f1`
fi

if [ $# -ge 3 ]; then
  RELEASE=$3
else
  RELEASE="0"
fi

echo Version to package is $VERSION, release $RELEASE

# prepare fresh directories
rm -rf BUILD RPMS SRPMS TEMP
mkdir -p BUILD RPMS SRPMS TEMP

# Build using rpmbuild
rpmbuild -ba --define="_topdir $PWD" --define="_tmppath $PWD/TEMP" --define="VERSION $VERSION" --define="RELEASE $RELEASE" --define="JMXTRANS_SOURCE $SOURCE"  SPECS/jmxtrans.spec
