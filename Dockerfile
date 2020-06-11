FROM uranus:latest

COPY ./ /storm/

WORKDIR /storm/

RUN mkdir /storm/storm-maven

RUN cd /storm/storm-maven && wget http://ftp.cuhk.edu.hk/pub/packages/apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz && tar -xvf apache-maven-3.6.3-bin.tar.gz && export MAVEN_HOME=/storm/storm-maven/apache-maven-3.6.3 && export PATH=${PATH}:${MAVEN_HOME}/bin && export M2_HOME=apache-maven-3.6.3 && export PATH=${PATH}:${M2_HOME}/bin

RUN cd /storm/ && mvn install:install-file -Dfile=/uranus/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/rt.jar -DgroupId=edu.anonimity.sgx -DartifactId=rt -Dversion=1.0 -Dpackaging=jar

RUN mvn clean package install -DskipTests=true -Dcheckstyle.skip && cd storm-dist/binary && mvn package -Dgpg.skip=true && cp ./final-package/target/apache-storm-2.2.0-SNAPSHOT.tar.gz /storm/compiled && cd /storm/compiled && tar zxvf apache-storm-2.2.0-SNAPSHOT.tar.gz

RUN mkdir /storm/storm-local-data