#!/bin/bash
set -ev

MVN_SETTINGS=${HOME}/travis/settings.xml

if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  if [ "$TRAVIS_BRANCH" == "master" ]; then
    echo "Building master"
    mvn deploy --settings ${MVN_SETTINGS} -B -V -PwithMutationTests,gpg,rpm,deb
  elif [ "$TRAVIS_BRANCH" == "release" ]; then
    if [[ `git log --format=%B -n 1` == *"[maven-release-plugin]"* ]]; then
      echo "Do not release commits created by maven release plugin"
    else
      echo "Building release"

      git config --global user.email "travis@jmxtrans.org"
      git config --global user.name "JmxTrans travis build"

      mvn release:prepare --settings ${MVN_SETTINGS} -B -V -PwithMutationTests,gpg,rpm,deb -Darguments="--settings ${MVN_SETTINGS}"
      mvn release:perform --settings ${MVN_SETTINGS} -B -V -PwithMutationTests,gpg,rpm,deb -Darguments="--settings ${MVN_SETTINGS}"
      git checkout master
      git merge release
      git push origin HEAD
    fi
  else
    echo "Building feature branch"
    mvn verify --settings ${MVN_SETTINGS} -B -V -PwithMutationTests,rpm,deb
  fi
else
  echo "Building pull request"
  mvn verify --settings ${MVN_SETTINGS} -B -V -PwithMutationTests,rpm,deb
fi
