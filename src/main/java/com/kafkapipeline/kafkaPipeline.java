package com.kafkapipeline;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class kafkaPipeline {

    public static void main(String[] args) throws Exception {

        String bootstrap = System.getenv("KAFKA_BOOTSTRAP") != null ? System.getenv("KAFKA_BOOTSTRAP") : "localhost:9092";
        String consumer_topic = System.getenv("CONSUMER_TOPIC") != null ? System.getenv("CONSUMER_TOPIC") : "vehicle-raw-data";
        String producer_topic = System.getenv("PRODUCER_TOPIC") != null ? System.getenv("PRODUCER_TOPIC") : "vehicle-processed-data";
        String consumer_group = System.getenv("CONSUMER_GROUP") != null ? System.getenv("CONSUMER_GROUP") : "vehicle-monitoring-group";

        Properties cProps = new Properties();
        cProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, consumer_group);
        cProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        Thread consumerThread = new Thread(() -> {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(cProps)) {
                // Subscribe to the Consumer Topic
                consumer.subscribe(Collections.singletonList(consumer_topic));
                System.out.println("LOG: Consumer Active on [" + consumer_topic + "]. Waiting for input...");

                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                    if (!records.isEmpty()) {
                        for (ConsumerRecord<String, String> record : records) {
                            System.out.println("\n[KAFKA-RECEIVED] from " + consumer_topic + " -> " + record.value());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Consumer Error: " + e.getMessage());
            }
        });
        consumerThread.start();

        // 3. START PRODUCER (SENDS TO PRODUCER TOPIC)
        Properties pProps = new Properties();
        pProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        pProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        pProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        Thread.sleep(2000); // Wait for consumer connection

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(pProps)) {
            String jsonPayload = "{\"device_id\":\"CM_999\",\"speed\":80.0,\"status\":\"INITIAL_CONNECT\"}";

            System.out.println("[PRODUCER-SENT] to [" + producer_topic + "] -> " + jsonPayload);

            // Use the producer_topic variable here
            producer.send(new ProducerRecord<>(producer_topic, "VEHICLE_KEY", jsonPayload));
            producer.flush();
        }

        System.out.println("LOG: Handshake complete. Listening on " + consumer_topic + "...");
        consumerThread.join();
    }
}
