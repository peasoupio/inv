mvn -B -DskipTests=true org.jacoco:jacoco-maven-plugin:prepare-agent sonar:sonar -Dsonar.projectKey=inv
mvn -B -DskipTests=true deploy -Pdistribute --settings ./ci/.travis-settings.xml

pushd ./target/docker/ || return
bash build.sh