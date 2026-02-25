package com.kafkapipeline;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class kafkaPipeline {


    public static void main(String[] args) throws Exception {
        String bootstrap = System.getenv("KAFKA_BOOTSTRAP") != null ? System.getenv("KAFKA_BOOTSTRAP") : "localhost:9092";
        String topic = System.getenv("KAFKA_TOPIC") != null ? System.getenv("KAFKA_TOPIC") : "vehicle-data";
        String consumer_group = "test-group-" + System.currentTimeMillis();


        // 3. START BACKGROUND CONSUMER
        Properties cProps = new Properties();
        cProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG,consumer_group);
        cProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        Thread consumerThread = new Thread(() -> {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(cProps)) {
                consumer.subscribe(Collections.singletonList(topic));
                System.out.println("LOG: Consumer Active. SILENTLY waiting for terminal input...");
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                    if (!records.isEmpty()) {
                        for (ConsumerRecord<String, String> record : records) {
                            System.out.println("\n[KAFKA-RECEIVED] -> " + record.value());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Consumer Error: " + e.getMessage());
            }
        });
        consumerThread.start();

        // 4. START PRODUCER (SENDS ONE INITIAL TEST)
        Properties pProps = new Properties();
        pProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        pProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        pProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        Thread.sleep(2000); // Wait for consumer connection

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(pProps)) {
            String jsonPayload = "{\"device_id\":\"CM_999\",\"speed\":80.0,\"status\":\"INITIAL_CONNECT\"}";
            System.out.println("[PRODUCER-SENT] -> " + jsonPayload);
            producer.send(new ProducerRecord<>(topic, "VEHICLE_KEY", jsonPayload));
            producer.flush();
        }


        System.out.println("LOG: Handshake complete. Now waiting for your manual terminal JSON...");
        consumerThread.join();
    }
}
