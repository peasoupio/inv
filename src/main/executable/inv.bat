set batdir=%~dp0

java -jar %batdir%../lib/${project.artifactId}-${project.version}.jar "%*"