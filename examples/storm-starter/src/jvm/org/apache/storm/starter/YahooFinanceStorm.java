package org.apache.storm.starter;

import org.apache.storm.starter.bolt.PriceCutOffBolt;
import org.apache.storm.starter.spout.YahooFinanceSpout;
import org.apache.storm.topology.ConfigurableTopology;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import org.apache.storm.Config;

import org.apache.storm.topology.TopologyBuilder;

public class YahooFinanceStorm extends ConfigurableTopology {

    public static void main(String[] args) throws Exception {
        ConfigurableTopology.start(new YahooFinanceStorm(), args);
    }

    @Override
    protected int run(String[] args) throws Exception {
        conf.setDebug(false);
        conf.put(Config.TOPOLOGY_ACKER_EXECUTORS, 0);
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("yahoo-finance-spout", new YahooFinanceSpout());

        builder.setBolt("price-cutoff-bolt", new PriceCutOffBolt())
                .fieldsGrouping("yahoo-finance-spout", new Fields("company"));


        return submit("YahooFinanceStorm", conf, builder);

    }
}