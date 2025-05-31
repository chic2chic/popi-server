package com.lgcns.kafka.producer;

import com.lgcns.kafka.message.MemberAnswerMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberAnswerProducer {

    private static final String TOPIC = "member-answer-topic";
    ;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(MemberAnswerMessage message) {
        kafkaTemplate.send(TOPIC, message);
    }
}
