package be.kuleuven.distributedsystems.cloud;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;

public class TicketSubscription {
    public static void main(String... args) throws Exception {

        String projectId = "demo-distributed-systems-kul";
        String subscriptionId = "pushTicketsSub";
        String topicId = "putTicketRequest";
        String pushEndpoint = "http://localhost:8080/subscription";
        String hostport = "localhost:8083";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
        createPushSubscriptionExample(projectId, subscriptionId, topicId, pushEndpoint, channel);
    }

    public static void createPushSubscriptionExample(
            String projectId, String subscriptionId, String topicId, String pushEndpoint, ManagedChannel channel)
            throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            TransportChannelProvider channelProvider =
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();


            TopicName topicName = TopicName.of(projectId, topicId);
            SubscriptionName subscriptionName = SubscriptionName.of(projectId, subscriptionId);
            PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();

            SubscriptionAdminClient subscriptionClient =
                    SubscriptionAdminClient.create(
                            SubscriptionAdminSettings.newBuilder()
                                    .setTransportChannelProvider(channelProvider)
                                    .setCredentialsProvider(credentialsProvider)
                                    .build());

            // Create a push subscription with default acknowledgement deadline of 10 seconds.
            // Messages not successfully acknowledged within 10 seconds will get resent by the server.
            Subscription subscription =
                    subscriptionClient.createSubscription(subscriptionName, topicName, pushConfig, 60);
            System.out.println("Created push subscription: " + subscription.getName());
        }
    }
}