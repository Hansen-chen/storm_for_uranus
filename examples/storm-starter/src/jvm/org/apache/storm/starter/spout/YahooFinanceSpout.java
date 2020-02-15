package org.apache.storm.starter.spout;

import java.util.*;
import java.io.*;
import java.math.BigDecimal;

//import yahoofinace packages
import org.apache.storm.utils.Utils;
import yahoofinance.YahooFinance;
import yahoofinance.Stock;

import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import org.apache.storm.topology.IRichSpout;
import org.apache.storm.topology.OutputFieldsDeclarer;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;

public class YahooFinanceSpout implements IRichSpout {
    private SpoutOutputCollector collector;
    private boolean completed = false;
    private TopologyContext context;

    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector){
        this.context = context;
        this.collector = collector;
    }

    public void nextTuple() {
        try {
            //Stock stock = YahooFinance.get("INTC");
            //BigDecimal price = stock.getQuote().getPrice();

            //this.collector.emit(new Values("INTC", price.doubleValue()));
            this.collector.emit(new Values("INTC", 67.27));
            //stock = YahooFinance.get("GOOGL");
            //price = stock.getQuote().getPrice();

            this.collector.emit(new Values("GOOGL", 1,518.73));
            //this.collector.emit(new Values("GOOGL", price.doubleValue()));
            //stock = YahooFinance.get("AAPL");
            //price = stock.getQuote().getPrice();

            //this.collector.emit(new Values("AAPL", price.doubleValue()));
            this.collector.emit(new Values("AAPL", 324.95));
        } catch(Exception e) {}
        Utils.sleep(1000*60*60);
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("company", "price"));
    }

    public void close() {}

    public boolean isDistributed() {
        return false;
    }

    public void activate() {}

    public void deactivate() {}

    public void ack(Object msgId) {}

    public void fail(Object msgId) {}

    public Map<String, Object> getComponentConfiguration() {
        return null;
    }

}