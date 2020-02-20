package org.apache.storm.starter;

import java.util.*;

import org.apache.storm.starter.bolt.HashtagCounterBolt;
import org.apache.storm.starter.bolt.HashtagReaderBolt;
import org.apache.storm.starter.spout.TwitterSampleSpout;
import org.apache.storm.topology.ConfigurableTopology;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.Config;
import org.apache.storm.topology.TopologyBuilder;

public class TwitterHashtagStorm extends ConfigurableTopology {

    public static void main(String[] args) throws Exception {
        ConfigurableTopology.start(new TwitterHashtagStorm(), args);
    }


    @Override
    protected int run(String[] args) throws Exception {
        String consumerKey = args[0];
        String consumerSecret = args[1];

        String accessToken = args[2];
        String accessTokenSecret = args[3];

        String[] arguments = args.clone();
        String[] keyWords = Arrays.copyOfRange(arguments, 4, arguments.length);

        conf.put(Config.TOPOLOGY_ACKER_EXECUTORS, 0);
        conf.setDebug(false);

        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("twitter-spout", new TwitterSampleSpout(consumerKey,
                consumerSecret, accessToken, accessTokenSecret, keyWords));

        builder.setBolt("twitter-hashtag-reader-bolt", new HashtagReaderBolt())
                .shuffleGrouping("twitter-spout");

        builder.setBolt("twitter-hashtag-counter-bolt", new HashtagCounterBolt())
                .fieldsGrouping("twitter-hashtag-reader-bolt", new Fields("hashtag"));

        return submit("TwitterHashtagStorm", conf, builder);

    }
}