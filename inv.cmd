@ECHO off

mvn -q -f %~dp0/core exec:java -Dexec.mainClass="io.peasoup.inv.Main" -Dexec.args="%*"