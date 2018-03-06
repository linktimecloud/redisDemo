package cloud.linktime.demo.kafka;

import cloud.linktime.demo.common.Constant;
import cloud.linktime.demo.common.FindServer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.zookeeper.KeeperException;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Created by tim on 2018/2/23.
 */
public class WordStreamRunnable implements Runnable {

    public void count(String applicationId, String servers, String sourceTopic) throws Exception {
        String brokerServer = FindServer.getKafkaBroker(servers);
        String redisServer = FindServer.getRedisAddress(brokerServer);
        String[] redisServers = redisServer.split(":");

        Jedis jedis = new Jedis(redisServers[0], Integer.valueOf(redisServers[1]));

        final StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> textLines = builder.stream(sourceTopic);

        KTable<String, Long> wordCounts = textLines
                .flatMapValues(textLine -> Arrays.asList(textLine.toLowerCase().split("\\W+")))
                .groupBy((key, word) -> word)
                .count("Counts");

        Map<String,String> wordMap = jedis.hgetAll(Constant.WORD_KEY);

        KStream<String, Long> outputStream = wordCounts.toStream().map(new KeyValueMapper<String, Long, KeyValue<? extends String, ? extends Long>>() {
            @Override
            public KeyValue<String, Long> apply(String key, Long value) {
                wordMap.put(key, String.valueOf(value));
                jedis.hmset(Constant.WORD_KEY, wordMap);
                return new KeyValue<>(key, value);
            }
        });

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, brokerServer);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3);

        Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);
        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java -jar  xxx.jar application_id servers source_topic");
            System.out.println("application_id: An identifier for the stream processing application. Must be unique within the Kafka cluster. It is used as 1) the default client-id prefix, 2) the group-id for membership management, 3) the changelog topic prefix." );
            System.out.println("such as java -jar  xxx.jar app1 9.90.92.1:9092,9.90.92.2:9092,9.90.92.3:9092 kafka-streams-wordcount-input");
            System.exit(1);
        }
        String applicationId = args[0];
        String servers = args[1];
        String sourceTopic = args[2];
        System.out.println("Start Kafka Streams program");

        new WordStreamRunnable().count(applicationId, servers, sourceTopic);
    }

    @Override
    public void run() {
        try {
            count("kafka-streams-wordcount", Constant.MASTER_MESOS + ":2181", "test");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
