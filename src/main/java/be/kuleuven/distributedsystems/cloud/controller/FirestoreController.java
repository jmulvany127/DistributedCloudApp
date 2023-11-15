package be.kuleuven.distributedsystems.cloud.controller;

import static java.util.stream.Collectors.groupingBy;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;


@Service
public class FirestoreController {
    private final Firestore firestore;

    @Autowired
    public FirestoreController(Firestore firestore) {
        this.firestore = firestore;
    }

    public DocumentReference getDocumentReference() {
        return firestore.collection("bookingsCol").document("bookingsDoc");
    }

    public void  addBooking(Booking booking) {
        DocumentReference docRef = firestore.collection("bookingCollection").document(booking.getId().toString());

        UUID id = booking.getId();
        LocalDateTime time = booking.getTime();
        List<Ticket> tickets = booking.getTickets();
        String customer = booking.getCustomer();

        //convert all the ticket data to string for storage in db
        List<Map<String, String>> ticketAsStrings = new ArrayList<>();
        for (Ticket ticket : tickets) {
            Map<String, String> ticketData = new HashMap<>();
            ticketData.put("seatId", ticket.getSeatId().toString());
            ticketData.put("ticketId", ticket.getTicketId().toString());
            ticketData.put("customer", ticket.getCustomer());
            ticketData.put("trainCompany", ticket.getTrainCompany());
            ticketData.put("bookingRef", ticket.getBookingReference());
            ticketData.put("trainId", ticket.getTrainId().toString());
            ticketAsStrings.add(ticketData);
        }

        Map<String, Object> docData = new HashMap<>();
        docData.put("bookingId", id.toString());
        docData.put("customer", customer);
        docData.put("time", time.toString());
        docData.put("tickets", ticketAsStrings);



        docRef.set(docData);
    }

    public void getBooking(String bookingId) {
        DocumentReference docRef = firestore.collection("bookingCollection").document(bookingId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                System.out.println("document data" + document.getData());

            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        System.out.println(bookingId);
        System.out.println(docRef);





    }


}



