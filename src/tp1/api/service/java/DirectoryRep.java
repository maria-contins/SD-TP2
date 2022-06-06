package tp1.api.service.java;

import tp1.impl.kafka.KafkaPublisher;
import tp1.impl.kafka.KafkaSubscriber;

public interface DirectoryRep extends Directory{

    KafkaSubscriber getSubscriber();
    KafkaPublisher getPublisher();
}
