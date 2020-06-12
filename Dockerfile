FROM uranus:latest

COPY ./ /storm/

WORKDIR /storm/

RUN wget http://ftp.cuhk.edu.hk/pub/packages/apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz && tar -xvf apache-maven-3.6.3-bin.tar.gz

RUN /storm/apache-maven-3.6.3/bin/mvn install:install-file -Dfile=/uranus/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/rt.jar -DgroupId=edu.anonimity.sgx -DartifactId=rt -Dversion=1.0 -Dpackaging=jar && /storm/apache-maven-3.6.3/bin/mvn clean package install -DskipTests=true -Dcheckstyle.skip

WORKDIR /storm/storm-dist/binary/

RUN /storm/apache-maven-3.6.3/bin/mvn package -Dgpg.skip=true

RUN mkdir /storm/compiled && cp /storm/storm-dist/binary/final-package/target/apache-storm-2.2.0-SNAPSHOT.tar.gz /storm/compiled

WORKDIR /storm/compiled/

RUN tar zxvf /storm/compiled/apache-storm-2.2.0-SNAPSHOT.tar.gz

WORKDIR /storm/compiled/apache-storm-2.2.0-SNAPSHOT/examples/storm-starter/

RUN /storm/apache-maven-3.6.3/bin/mvn package -Dcheckstyle.skip

WORKDIR /storm/

RUN mkdir /storm/storm-local-data