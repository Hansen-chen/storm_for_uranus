import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.topology.TopologyBuilder;

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
