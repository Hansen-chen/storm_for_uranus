FROM uranus:latest

COPY ./ /storm/

WORKDIR /storm/

RUN mvn install:install-file -Dfile=/uranus/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/rt.jar -DgroupId=edu.anonimity.sgx -DartifactId=rt -Dversion=1.0 -Dpackaging=jar

FROM maven:3.6.3-jdk-11 AS build2

WORKDIR /storm/

RUN cd storm_for_uranus && mvn clean package install -DskipTests=true -Dcheckstyle.skip && cd storm-dist/binary && mvn package -Dgpg.skip=true && cp ./final-package/target/apache-storm-2.2.0-SNAPSHOT.tar.gz /storm/compiled && cd /storm/compiled && tar zxvf apache-storm-2.2.0-SNAPSHOT.tar.gz

RUN mkdir /storm/storm-local-data