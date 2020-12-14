@ECHO off
SET DIR=%~dp0
mvn -q -f %DIR%/launcher exec:java -Dexec.mainClass="io.peasoup.inv.Main" -Dexec.args="%*"