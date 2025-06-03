package com.lgcns;

import java.util.Map;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

public abstract class AbstractKafkaIntegrationTest {

    @Autowired protected EmbeddedKafkaBroker embeddedKafkaBroker;

    protected <T> KafkaConsumer<String, T> createConsumer(Class<T> messageClass) {
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);

        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.lgcns.kafka.message");

        return new KafkaConsumer<>(
                consumerProps, new StringDeserializer(), new JsonDeserializer<>(messageClass));
    }
}
