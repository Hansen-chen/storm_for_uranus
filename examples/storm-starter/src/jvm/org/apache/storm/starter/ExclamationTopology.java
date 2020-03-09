/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.  The ASF licenses this file to you under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.apache.storm.starter;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.storm.Config;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.testing.TestWordSpout;
import org.apache.storm.topology.ConfigurableTopology;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a basic example of a Storm topology.
 */
public class ExclamationTopology extends ConfigurableTopology {

    public static void main(String[] args) throws Exception {
        ConfigurableTopology.start(new ExclamationTopology(), args);
    }

    protected int run(String[] args) {
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("word", new ExclamationTestWordSpout(), 1);
        builder.setBolt("exclaim1", new ExclamationBolt(), 1).shuffleGrouping("word");
        builder.setBolt("exclaim2", new ExclamationBolt(), 1).shuffleGrouping("exclaim1");

        conf.registerMetricsConsumer(org.apache.storm.metric.LoggingMetricsConsumer.class);
        conf.setDebug(true);
        conf.registerMetricsConsumer(org.apache.storm.metric.LoggingMetricsConsumer.class,1);
        //conf.setStatsSampleRate(1.0d);
        conf.put(Config.TOPOLOGY_MAX_SPOUT_PENDING, 50);
        conf.put(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, 60);
        conf.put(Config.TOPOLOGY_ACKER_EXECUTORS, 5);

        String topologyName = "test";

        conf.setNumWorkers(1);

        if (args != null && args.length > 0) {
            topologyName = args[0];
        }

        return submit(topologyName, conf, builder);
    }

    public static class ExclamationBolt extends BaseRichBolt {
        OutputCollector collector;

        @Override
        public void prepare(Map<String, Object> conf, TopologyContext context, OutputCollector collector) {
            this.collector = collector;
        }

        @Override
        public void execute(Tuple tuple) {
            collector.emit(tuple, new Values(tuple.getString(0) + "!!!"));
            collector.ack(tuple);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("word"));
        }

    }
    public static class ExclamationTestWordSpout extends BaseRichSpout {

        SpoutOutputCollector collector;

        @Override
        public void open(Map<String, Object> conf, TopologyContext context, SpoutOutputCollector collector) {
            this.collector = collector;
        }


        @Override
        public void nextTuple() {
            Utils.sleep(1000);
            final String[] words = new String[]{ "nathan", "mike", "jackson", "golda", "bertels" };
            final Random rand = new Random();
            final String word = words[rand.nextInt(words.length)];
            collector.emit(new Values(word), UUID.randomUUID());
        }

        @Override
        public void ack(Object msgId) {

        }

        @Override
        public void fail(Object msgId) {

        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("word"));
        }

        @Override
        public Map<String, Object> getComponentConfiguration() {
            return null;
        }
    }

}
