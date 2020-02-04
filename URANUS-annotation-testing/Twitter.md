# Famous Storm Examples

## Apache storm in Twitter
### Comment: We need twitter4j Java library to access the Twitter Streaming API. Since it uses a third party API, this example may be a challenge for our annotated storm. The System.out.print function in execute also needs to be commented out.

* TwitterSampleSpout
>The purpose of spout is to get the tweets submitted by people as soon as possible. 
>Twitter provides “Twitter Streaming API”, a web service based tool to retrieve the
>tweets submitted by people in real time. Twitter Streaming API can be accessed in any
>programming language.

>twitter4j is an open source, unofficial Java library, which provides a Java based 
> module to easily access the Twitter Streaming API. twitter4j provides a listener-based
> framework to access the tweets. To access the Twitter Streaming API, we need to sign0
> in for Twitter developer account and should get the following OAuth authentication 
> details.
> * Customerkey
> * CustomerSecret
> * AccessToken
> * AccessTookenSecret
> Storm provides a twitter spout, TwitterSampleSpout, in its starter kit. We will be
> using it to retrieve the tweets. The spout needs OAuth authentication details and at
> least a keyword. The spout will emit real-time tweets based on keywords. The complete
> program code is given below.

```java
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import backtype.storm.Config;
import backtype.storm.spout.SpoutOutputCollector;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

import backtype.storm.utils.Utils;

@SuppressWarnings("serial")
public class TwitterSampleSpout extends BaseRichSpout {
   SpoutOutputCollector _collector;
   LinkedBlockingQueue<Status> queue = null;
   TwitterStream _twitterStream;
		
   String consumerKey;
   String consumerSecret;
   String accessToken;
   String accessTokenSecret;
   String[] keyWords;
		
   public TwitterSampleSpout(String consumerKey, String consumerSecret,
      String accessToken, String accessTokenSecret, String[] keyWords) {
         this.consumerKey = consumerKey;
         this.consumerSecret = consumerSecret;
         this.accessToken = accessToken;
         this.accessTokenSecret = accessTokenSecret;
         this.keyWords = keyWords;
   }
		
   public TwitterSampleSpout() {
      // TODO Auto-generated constructor stub
   }
		
   @Override
   public void open(Map conf, TopologyContext context,
      SpoutOutputCollector collector) {
         queue = new LinkedBlockingQueue<Status>(1000);
         _collector = collector;
         StatusListener listener = new StatusListener() {
            @Override
            public void onStatus(Status status) {
               queue.offer(status);
            }
					
            @Override
            public void onDeletionNotice(StatusDeletionNotice sdn) {}
					
            @Override
            public void onTrackLimitationNotice(int i) {}
					
            @Override
            public void onScrubGeo(long l, long l1) {}
					
            @Override
            public void onException(Exception ex) {}
					
            @Override
            public void onStallWarning(StallWarning arg0) {
               // TODO Auto-generated method stub
            }
         };
				
         ConfigurationBuilder cb = new ConfigurationBuilder();
				
         cb.setDebugEnabled(true)
            .setOAuthConsumerKey(consumerKey)
            .setOAuthConsumerSecret(consumerSecret)
            .setOAuthAccessToken(accessToken)
            .setOAuthAccessTokenSecret(accessTokenSecret);
					
         _twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
         _twitterStream.addListener(listener);
				
         if (keyWords.length == 0) {
            _twitterStream.sample();
         }else {
            FilterQuery query = new FilterQuery().track(keyWords);
            _twitterStream.filter(query);
         }
   }
			
   @Override
   public void nextTuple() {
      Status ret = queue.poll();
				
      if (ret == null) {
         Utils.sleep(50);
      } else {
         _collector.emit(new Values(ret));
      }
   }
			
   @Override
   public void close() {
      _twitterStream.shutdown();
   }
			
   @Override
   public Map<String, Object> getComponentConfiguration() {
      Config ret = new Config();
      ret.setMaxTaskParallelism(1);
      return ret;
   }
			
   @Override
   public void ack(Object id) {}
			
   @Override
   public void fail(Object id) {}
			
   @Override
   public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("tweet"));
   }
}
```

* HashtagReaderBolt

> The tweet emitted by spout will be forwarded to HashtagReaderBolt, which will process
> the tweet and emit all the available hashtags. HashtagReaderBolt uses
> getHashTagEntities method provided by twitter4j. getHashTagEntities reads the tweet
> and returns the list of hashtag. The complete program code is as follows 

```java
import java.util.HashMap;
import java.util.Map;

import twitter4j.*;
import twitter4j.conf.*;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

public class HashtagReaderBolt implements IRichBolt {
   private OutputCollector collector;

   @Override
   public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
      this.collector = collector;
   }

   @Override
   public void execute(Tuple tuple) {
      Status tweet = (Status) tuple.getValueByField("tweet");
      for(HashtagEntity hashtage : tweet.getHashtagEntities()) {
         System.out.println("Hashtag: " + hashtage.getText());
         this.collector.emit(new Values(hashtage.getText()));
      }
   }

   @Override
   public void cleanup() {}

   @Override
   public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("hashtag"));
   }
	
   @Override
   public Map<String, Object> getComponentConfiguration() {
      return null;
   }
	
}
```

* HashtagCounterBolt

> The emitted hashtag will be forwarded to HashtagCounterBolt. This bolt will process
> all the hashtags and save each and every hashtag and its count in memory using Java
> Map object. The complete program code is given below.

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

public class HashtagCounterBolt implements IRichBolt {
   Map<String, Integer> counterMap;
   private OutputCollector collector;

   @Override
   public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
      this.counterMap = new HashMap<String, Integer>();
      this.collector = collector;
   }

   @Override
   public void execute(Tuple tuple) {
      String key = tuple.getString(0);

      if(!counterMap.containsKey(key)){
         counterMap.put(key, 1);
      }else{
         Integer c = counterMap.get(key) + 1;
         counterMap.put(key, c);
      }
		
      collector.ack(tuple);
   }

   @Override
   public void cleanup() {
      for(Map.Entry<String, Integer> entry:counterMap.entrySet()){
         System.out.println("Result: " + entry.getKey()+" : " + entry.getValue());
      }
   }

   @Override
   public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("hashtag"));
   }
	
   @Override
   public Map<String, Object> getComponentConfiguration() {
      return null;
   }
	
}
```

* TwitterHashtagStorm

> Submitting a topology is the main application. Twitter topology consists of 
> TwitterSampleSpout, HashtagReaderBolt, and HashtagCounterBolt. The following program
> code shows how to submit a topology.

```java
import java.util.*;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;

public class TwitterHashtagStorm {
   public static void main(String[] args) throws Exception{
      String consumerKey = args[0];
      String consumerSecret = args[1];
		
      String accessToken = args[2];
      String accessTokenSecret = args[3];
		
      String[] arguments = args.clone();
      String[] keyWords = Arrays.copyOfRange(arguments, 4, arguments.length);
		
      Config config = new Config();
      config.setDebug(true);
		
      TopologyBuilder builder = new TopologyBuilder();
      builder.setSpout("twitter-spout", new TwitterSampleSpout(consumerKey,
         consumerSecret, accessToken, accessTokenSecret, keyWords));

      builder.setBolt("twitter-hashtag-reader-bolt", new HashtagReaderBolt())
         .shuffleGrouping("twitter-spout");

      builder.setBolt("twitter-hashtag-counter-bolt", new HashtagCounterBolt())
         .fieldsGrouping("twitter-hashtag-reader-bolt", new Fields("hashtag"));
			
      LocalCluster cluster = new LocalCluster();
      cluster.submitTopology("TwitterHashtagStorm", config,
         builder.createTopology());
      Thread.sleep(10000);
      cluster.shutdown();
   }
}
```