@ECHO off

SET SCRIPTPATH=%~dp0

REM Call INV jar with parameters
java -jar %SCRIPTPATH%/${project.artifactId}-${project.version}.jar %*