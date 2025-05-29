package com.lgcns.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.lgcns.kafka.message.ItemPurchasedMessage;
import com.lgcns.kafka.producer.ItemPurchasedProducer;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "item-purchased-topic")
class EmbeddedKafkaIntegrationTest {

    @Autowired private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired private ItemPurchasedProducer itemPurchasedProducer;

    @Test
    void 상품_구매_메시지를_카프카에_발행한다() {
        // given
        List<ItemPurchasedMessage.Item> items =
                List.of(new ItemPurchasedMessage.Item(1L, 2), new ItemPurchasedMessage.Item(2L, 3));
        ItemPurchasedMessage message = new ItemPurchasedMessage(items);

        // when
        itemPurchasedProducer.sendMessage(message);

        // then
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.lgcns.kafka.message");

        try (KafkaConsumer<String, ItemPurchasedMessage> consumer =
                new KafkaConsumer<>(
                        consumerProps,
                        new StringDeserializer(),
                        new JsonDeserializer<>(ItemPurchasedMessage.class, false))) {
            consumer.subscribe(Collections.singletonList("item-purchased-topic"));

            ConsumerRecord<String, ItemPurchasedMessage> record =
                    KafkaTestUtils.getSingleRecord(
                            consumer, "item-purchased-topic", Duration.ofSeconds(5));

            ItemPurchasedMessage received = record.value();

            Assertions.assertAll(
                    () -> assertThat(received.items()).hasSize(2),
                    () -> assertThat(received.items()).extracting("itemId").containsExactly(1L, 2L),
                    () ->
                            assertThat(received.items())
                                    .extracting("quantity")
                                    .containsExactly(2, 3));
        }
    }
}
