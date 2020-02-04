# Famous Storm Examples

## Mobile Call Log Analyzer

> If you are a registered Yahoo! user, then you can customize Yahoo! Finance to take 
> advantage of its certain offerings. Yahoo! Finance API is used to query financial data
> from Yahoo!

> This API displays data that is delayed by 15-minutes from real time, and updates its 
> database every 1 minute, to access current stock-related information. Now let us take
> a real-time scenario of a company and see how to raise an alert when its stock value 
> goes below 100.

* YahooFinanceSpout

> The purpose of spout is to get the details of the company and emit the prices to 
> bolts. You can use the following program code to create a spout.

```java
import java.util.*;
import java.io.*;
import java.math.BigDecimal;

//import yahoofinace packages
import yahoofinance.YahooFinance;
import yahoofinance.Stock;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.OutputFieldsDeclarer;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;

public class YahooFinanceSpout implements IRichSpout {
   private SpoutOutputCollector collector;
   private boolean completed = false;
   private TopologyContext context;
	
   @Override
   public void open(Map conf, TopologyContext context, SpoutOutputCollector collector){
      this.context = context;
      this.collector = collector;
   }

   @Override
   public void nextTuple() {
      try {
         Stock stock = YahooFinance.get("INTC");
         BigDecimal price = stock.getQuote().getPrice();

         this.collector.emit(new Values("INTC", price.doubleValue()));
         stock = YahooFinance.get("GOOGL");
         price = stock.getQuote().getPrice();

         this.collector.emit(new Values("GOOGL", price.doubleValue()));
         stock = YahooFinance.get("AAPL");
         price = stock.getQuote().getPrice();

         this.collector.emit(new Values("AAPL", price.doubleValue()));
      } catch(Exception e) {}
   }

   @Override
   public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("company", "price"));
   }

   @Override
   public void close() {}
	
   public boolean isDistributed() {
      return false;
   }

   @Override
   public void activate() {}

   @Override
   public void deactivate() {}

   @Override
   public void ack(Object msgId) {}

   @Override
   public void fail(Object msgId) {}

   @Override
   public Map<String, Object> getComponentConfiguration() {
      return null;
   }
	
}
```

* PriceCutOffBolt

> Here the purpose of bolt is to process the given company’s prices when the prices fall 
> below 100. It uses Java Map object to set the cutoff price limit alert as true when
> the stock prices fall below 100; otherwise false. The complete program code is as 
> follows

```java
import java.util.HashMap;
import java.util.Map;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;

import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;

import backtype.storm.tuple.Tuple;

public class PriceCutOffBolt implements IRichBolt {
   Map<String, Integer> cutOffMap;
   Map<String, Boolean> resultMap;
	
   private OutputCollector collector;

   @Override
   public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
      this.cutOffMap = new HashMap <String, Integer>();
      this.cutOffMap.put("INTC", 100);
      this.cutOffMap.put("AAPL", 100);
      this.cutOffMap.put("GOOGL", 100);

      this.resultMap = new HashMap<String, Boolean>();
      this.collector = collector;
   }

   @Override
   public void execute(Tuple tuple) {
      String company = tuple.getString(0);
      Double price = tuple.getDouble(1);

      if(this.cutOffMap.containsKey(company)){
         Integer cutOffPrice = this.cutOffMap.get(company);

         if(price < cutOffPrice) {
            this.resultMap.put(company, true);
         } else {
            this.resultMap.put(company, false);
         }
      }
		
      collector.ack(tuple);
   }

   @Override
   public void cleanup() {
      for(Map.Entry<String, Boolean> entry:resultMap.entrySet()){
         System.out.println(entry.getKey()+" : " + entry.getValue());
      }
   }

   @Override
   public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("cut_off_price"));
   }
	
   @Override
   public Map<String, Object> getComponentConfiguration() {
      return null;
   }
	
}
```

* YahooFinanceStorm

> This is the main application where YahooFinanceSpout.java and PriceCutOffBolt.java are 
> connected together and produce a topology. The following program code shows how you 
> can submit a topology.

```java
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;

public class YahooFinanceStorm {
   public static void main(String[] args) throws Exception{
      Config config = new Config();
      config.setDebug(true);
		
      TopologyBuilder builder = new TopologyBuilder();
      builder.setSpout("yahoo-finance-spout", new YahooFinanceSpout());

      builder.setBolt("price-cutoff-bolt", new PriceCutOffBolt())
         .fieldsGrouping("yahoo-finance-spout", new Fields("company"));
			
      LocalCluster cluster = new LocalCluster();
      cluster.submitTopology("YahooFinanceStorm", config, builder.createTopology());
      Thread.sleep(10000);
      cluster.shutdown();
   }
}
```