#!/usr/bin/env bash
# Builds the jar, copies it with db/ to the test VM, applies pending
# migrations and restarts the systemd user service "biblia".
#
# Usage: ./deploy.sh [user@host]

set -euo pipefail

TARGET="${1:-jdamico@192.168.68.137}"
JAVA_HOME="${JAVA_HOME_21:-/usr/lib/jvm/java-21-openjdk-amd64}"

cd "$(dirname "$0")"

JAVA_HOME="$JAVA_HOME" mvn -q -DskipTests package

ssh "$TARGET" 'mkdir -p ~/biblia'
scp -q target/biblia-*.jar "$TARGET:biblia/biblia.jar"
scp -q -r db scripts "$TARGET:biblia/"

ssh "$TARGET" 'cd ~/biblia && ./db/migrate.sh && systemctl --user restart biblia && systemctl --user is-active biblia'
