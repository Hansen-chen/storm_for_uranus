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

import java.util.*;
import org.apache.storm.Config;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.*;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnchoredWordCount extends ConfigurableTopology {
    public static void main(String[] args) throws Exception {
        ConfigurableTopology.start(new AnchoredWordCount(), args);
    }
    private static final Logger LOG = LoggerFactory.getLogger(StatefulWindowingTopology.class);

    @Override
    protected int run(String[] args) throws Exception {
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("spout", new RandomSentenceSpout(), 1);

        builder.setBolt("split", new SplitSentence(), 1).shuffleGrouping("spout");
        builder.setBolt("count", new WordCount(), 1).fieldsGrouping("split", new Fields("word"));

        Config conf = new Config();
        conf.setMaxTaskParallelism(1);

        String topologyName = "word-count";

        conf.setNumWorkers(1);
        conf.setDebug(false);
        conf.put(Config.TOPOLOGY_ACKER_EXECUTORS, 0);
        conf.registerMetricsConsumer(org.apache.storm.metric.LoggingMetricsConsumer.class);
        //config.setMaxSpoutPending(500);

        if (args != null && args.length > 0) {
            topologyName = args[0];
        }
        return submit(topologyName, conf, builder);
    }

    public static class RandomSentenceSpout extends BaseRichSpout {
        SpoutOutputCollector collector;
        Random random;


        @Override
        public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
            this.collector = collector;
            this.random = new Random();
        }

        @Override
        public void nextTuple() {
            Utils.sleep(25);
            String[] sentences = new String[]{
                sentence("the cow jumped over the moon"), sentence("an apple a day keeps the doctor away"),
                sentence("four score and seven years ago"),
                sentence("snow white and the seven dwarfs"), sentence("i am at two with nature")
            };
            final String sentence = sentences[random.nextInt(sentences.length)];

            this.collector.emit(new Values(sentence), UUID.randomUUID());

        }

        protected String sentence(String input) {
            return input;
        }

        @Override
        public void ack(Object id) {
        }

        @Override
        public void fail(Object id) {
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("word"));
        }
    }

    public static class SplitSentence implements IRichBolt {

        private OutputCollector collector;

        public void prepare(Map conf, TopologyContext context, OutputCollector collector) {

            this.collector = collector;
        }


        public void execute(Tuple tuple) {
            String sentence = tuple.getString(0);
            for (String word : sentence.split("\\s+")) {
                //LOG.info("Split sentence, emit : "+word);
                collector.emit(new Values(word, 1));
            }
            collector.ack(tuple);
        }


        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("word", "count"));
        }

        public void cleanup() {

        }
        public Map<String, Object> getComponentConfiguration() {
            return null;
        }
    }

    public static class WordCount implements IRichBolt {
        Map<String, Integer> counts = new HashMap<>();
        private OutputCollector collector;

        public void prepare(Map conf, TopologyContext context, OutputCollector collector) {

            this.collector = collector;
        }

        public void execute(Tuple tuple) {
            String word = tuple.getString(0);

            Integer count = counts.get(word);
            if (count == null) {
                count = 0;
            }
            count++;
            counts.put(word, count);

            //LOG.info("Calculating "+word + ": " + count);
            //collector.emit(new Values(word, count));
            collector.ack(tuple);
        }


        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("word", "count"));
        }


        public void cleanup() {

            LOG.info("--- FINAL COUNTS ---");
            List<String> keys = new ArrayList<String>();
            keys.addAll(this.counts.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                LOG.info(key + " : " + this.counts.get(key));
            }
            LOG.info("--------------");



        }
        public Map<String, Object> getComponentConfiguration() {
            return null;
        }
    }
}
