mvn deploy -Pdistribute --settings .travis-settings.xml -DskipTests=true -B
bash ./target/image/build.sh