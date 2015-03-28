#!/bin/bash
set -ev

if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  if [ "$TRAVIS_BRANCH" == "master" ]; then
    echo "Building master"
    mvn deploy --settings target/travis/settings.xml -B -V -PwithMutationTests,gpg,rpm,deb
  elif [ "$TRAVIS_BRANCH" == "release" ]; then
    if [[ `git log --format=%B -n 1` == *"[maven-release-plugin]"* ]]; then
      echo "Do not release commits created by maven release plugin"
    else
      echo "Building release"
      mvn release:prepare --settings target/travis/settings.xml -B -V -PwithMutationTests,gpg,rpm,deb
      mvn release:perform --settings target/travis/settings.xml -B -V -PwithMutationTests,gpg,rpm,deb
      git checkout master
      git merge release
      git push origin HEAD
    fi
  else
    echo "Building feature branch"
    mvn verify --settings target/travis/settings.xml -B -V -PwithMutationTests,rpm,deb
  fi
else
  echo "Building pull request"
  mvn verify --settings target/travis/settings.xml -B -V -PwithMutationTests,rpm,deb
fi
