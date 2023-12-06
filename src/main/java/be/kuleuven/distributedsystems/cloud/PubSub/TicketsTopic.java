package be.kuleuven.distributedsystems.cloud.PubSub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
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
        String projectId = "fos-jm-cloud-app";
        String topicId = "confirmBookingRequest";
        createTopic(projectId, topicId);
    }

    public static void createTopic(String projectId, String topicId) throws IOException {
        try (TopicAdminClient topicClient = TopicAdminClient.create()){
            //create topic
            TopicName topicName = TopicName.of(projectId,topicId);
            Topic topic  = topicClient.createTopic(topicName);
            System.out.println("Created topic: " +topic.getName());
        }
    }
}