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

      # decrypt SSH key so we can push release to GitHub
      gpg --homedir ${HOME}/travis \
          --output ${HOME}/.ssh/id_rsa \
          --passphrase ${GPG_PASSPHRASE} \
          --decrypt ${HOME}/travis/id_rsa.gpg
      chmod 600 ${HOME}/.ssh/id_rsa

      # configure our git identity
      git config --global user.email "travis@jmxtrans.org"
      git config --global user.name "JmxTrans travis build"

      # travis checkout the commit as detached head (which is normally what we
      # want) but maven release plugin does not like working in detached head
      # mode. This might be a problem if other commits have already been pushed
      # to the release branch, but in that case we will have problem anyway.
      git checkout release

      mvn release:prepare --settings ${MVN_SETTINGS} -B -V -Pgpg,rpm,deb -Darguments="--settings ${MVN_SETTINGS}"
      mvn release:perform --settings ${MVN_SETTINGS} -B -V -Pgpg,rpm,deb -Darguments="--settings ${MVN_SETTINGS}"

      git fetch origin +master:master
      git checkout master
      git merge release
      git push origin master
    fi
  else
    echo "Building feature branch"
    mvn verify --settings ${MVN_SETTINGS} -B -V -PwithMutationTests,rpm,deb
  fi
else
  echo "Building pull request"
  mvn verify --settings ${MVN_SETTINGS} -B -V -PwithMutationTests
fi
