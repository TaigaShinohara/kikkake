#!/bin/bash
set -e
cd "$(dirname "$0")"
export JAVA_HOME=~/workspace/tools/jdk21
export GRADLE_USER_HOME=~/workspace/kikkake/.gradle-home
export GRADLE_OPTS="-Xmx768m -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
if ! curl -s -o /dev/null --max-time 3 http://127.0.0.1:18080/central/; then
  (cd ~/workspace/tools && nohup python3 mirror.py > /tmp/mirror.log 2>&1 &)
  sleep 1
fi
~/workspace/tools/gradle-8.9/bin/gradle "$@" -Dorg.gradle.daemon=false -Dkotlin.daemon.enabled=false --max-workers=1
