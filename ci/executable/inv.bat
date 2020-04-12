@ECHO off

REM Get parent directory of this script
SET BASEDIR=%~dp0

REM Get lib directory
SET LIBDIR=%BASEDIR%../lib

REM Get INV jar
SET INVJAR=${project.artifactId}-${project.version}.jar

REM Call INV jar with parameters
java -jar %LIBDIR%/%INVJAR% "%*"