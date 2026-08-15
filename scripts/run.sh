#!/usr/bin/env bash
# Build the boot jar and run it with the JDWP debug agent on port 6332.
set -euo pipefail

cd "$(dirname "$0")/.."

./gradlew bootJar
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:6332 -jar build/libs/ds-spring-user-framework-demo-1.0.1-SNAPSHOT.jar --spring.profiles.active=local
