
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import com.informatica.vds.api.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import org.slf4j.Logger;

public class KafkaSource implements VDSSource {

    private static final Logger logger = LoggerFactory.getLogger(KafkaSource.class);

    private static Properties props = new Properties();
//    private static Consumer<Integer, String> consumer;

    private static ConsumerConnector consumer;

    // UI json costants
    public static final String KAFKA_DESTINATION = "kafkaDestination";
    public static final String TOPIC = "topic";
    public static final String ZKSERVER = "zkServer";
    public static final String GROUPID = "groupId";
    public static final String AUTOCOMMIT = "autoCommit";
    public static final String COMMITINTERVAL = "commitInterval";
    public static final String SESSIONTIMEOUT = "sessionTimeout";
    public static final String KEYDESERIALIZER = "sessionTimeout";
    public static final String VALUEDESERIALIZER = "sessionTimeout";

    /**
     * determines whether to retry in case of open and write failure
     */
    protected IPluginRetryPolicy pluginRetryPolicyHandler;

    private static String kafkaDestination;
    private static String topic;
    private static String zkServer;
    private static String groupId;
    private static String autoCommit = "true";
    private static String commitInterval = "1000";
    private static String sessionTimeout = "30000";
    private static String keyDeserializer = "key.deserializer";
    private static String valueDeserializer = "value.deserializer";

    private Map<String, Integer> topicCountMap;
    private Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap;
    private KafkaStream<byte[], byte[]> stream;
    private ConsumerIterator<byte[], byte[]> it;
    private ByteBuffer byteMessage;
    
//    private ConsumerIterator<byte[], byte[]> it;
    private HashMap<String, String> hashMap;

    @Override
    public void open(VDSConfiguration vdsc) throws Exception {
        initConfig(vdsc);

        ConsumerConfig consumerConfig = new ConsumerConfig(props);
        consumer = Consumer.createJavaConsumerConnector(consumerConfig);

        hashMap = new HashMap<String, String>();
        hashMap.put(TOPIC, topic);
        initRead();
    }

    void initConfig(VDSConfiguration vdsc) throws VDSException, Exception {
        // get the configurations
        try {
            kafkaDestination = vdsc.getString(KAFKA_DESTINATION).trim();
            topic = vdsc.getString(TOPIC).trim();
            zkServer = vdsc.getString(ZKSERVER).trim();
            groupId = vdsc.getString(GROUPID).trim();
            autoCommit = vdsc.getString(AUTOCOMMIT).trim();
            commitInterval = vdsc.getString(COMMITINTERVAL).trim();
            sessionTimeout = vdsc.getString(SESSIONTIMEOUT).trim();
            keyDeserializer = vdsc.getString(KEYDESERIALIZER).trim();
            valueDeserializer = vdsc.getString(VALUEDESERIALIZER).trim();

        } catch (Exception e) {
            throw new Exception("Must provide valid Kafka Desitnation: "
                    + kafkaDestination);
        }
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("metadata.broker.list", kafkaDestination);

        // why no ZK?
        props.put("zookeeper.connect", zkServer);
        //     props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", groupId);
        props.put("enable.auto.commit", autoCommit);
        props.put("auto.commit.interval.ms", commitInterval);
        props.put("session.timeout.ms", sessionTimeout);
        props.put("key.deserializer", keyDeserializer);
        props.put("value.deserializer", valueDeserializer);
    }

    void initRead() {
        topicCountMap = new HashMap<>();
        topicCountMap.put(topic, 1);

        consumerMap = consumer.createMessageStreams(topicCountMap);
        stream = consumerMap.get(topic).get(0);

        it = stream.iterator();

    }

    @Override
    public void read(VDSEventList outbound) throws Exception {
        logger.info("Reading events from topic: " + topic);
        int count = 0;

        while (it.hasNext()) {

        	byteMessage = ByteBuffer.wrap(it.next().message());
            logger.info("Got message: " + Arrays.toString(byteMessage.array()));
            outbound.addEvent(byteMessage.array(), byteMessage.array().length);
            count++;
            break;

        }
        logger.info(String.format("Sent %d events", count));
    }

    /*
     @Override
     public void write(VDSEvent inputEvent) throws Exception {
     ByteBuffer src = inputEvent.getBuffer();
     int srcLength = inputEvent.getBufferLen();
     byte[] srcByte = new byte[srcLength];

     if (srcLength == 0) {
     logger.debug("Received data of size {}, returning without writing to Kafka", srcLength);
     return;
     } else {
     src.get(srcByte);
     // write to Kafka here
     String messageStr = new String(srcByte);
     producer.send(new KeyedMessage<Integer, String>(topic, messageStr));
     }
     }
     */
    @Override
    public void setRetryPolicyHandler(IPluginRetryPolicy iPluginRetryPolicyHandler) {
        this.pluginRetryPolicyHandler = iPluginRetryPolicyHandler;
        this.pluginRetryPolicyHandler.setLogger(logger);
    }

    @Override
    public void close() throws IOException {
        consumer.shutdown();
    }

}
