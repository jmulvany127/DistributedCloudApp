package be.kuleuven.distributedsystems.cloud.PubSUb;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;

public class TicketsTopic {
    // file must be run everytime emulator is restarted to register the Topic
    public static void main(String... args) throws Exception {

        //define project, pub sub and host details
        String projectId = "demo-distributed-systems-kul";
        String topicId = "putTicketRequest";
        String hostport = "localhost:8083";

        //Ensures emulator on local host is used instead of actual cloud pub sub
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();

        createTopic(projectId, topicId, channel);
    }

    public static void createTopic(String projectId, String topicId, ManagedChannel channel) throws IOException {
        try {
            // Set the channel and credentials provider when creating a `TopicAdminClient`
            TransportChannelProvider channelProvider =
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();


            //create topic admin client
            TopicAdminClient topicClient =
                    TopicAdminClient.create(
                            TopicAdminSettings.newBuilder()
                                    .setTransportChannelProvider(channelProvider)
                                    .setCredentialsProvider(credentialsProvider)
                                    .build());
            //create topic
            TopicName topicName = TopicName.of(projectId,topicId);
            Topic topic  = topicClient.createTopic(topicName);
            System.out.println("Created topic: " +topic.getName());

        } finally {
            channel.shutdown();
        }
    }
}