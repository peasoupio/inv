@ECHO off

mvn -q -f core exec:java -Dexec.mainClass="io.peasoup.inv.Main" -Dexec.args="%*"