mvn deploy -Pdistribute --settings .travis-settings.xml -DskipTests=true -B

pushd ./target/image/
bash build.sh