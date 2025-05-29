package com.lgcns.kafka.producer;

import com.lgcns.kafka.message.ItemPurchasedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemPurchasedProducer {

    private static final String TOPIC = "item-purchased-topic";
    private final KafkaTemplate<String, ItemPurchasedMessage> kafkaTemplate;

    public void sendMessage(ItemPurchasedMessage message) {
        kafkaTemplate.send(TOPIC, message);
    }
}
