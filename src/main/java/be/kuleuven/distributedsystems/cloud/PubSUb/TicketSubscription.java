package be.kuleuven.distributedsystems.cloud.PubSUb;

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
        String projectId = "demo-distributed-systems-kul";
        String subscriptionId = "pushTicketsSub";
        String topicId = "putTicketRequest";
        String pushEndpoint = "http://localhost:8080/subscription";
        String hostport = "localhost:8083";

        //Ensures emulator on local host is used instead of actual cloud pub sub
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();

        createPushSubscription(projectId, subscriptionId, topicId, pushEndpoint, channel);
    }

    public static void createPushSubscription(
            String projectId, String subscriptionId, String topicId, String pushEndpoint, ManagedChannel channel)
            throws IOException {
                    // Set the channel and credentials provider when creating a `SubscriptionAdminClient
                    TransportChannelProvider channelProvider =
                            FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
                    CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

                    //define subscription and topic names
                    TopicName topicName = TopicName.of(projectId, topicId);
                    SubscriptionName subscriptionName = SubscriptionName.of(projectId, subscriptionId);
                    //set endpoint of subscription push, to URL where POST requests are handled by subscriber
                    PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();

                    //create subscription admin client
                    SubscriptionAdminClient subscriptionClient =
                            SubscriptionAdminClient.create(
                                    SubscriptionAdminSettings.newBuilder()
                                            .setTransportChannelProvider(channelProvider)
                                            .setCredentialsProvider(credentialsProvider)
                                            .build());

                    // Create a push subscription with default acknowledgement deadline of 60 seconds.
                    // Messages not successfully acknowledged within 60 seconds will get resent by the server.
                    Subscription subscription =
                            subscriptionClient.createSubscription(subscriptionName, topicName, pushConfig, 60);
                    System.out.println("Created push subscription: " + subscription.getName());
            }
}