mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar -Dsonar.projectKey=inv
mvn deploy -Pdistribute --settings .travis-settings.xml -DskipTests=true -B

pushd ./target/image/ || return
bash build.sh