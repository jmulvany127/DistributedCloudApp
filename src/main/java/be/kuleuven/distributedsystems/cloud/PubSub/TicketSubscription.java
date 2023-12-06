package be.kuleuven.distributedsystems.cloud.PubSub;

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
    // file must be run everytime emulator is restarted to register the Topic
    public static void main(String... args) throws Exception {

        //define project, pub sub and host details
        String projectId = "fos-jm-cloud-app";
        String subscriptionId = "confirmBookingRequest-sub";
        String topicId = "confirmBookingRequest";
        String pushEndpoint = "https://fos-jm-cloud-app.ew.r.appspot.com/subscription";

        createPushSubscription(projectId, subscriptionId, topicId, pushEndpoint);
    }

    public static void createPushSubscription(String projectId, String subscriptionId, String topicId, String pushEndpoint)
            throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            //define subscription and topic names
            TopicName topicName = TopicName.of(projectId, topicId);
            SubscriptionName subscriptionName = SubscriptionName.of(projectId, subscriptionId);
            //set endpoint of subscription push, to URL where POST requests are handled by subscriber
            PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();

            // Create a push subscription with default acknowledgement deadline of 60 seconds.
            // Messages not successfully acknowledged within 60 seconds will get resent by the server.
            Subscription subscription =
                    subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 60);
            System.out.println("Created push subscription: " + subscription.getName());
        }
    }
}