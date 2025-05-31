package com.lgcns.kafka.producer;

import com.lgcns.kafka.message.MemberEnteredMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberEnteredProducer {

    private static final String TOPIC = "member-entered-topic";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(MemberEnteredMessage message) {
        kafkaTemplate.send(TOPIC, message);
    }
}
