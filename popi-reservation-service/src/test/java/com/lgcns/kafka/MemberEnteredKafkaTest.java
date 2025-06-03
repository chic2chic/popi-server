package com.lgcns.kafka;

import com.lgcns.AbstractKafkaIntegrationTest;
import com.lgcns.dto.request.QrEntranceInfoRequest;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.kafka.message.MemberEnteredMessage;
import com.lgcns.kafka.producer.MemberEnteredProducer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = MemberEnteredKafkaTest.TOPIC)
public class MemberEnteredKafkaTest extends AbstractKafkaIntegrationTest {

    @Autowired private MemberEnteredProducer memberEnteredProducer;

    static final String TOPIC = "member-entered-topic";

    @Test
    void 방문자_입장_메세지를_카프카에_전송한다() {
        // given
        QrEntranceInfoRequest qrEntranceInfoRequest =
                new QrEntranceInfoRequest(
                        1L,
                        1L,
                        1L,
                        MemberAge.TWENTIES,
                        MemberGender.MALE,
                        LocalDate.now(),
                        LocalTime.now().truncatedTo(ChronoUnit.SECONDS));

        MemberEnteredMessage memberEnteredMessage =
                MemberEnteredMessage.from(qrEntranceInfoRequest);

        // when
        memberEnteredProducer.sendMessage(memberEnteredMessage);

        // then
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.lgcns.kafka.message");

        try (KafkaConsumer<String, MemberEnteredMessage> consumer =
                createConsumer(MemberEnteredMessage.class)) {
            consumer.subscribe(Collections.singletonList("member-entered-topic"));

            ConsumerRecord<String, MemberEnteredMessage> record =
                    KafkaTestUtils.getSingleRecord(consumer, "member-entered-topic");

            MemberEnteredMessage received = record.value();

            Assertions.assertAll(
                    () ->
                            Assertions.assertEquals(
                                    memberEnteredMessage.popupId(), received.popupId()),
                    () -> Assertions.assertEquals(memberEnteredMessage.gender(), received.gender()),
                    () -> Assertions.assertEquals(memberEnteredMessage.age(), received.age()),
                    () ->
                            Assertions.assertEquals(
                                    memberEnteredMessage.reservationDate(),
                                    received.reservationDate()),
                    () ->
                            Assertions.assertEquals(
                                    memberEnteredMessage.reservationTime(),
                                    received.reservationTime()));
        }
    }
}
