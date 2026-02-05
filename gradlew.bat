@echo off
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar;%APP_HOME%gradle\wrapper\gradle-wrapper-shared.jar
java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
