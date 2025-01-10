package com.example.gcpcloudstreambug;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(classes = GcpCloudStreamBugApplication.class)
class GcpCloudStreamBugApplicationTests {

    @Autowired
    private PubSubTemplate pubSubTemplate;
    @Autowired
    private LogCaptor logCaptor;

    private static final GenericContainer<?> PUBSUB = new GenericContainer<>("gcr.io/google.com/cloudsdktool/cloud-sdk")
            .withExposedPorts(8085)
            .withReuse(true)
            .withCommand("gcloud", "beta", "emulators", "pubsub", "start", "--host-port=0.0.0.0:8085")
            .waitingFor(Wait.forLogMessage(".*Server started.*", 1));

    @DynamicPropertySource
    public static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("pubsub.emulator.port", () -> PUBSUB.getMappedPort(8085));
    }

    @BeforeAll
    public static void init() {
        PUBSUB.start();

        ManagedChannel channel = ManagedChannelBuilder.forTarget("[::1]:" + PUBSUB.getMappedPort(8085))
                .usePlaintext()
                .build();
        try {
            GrpcTransportChannel transportChannel = GrpcTransportChannel.create(channel);
            TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(transportChannel);
            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

            TopicName topicName = TopicName.of("local", "test");
            try (TopicAdminClient topicAdminClient = TopicAdminClient.create(
                    TopicAdminSettings.newBuilder()
                            .setTransportChannelProvider(channelProvider)
                            .setCredentialsProvider(credentialsProvider)
                            .build())) {

                Topic topic = topicAdminClient.createTopic(topicName);
                System.out.println("Created topic: " + topic.getName());
            }

            SubscriptionName subscriptionName = SubscriptionName.of("local", "test.gcp-cloud-stream-bug");
            try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(SubscriptionAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build())) {
                Subscription subscription = subscriptionAdminClient.createSubscription(subscriptionName, topicName, PushConfig.newBuilder().build(), 10);
                System.out.println("Created subscription: " + subscription.getName());
            }
        } catch (Exception exception) {
            log.error("There was an unexpected error", exception);
        } finally {
            channel.shutdown();
        }
    }

    @AfterAll
    public static void cleanup() {
        PUBSUB.stop();
    }

    @Test
    void test() {
        logCaptor.start();
        pubSubTemplate.publish("test", "a message");
        await().atMost(5, SECONDS)
                .untilAsserted(() -> assertThat(logCaptor.getLogEvents())
                        .filteredOn(logEvent -> logEvent.getFormattedMessage().equals("a message"))
                        .hasSize(1));
        logCaptor.stop();
    }
}
