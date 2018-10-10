package com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventHandler;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SimpleKafkaEventLog implements EventLog {

    private Properties producerProps;
    private Properties consumerProps;
    private Producer<String, String> producer;
    private Map<Topic, Consumer> consumers;

    public SimpleKafkaEventLog() {
        producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:9092");
        producerProps.put("acks", "all");
        producerProps.put("retries", 0);
        producerProps.put("batch.size", 16384);
        producerProps.put("linger.ms", 1);
        producerProps.put("buffer.memory", 33554432);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(producerProps);

        consumerProps = new Properties();


        consumers = new HashMap<>();
    }

    @Override
    public void publish(Topic topic, Event event) {
        producer.send(new ProducerRecord<>(topic.getName(),
                event.getRef().getReference(),
                event.toString()));
    }

    @Override
    public void subscribe(Topic topic, EventHandler handler) {
        /* TODO: publish() handled invoking EventHandlers in InProcessEventLog
            - need to handle concurrency here
         */


    }

    @Override
    public int getNumberOfSubscribers(Topic topic) {
        return 0;
    }

    @Override
    public List<Event> eventsBy(Topic topic) {
        return null;

        /* TODO: not sure it's possible to implement this method using
        Kafka Consumer API (or at all for that matter). Need to look ath
        Streams for this I think...
        */
    }
}
