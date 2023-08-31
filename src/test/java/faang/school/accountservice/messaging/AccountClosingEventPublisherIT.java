package faang.school.accountservice.messaging;

import faang.school.accountservice.dto.account.AccountClosingEvent;
import faang.school.accountservice.dto.account.AccountFreezingEvent;
import faang.school.accountservice.messaging.account.AccountClosingEventPublisher;
import faang.school.accountservice.model.Account;
import faang.school.accountservice.model.AccountStatus;
import faang.school.accountservice.model.AccountType;
import faang.school.accountservice.model.Currency;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.Map;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {"account-close"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        })
class AccountClosingEventPublisherIT {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private AccountClosingEventPublisher accountClosingEventPublisher;

    @Value("${spring.kafka.topics.account-close}")
    private String TOPIC;

    @Test
    void send_ShouldSendToBroker() {
        Consumer<String, AccountClosingEvent> consumerServiceTest = createConsumer(TOPIC);

        accountClosingEventPublisher.send(mockAccount());

        ConsumerRecord<String, AccountClosingEvent> record =
                KafkaTestUtils.getSingleRecord(consumerServiceTest, TOPIC);

        AccountClosingEvent received = record.value();

        Assertions.assertEquals(mockAccountClosingEvent(), received);
    }

    private Consumer<String, AccountClosingEvent> createConsumer(final String topicName) {
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("group_consumer_test", "false", embeddedKafkaBroker);

        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Consumer<String, AccountClosingEvent> consumer =
                new DefaultKafkaConsumerFactory<>(consumerProps,
                        new StringDeserializer(),
                        new JsonDeserializer<>(AccountClosingEvent.class, false))
                        .createConsumer();

        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, topicName);

        return consumer;
    }

    private AccountClosingEvent mockAccountClosingEvent() {
        return AccountClosingEvent.builder()
                .userId(1L)
                .number("1234534654733564")
                .build();
    }

    private Account mockAccount() {
        return Account.builder()
                .id(1L)
                .number("1234534654733564")
                .userId(1L)
                .type(AccountType.CURRENT_ACCOUNT)
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .version(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }
}