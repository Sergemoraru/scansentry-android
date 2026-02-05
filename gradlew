#!/bin/sh

APP_HOME=$(cd "$(dirname "$0")" && pwd)

# Use both wrapper jars (Gradle 8.x split wrapper)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar:$APP_HOME/gradle/wrapper/gradle-wrapper-shared.jar"

JAVA_CMD="java"

exec "$JAVA_CMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
