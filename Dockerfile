FROM uranus:latest

COPY ./ /storm/

WORKDIR /storm/

RUN mvn clean package install -DskipTests=true -Dcheckstyle.skip && cd storm-dist/binary && mvn package -Dgpg.skip=true && cp ./final-package/target/apache-storm-2.2.0-SNAPSHOT.tar.gz /storm/compiled && cd /storm/compiled && tar zxvf apache-storm-2.2.0-SNAPSHOT.tar.gz

RUN mkdir /storm/storm-local-data

RUN cd cd /storm/compiled/apache-storm-2.2.0-SNAPSHOT/examples/storm-starter

RUN mvn package -Dcheckstyle.skip

