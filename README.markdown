# Storm copy used by Hansen

## Machine
### Address : 202.45.128.173
### No. : 14
### Gate : gatekeeper.cs.hku.hk

## Build Storm

0. wget binary targz file from (https://maven.apache.org/download.cgi?Preferred=http%3A%2F%2Fapache.01link.hk%2F)
1. export MAVEN_HOME=/home/hansen/apache-maven-3.6.3 && export PATH=${PATH}:${MAVEN_HOME}/bin && export M2_HOME=/home/hansen/apache-maven-3.6.3 && export PATH=${PATH}:${M2_HOME}/bin
(mvn location and version got from 'mvn -version')   
2. (optional)add ali maven mirror to maven folder conf/mirror/setting.xml (https://blog.csdn.net/wudinaniya/article/details/98116734)
3. (optional) mvn dependency::tree && mvn help:effective-pom
4. mvn install:install-file -Dfile=../openjdk-sgx/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/rt.jar -DgroupId=edu.anonimity.sgx -DartifactId=rt -Dversion=1.0 -Dpackaging=jar
5. mvn clean package install -DskipTests=true -Dcheckstyle.skip
&& cd storm-dist/binary 
&& mvn package -Dgpg.skip=true
&& cp ./final-package/target/apache-storm-2.2.0-SNAPSHOT.tar.gz ~/source_code/storm/compiled 
&& cd  ~/source_code/storm/compiled 
&& tar zxvf apache-storm-2.2.0-SNAPSHOT.tar.gz
&& cd apache-storm-2.2.0-SNAPSHOT/

## Run Examples (~/openjdk-sgx/ev_test/TestSuit.java, 202.45.128.173, javac -d . -cp path_to_rt.jar TestSuit.java, java -ea TestSuit)

### Other terminals:
0. run zookeeper using normal java
1. export JAVA_HOME=~/openjdk-sgx/build/linux-x86_64-normal-server-release/images/j2sdk-image && export PATH=$JAVA_HOME/bin:$PATH
2. run nimbus, supervisor http://admicloud.github.io/www/storm.html

### One terminal:
0. cd ~/source_code/storm/compiled/apache-storm-2.2.0-SNAPSHOT/examples/storm-starter
1. mvn package -Dcheckstyle.skip 

### One terminal:
3. ./bin/storm jar examples/storm-starter/target/storm-starter-2.2.0-SNAPSHOT.jar org.apache.storm.starter.AnchoredWordCount testing
4. ./bin/storm kill testing

## Examples
1. ExclamationTopology
2. AnchoredWordCount
3. LogAnalyserStorm
4. YahooFinanceStorm

## Get Log Output

0. cd ~/source_code/storm/compiled/apache-storm-2.2.0-SNAPSHOT/logs/workers-artifacts/
1. cd topology name
2. cd worker id
3. grep keyword worker.log (or vim worker.log)

## Useful Link

1. https://blog.csdn.net/lizheng520lp/article/details/84862380
2. https://www.cnblogs.com/davenkin/archive/2012/02/15/install-jar-into-maven-local-repository.html
3. http://pclevin.blogspot.com/2015/02/maven-dependency-scope.html
4. http://www.corejavaguru.com/bigdata/storm/word-count-topology




Master Branch:  
[![Travis CI](https://travis-ci.org/apache/storm.svg?branch=master)](https://travis-ci.org/apache/storm)
[![Maven Version](https://maven-badges.herokuapp.com/maven-central/org.apache.storm/storm-core/badge.svg)](http://search.maven.org/#search|gav|1|g:"org.apache.storm"%20AND%20a:"storm-core")
 
Storm is a distributed realtime computation system. Similar to how Hadoop provides a set of general primitives for doing batch processing, Storm provides a set of general primitives for doing realtime computation. Storm is simple, can be used with any programming language, [is used by many companies](http://storm.apache.org/Powered-By.html), and is a lot of fun to use!

The [Rationale page](http://storm.apache.org/documentation/Rationale.html) explains what Storm is and why it was built. [This presentation](http://vimeo.com/40972420) is also a good introduction to the project.

Storm has a website at [storm.apache.org](http://storm.apache.org). Follow [@stormprocessor](https://twitter.com/stormprocessor) on Twitter for updates on the project.

## Documentation

Documentation and tutorials can be found on the [Storm website](http://storm.apache.org/documentation/Home.html).

Developers and contributors should also take a look at our [Developer documentation](DEVELOPER.md).
 

## Getting help

__NOTE:__ The google groups account storm-user@googlegroups.com is now officially deprecated in favor of the Apache-hosted user/dev mailing lists.

### Storm Users
Storm users should send messages and subscribe to [user@storm.apache.org](mailto:user@storm.apache.org).

You can subscribe to this list by sending an email to [user-subscribe@storm.apache.org](mailto:user-subscribe@storm.apache.org). Likewise, you can cancel a subscription by sending an email to [user-unsubscribe@storm.apache.org](mailto:user-unsubscribe@storm.apache.org).

You can also [browse the archives of the storm-user mailing list](http://mail-archives.apache.org/mod_mbox/storm-user/).

### Storm Developers
Storm developers should send messages and subscribe to [dev@storm.apache.org](mailto:dev@storm.apache.org).

You can subscribe to this list by sending an email to [dev-subscribe@storm.apache.org](mailto:dev-subscribe@storm.apache.org). Likewise, you can cancel a subscription by sending an email to [dev-unsubscribe@storm.apache.org](mailto:dev-unsubscribe@storm.apache.org).

You can also [browse the archives of the storm-dev mailing list](http://mail-archives.apache.org/mod_mbox/storm-dev/).

Storm developers who would want to track the JIRA issues should subscribe to [issues@storm.apache.org](mailto:issues@storm.apache.org).

You can subscribe to this list by sending an email to [issues-subscribe@storm.apache.org](mailto:issues-subscribe@storm.apache.org). Likewise, you can cancel a subscription by sending an email to [issues-unsubscribe@storm.apache.org](mailto:issues-unsubscribe@storm.apache.org).

You can view the archives of the mailing list [here](http://mail-archives.apache.org/mod_mbox/storm-issues/).

### Issue tracker
In case you want to raise a bug/feature or propose an idea, please use [Apache Jira](https://issues.apache.org/jira/projects/STORM)

### Which list should I send/subscribe to?
If you are using a pre-built binary distribution of Storm, then chances are you should send questions, comments, storm-related announcements, etc. to [user@storm.apache.org](mailto:user@storm.apache.org).

If you are building storm from source, developing new features, or otherwise hacking storm source code, then [dev@storm.apache.org](mailto:dev@storm.apache.org) is more appropriate.

If you are committers and/or PMCs, or contributors looking for following up and participating development of Storm, then you would want to also subscribe [issues@storm.apache.org](issues@storm.apache.org) in addition to [dev@storm.apache.org](dev@storm.apache.org).

### What will happen with storm-user@googlegroups.com?
All existing messages will remain archived there, and can be accessed/searched [here](https://groups.google.com/forum/#!forum/storm-user).

New messages sent to storm-user@googlegroups.com will either be rejected/bounced or replied to with a message to direct the email to the appropriate Apache-hosted group.

### IRC
You can also come to the #storm-user room on [freenode](http://freenode.net/). You can usually find a Storm developer there to help you out.

## License

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

The LICENSE and NOTICE files cover the source distributions. The LICENSE-binary and NOTICE-binary files cover the binary distributions. The DEPENDENCY-LICENSES file lists the licenses of all dependencies of Storm, including those not packaged in the source or binary distributions, such as dependencies of optional connector modules.


## Project lead

* Nathan Marz ([@nathanmarz](http://twitter.com/nathanmarz))

## Committers

* James Xu ([@xumingming](https://github.com/xumingming))
* Jason Jackson ([@jason_j](http://twitter.com/jason_j))
* Andy Feng ([@anfeng](https://github.com/anfeng))
* Flip Kromer ([@mrflip](https://github.com/mrflip))
* David Lao ([@davidlao2k](https://github.com/davidlao2k))
* P. Taylor Goetz ([@ptgoetz](https://github.com/ptgoetz))
* Derek Dagit ([@d2r](https://github.com/d2r))
* Robert Evans ([@revans2](https://github.com/revans2))
* Michael G. Noll ([@miguno](https://github.com/miguno))
* Kishor Patil ([@kishorvpatil](https://github.com/kishorvpatil))
* Sriharsha Chintalapani([@harshach](https://github.com/harshach))
* Sean Zhong ([@clockfly](http://github.com/clockfly))
* Kyle Nusbaum ([@knusbaum](https://github.com/knusbaum))
* Parth Brahmbhatt ([@Parth-Brahmbhatt](https://github.com/Parth-Brahmbhatt))
* Jungtaek Lim ([@HeartSaVioR](https://github.com/HeartSaVioR))
* Aaron Dossett ([@dossett](https://github.com/dossett))
* Matthias J. Sax ([@mjsax](https://github.com/mjsax))
* Arun Mahadevan ([@arunmahadevan](https://github.com/arunmahadevan))
* Boyang Jerry Peng ([@jerrypeng](https://github.com/jerrypeng))
* Zhuo Liu ([@zhuoliu](https://github.com/zhuoliu))
* Haohui Mai ([@haohui](https://github.com/haohui))
* Sanket Chintapalli ([@redsanket](https://github.com/redsanket))
* Longda Feng ([@longda](https://github.com/longdafeng))
* John Fang ([@hustfxj](https://github.com/hustfxj))
* Abhishek Agarwal ([@abhishekagarwal87](https://github.com/abhishekagarwal87))
* Satish Duggana ([@satishd](https://github.com/satishd))
* Xin Wang ([@vesense](https://github.com/vesense))
* Hugo da Cruz Louro ([@hmcl](https://github.com/hmcl))
* Stig Rohde Døssing ([@srdo](https://github.com/srdo/))
* Roshan Naik ([@roshannaik](http://github.com/roshannaik))
* Ethan Li ([@Ethanlm](https://github.com/Ethanlm))
* Govind Menon ([@govind](https://github.com/govind-menon))

## Acknowledgements

YourKit is kindly supporting open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of innovative and intelligent tools for profiling Java and .NET applications. Take a look at YourKit's leading software products: [YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and [YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).
