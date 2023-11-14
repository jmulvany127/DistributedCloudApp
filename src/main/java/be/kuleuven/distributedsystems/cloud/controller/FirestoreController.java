package be.kuleuven.distributedsystems.cloud.controller;

import static java.util.stream.Collectors.groupingBy;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.google.api.core.ApiFunction;
import com.google.auto.value.extension.serializable.SerializableAutoValue;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import be.kuleuven.distributedsystems.cloud.Application;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;


@Service
public class FirestoreController {
    private final Firestore firestore;

    @Autowired
    public FirestoreController(Firestore firestore) {
        this.firestore = firestore;
    }

    public DocumentReference getDocumentReference() {
        System.out.println(firestore.collection("users").document("bookings"));
        return firestore.collection("bookingsCol").document("bookingsDoc");
    }

    public void  addBooking(Booking booking) {
        DocumentReference docRef = getDocumentReference();

        UUID id = booking.getId();
        LocalDateTime time = booking.getTime();
        List<Ticket> tickets = booking.getTickets();
        String customer = booking.getCustomer();


        docRef.set(customer);
    }




}
