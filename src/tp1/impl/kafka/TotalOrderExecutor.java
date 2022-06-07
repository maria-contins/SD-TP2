package tp1.impl.kafka;

import tp1.impl.kafka.sync.SyncPoint;
import tp1.impl.servers.common.JavaRepDirectory;

import java.util.List;

public class TotalOrderExecutor {
    static final String FROM_BEGINNING = "earliest";
    static final String TOPIC = "dir_operations";
    static final String KAFKA_BROKERS = "kafka:9092";

    final KafkaPublisher sender;
    final KafkaSubscriber receiver;
    //final SyncPoint<String> sync;

    public TotalOrderExecutor(JavaRepDirectory repDir){

        this.sender = KafkaPublisher.createPublisher(KAFKA_BROKERS);
        this.receiver = KafkaSubscriber.createSubscriber(KAFKA_BROKERS, List.of(TOPIC), FROM_BEGINNING);
        //this.sync = new SyncPoint<>();
        this.receiver.start(false, new RecordProcessorClass(repDir) {
        });
    }

    public void publish(String operation, String operationInfo){
        SyncPoint.getInstance().waitForResult(sender.publish(TOPIC,operation, operationInfo));
    }
}
