package be.kuleuven.distributedsystems.cloud.controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.util.concurrent.ExecutionException;

public class EuroStarInitController {
    private final Firestore firestore;

    @Autowired
    public EuroStarInitController(Firestore firestore) { this.firestore = firestore; }

    private void addTrainInfo() {
        DocumentReference docRef = firestore.collection("OurTrain").document();

        if (!dataInitialised()) {

            //add data
        }

    }

    private boolean dataInitialised() {
        CollectionReference collectionReference = firestore.collection("OurTrain");
        ApiFuture<DocumentSnapshot> future = collectionReference.document("train").get();

        try {
            return !future.get().exists();
        } catch (ExecutionException | InterruptedException e) {
            return true;
        }
    }

}
