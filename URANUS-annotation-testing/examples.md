# Famous Storm Examples

## Mobile Call Log Analyzer
### Comment: This topology example has no ack/fail functionalities, however, it has Log in execute function (so may not be able to log in enclave).

> Mobile call and its duration will be given as input to Apache Storm and the Storm will 
> process and group the call between the same caller and receiver and their total number 
> of calls.

* FakeCallLogReaderSpout
> In our scenario, we need to collect the call log details. The information of the call 
> log contains.
> * caller number
> * receiver number
> * duration
> Since, we don’t have real-time information of call logs, we will generate fake call
> logs. The fake information will be created using Random class. The complete program code
> is given below.

```
import java.util.*;
//import storm tuple packages
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

//import Spout interface packages
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;

//Create a class FakeLogReaderSpout which implement IRichSpout interface 
   to access functionalities
	
public class FakeCallLogReaderSpout implements IRichSpout {
   //Create instance for SpoutOutputCollector which passes tuples to bolt.
   private SpoutOutputCollector collector;
   private boolean completed = false;
	
   //Create instance for TopologyContext which contains topology data.
   private TopologyContext context;
	
   //Create instance for Random class.
   private Random randomGenerator = new Random();
   private Integer idx = 0;

   @Override
   public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
      this.context = context;
      this.collector = collector;
   }

   @Override
   public void nextTuple() {
      if(this.idx <= 1000) {
         List<String> mobileNumbers = new ArrayList<String>();
         mobileNumbers.add("1234123401");
         mobileNumbers.add("1234123402");
         mobileNumbers.add("1234123403");
         mobileNumbers.add("1234123404");

         Integer localIdx = 0;
         while(localIdx++ < 100 && this.idx++ < 1000) {
            String fromMobileNumber = mobileNumbers.get(randomGenerator.nextInt(4));
            String toMobileNumber = mobileNumbers.get(randomGenerator.nextInt(4));
				
            while(fromMobileNumber == toMobileNumber) {
               toMobileNumber = mobileNumbers.get(randomGenerator.nextInt(4));
            }
				
            Integer duration = randomGenerator.nextInt(60);
            this.collector.emit(new Values(fromMobileNumber, toMobileNumber, duration));
         }
      }
   }

   @Override
   public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("from", "to", "duration"));
   }

   //Override all the interface methods
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

* CallLogCreatorBolt

> Call log creator bolt receives the call log tuple. The call log tuple has caller number,
> receiver number, and call duration. This bolt simply creates a new value by combining
> the caller number and the receiver number. The format of the new value is "Caller number
> – Receiver number" and it is named as new field, "call". The complete code is given 
> below.

```
//import util packages
import java.util.HashMap;
import java.util.Map;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;

//import Storm IRichBolt package
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

//Create a class CallLogCreatorBolt which implement IRichBolt interface
public class CallLogCreatorBolt implements IRichBolt {
   //Create instance for OutputCollector which collects and emits tuples to produce output
   private OutputCollector collector;

   @Override
   public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
      this.collector = collector;
   }

   @Override
   public void execute(Tuple tuple) {
      String from = tuple.getString(0);
      String to = tuple.getString(1);
      Integer duration = tuple.getInteger(2);
      collector.emit(new Values(from + " - " + to, duration));
   }

   @Override
   public void cleanup() {}

   @Override
   public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("call", "duration"));
   }
	
   @Override
   public Map<String, Object> getComponentConfiguration() {
      return null;
   }
}
```

* CallLogCounterBolt

> Call log counter bolt receives call and its duration as a tuple. This bolt initializes a
> dictionary (Map) object in the prepare method. In execute method, it checks the tuple
> and creates a new entry in the dictionary object for every new “call” value in the tuple
> and sets a value 1 in the dictionary object. For the already available entry in the
> dictionary, it just increment its value. In simple terms, this bolt saves the call and
> its count in the dictionary object. Instead of saving the call and its count in the
> dictionary, we can also save it to a datasource. The complete program code is as follows

```
import java.util.HashMap;
import java.util.Map;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

public class CallLogCounterBolt implements IRichBolt {
   Map<String, Integer> counterMap;
   private OutputCollector collector;

   @Override
   public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
      this.counterMap = new HashMap<String, Integer>();
      this.collector = collector;
   }

   @Override
   public void execute(Tuple tuple) {
      String call = tuple.getString(0);
      Integer duration = tuple.getInteger(1);
		
      if(!counterMap.containsKey(call)){
         counterMap.put(call, 1);
      }else{
         Integer c = counterMap.get(call) + 1;
         counterMap.put(call, c);
      }
		
      collector.ack(tuple);
   }

   @Override
   public void cleanup() {
      for(Map.Entry<String, Integer> entry:counterMap.entrySet()){
         System.out.println(entry.getKey()+" : " + entry.getValue());
      }
   }

   @Override
   public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("call"));
   }
	
   @Override
   public Map<String, Object> getComponentConfiguration() {
      return null;
   }
	
}
```

* LogAnalyserStorm

> This class creates the topology. The topology is running in LocalCluster

```
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

//import storm configuration packages
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;

//Create main class LogAnalyserStorm submit topology.
public class LogAnalyserStorm {
   public static void main(String[] args) throws Exception{
      //Create Config instance for cluster configuration
      Config config = new Config();
      config.setDebug(true);
		
      //
      TopologyBuilder builder = new TopologyBuilder();
      builder.setSpout("call-log-reader-spout", new FakeCallLogReaderSpout());

      builder.setBolt("call-log-creator-bolt", new CallLogCreatorBolt())
         .shuffleGrouping("call-log-reader-spout");

      builder.setBolt("call-log-counter-bolt", new CallLogCounterBolt())
         .fieldsGrouping("call-log-creator-bolt", new Fields("call"));
			
      LocalCluster cluster = new LocalCluster();
      cluster.submitTopology("LogAnalyserStorm", config, builder.createTopology());
      Thread.sleep(10000);
		
      //Stop the topology
		
      cluster.shutdown();
   }
}
```