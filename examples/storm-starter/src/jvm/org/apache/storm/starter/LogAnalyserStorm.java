package org.apache.storm.starter;

import org.apache.storm.Config;
import org.apache.storm.starter.bolt.CallLogCounterBolt;
import org.apache.storm.starter.bolt.CallLogCreatorBolt;
import org.apache.storm.starter.spout.FakeCallLogReaderSpout;
import org.apache.storm.topology.ConfigurableTopology;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;

//import storm configuration packages


//Create main class LogAnalyserStorm submit topology.
public class LogAnalyserStorm extends ConfigurableTopology {
    public static void main(String[] args) throws Exception {
        ConfigurableTopology.start(new LogAnalyserStorm(), args);
    }

    @Override
    protected int run(String[] args) throws Exception {
        //Create Config instance for cluster configuration
        conf.setDebug(true);
        conf.registerMetricsConsumer(org.apache.storm.metric.LoggingMetricsConsumer.class);
        conf.setStatsSampleRate(1.0d);


        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("call-log-reader-spout", new FakeCallLogReaderSpout());

        builder.setBolt("call-log-creator-bolt", new CallLogCreatorBolt())
                .shuffleGrouping("call-log-reader-spout");

        builder.setBolt("call-log-counter-bolt", new CallLogCounterBolt())
                .fieldsGrouping("call-log-creator-bolt", new Fields("call"));

        return submit("LogAnalyserStorm", conf, builder);

    }
}