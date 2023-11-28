package be.kuleuven.distributedsystems.cloud.controller;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileReader;

public class EuroStarInitController {
    private final Firestore firestore;

    @Autowired
    public EuroStarInitController(Firestore firestore) { this.firestore = firestore; }

    public void addTrainInfo() {
        DocumentReference docRef = firestore.collection("OurTrain").document();


    }


}
