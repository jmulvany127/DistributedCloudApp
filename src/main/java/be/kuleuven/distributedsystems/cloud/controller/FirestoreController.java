package be.kuleuven.distributedsystems.cloud.controller;

import static java.util.stream.Collectors.groupingBy;

import com.google.cloud.firestore.Firestore;


public class FirestoreController {
    private final Firestore firestore;

    public FirestoreController(Firestore firestore) {
        this.firestore = firestore;
    }

    


}
