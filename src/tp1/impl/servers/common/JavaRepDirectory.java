package tp1.impl.servers.common;


import tp1.api.service.java.Directory;
import tp1.api.service.java.DirectoryRep;
import tp1.impl.kafka.KafkaPublisher;
import tp1.impl.kafka.KafkaSubscriber;
import tp1.impl.kafka.examples.KafkaSender;
import java.util.List;


public class JavaRepDirectory extends JavaDirectory implements DirectoryRep {

    private static final String FROM_BEGINNING = "earliest";

    KafkaPublisher publisher;
    KafkaSubscriber subscriber;

    public JavaRepDirectory(){
        subscriber = KafkaSubscriber.createSubscriber(KafkaSender.KAFKA_BROKERS, List.of(KafkaSender.TOPIC),
                FROM_BEGINNING);

        publisher = KafkaPublisher.createPublisher(KafkaSender.KAFKA_BROKERS);

        subscriber.start(true, (r) -> {
            //DECODE

        });
    }

    public KafkaSubscriber getSubscriber(){
        return subscriber;
    }

    public KafkaPublisher getPublisher(){
        return publisher;
    }

    private void writeFile(){

    }



}
