package com.lgcns.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lgcns.dto.request.SurveyChoiceRequest;
import com.lgcns.kafka.message.MemberAnswerMessage;
import com.lgcns.kafka.message.dto.SurveyChoiceDto;
import com.lgcns.kafka.producer.MemberAnswerProducer;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = MemberAnswerKafkaTest.TOPIC)
public class MemberAnswerKafkaTest extends AbstractKafkaIntegrationTest {

    @Autowired private MemberAnswerProducer memberAnswerProducer;

    static final String TOPIC = "member-answer-topic";

    @Test
    void 설문_응답_메시지를_정상적으로_카프카에_전송한다() {
        // given
        List<SurveyChoiceRequest> request =
                List.of(
                        new SurveyChoiceRequest(1L, 1L),
                        new SurveyChoiceRequest(2L, 5L),
                        new SurveyChoiceRequest(3L, 9L),
                        new SurveyChoiceRequest(4L, 13L));

        MemberAnswerMessage message = MemberAnswerMessage.of(1L, request);

        // when
        memberAnswerProducer.sendMessage(message);

        // then
        try (KafkaConsumer<String, MemberAnswerMessage> consumer =
                createConsumer(MemberAnswerMessage.class)) {

            consumer.subscribe(Collections.singletonList(TOPIC));

            ConsumerRecord<String, MemberAnswerMessage> record =
                    KafkaTestUtils.getSingleRecord(consumer, TOPIC);

            MemberAnswerMessage received = record.value();

            Assertions.assertAll(
                    () -> assertEquals(message.memberId(), received.memberId()),
                    () ->
                            assertEquals(
                                    message.surveyChoices().size(),
                                    received.surveyChoices().size()),
                    () -> {
                        for (int i = 0; i < message.surveyChoices().size(); i++) {
                            SurveyChoiceDto expected = message.surveyChoices().get(i);
                            SurveyChoiceDto actual = received.surveyChoices().get(i);

                            assertEquals(expected.surveyId(), actual.surveyId());
                            assertEquals(expected.choiceId(), actual.choiceId());
                        }
                    });
        }
    }
}
