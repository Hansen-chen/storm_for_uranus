FROM uranus:latest

COPY . .

WORKDIR /storm/

RUN git clone https://github.com/Hansen-chen/storm_for_uranus

RUN wget http://ftp.cuhk.edu.hk/pub/packages/apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz

RUN export MAVEN_HOME=apache-maven-3.6.3 && export PATH=${PATH}:${MAVEN_HOME}/bin && export M2_HOME=apache-maven-3.6.3 && export PATH=${PATH}:${M2_HOME}/bin

RUN cd storm_for_uranus && mvn clean package install -DskipTests=true -Dcheckstyle.skip && cd storm-dist/binary && mvn package -Dgpg.skip=true && cp ./final-package/target/apache-storm-2.2.0-SNAPSHOT.tar.gz /storm/compiled && cd /storm/compiled && tar zxvf apache-storm-2.2.0-SNAPSHOT.tar.gz

RUN mkdir /storm/storm-local-data

RUN cd /storm/compiled/apache-storm-2.2.0-SNAPSHOT/examples/storm-starter

RUN mvn package -Dcheckstyle.skip

