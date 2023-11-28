package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;


@Service
public class FirestoreController {
    private final Firestore firestore;

    @Autowired
    public FirestoreController(Firestore firestore) {
        this.firestore = firestore;
    }

    public void addBooking(Booking booking) {
        DocumentReference docRef = firestore.collection("bookingCollection").document(booking.getId());
        //extract fields from booking
        String id = booking.getId();
        String time = booking.getTime();
        List<Ticket> tickets = booking.getTickets();
        String customer = booking.getCustomer();

        //create data object with fields of the booking to store in db
        Map<String, Object> docData = new HashMap<>();
        docData.put("bookingId", id);
        docData.put("customer", customer);
        docData.put("time", time);
        docData.put("tickets", tickets);
        //store in db
        docRef.set(docData);
    }

    public Booking getBooking(String bookingId) {
        DocumentReference docRef = firestore.collection("bookingCollection").document(bookingId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            //get document from the db
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                //extract data from different fields of the document
                List<Ticket> ticketList = (List<Ticket>) document.get("tickets");
                String bookingRef = (String) document.get("bookingId");
                String timeString = (String) document.get("time");
                String time = String.valueOf(LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                String customer = (String) document.get("customer");

                return new Booking(bookingRef, time, ticketList, customer);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Error when receiving booking");
    }

    public List<Booking> getAllBookings() {
        CollectionReference docRef = firestore.collection("bookingCollection");
        ApiFuture<QuerySnapshot> querySnapshot = docRef.get();

        System.out.println(docRef);

        try {
            //list to hold local bookings
            List<Booking> bookings = new ArrayList<>();
            //for each document create a local booking to be returned
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()){
                Booking newBooking = document.toObject(Booking.class);
                bookings.add(newBooking);
            }
            return bookings;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}