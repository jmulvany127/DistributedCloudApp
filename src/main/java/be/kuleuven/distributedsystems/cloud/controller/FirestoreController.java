package be.kuleuven.distributedsystems.cloud.controller;

import static java.util.stream.Collectors.groupingBy;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.awt.print.Book;
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

    public Booking getBooking(String bookingId) {
        DocumentReference docRef = firestore.collection("bookingCollection").document(bookingId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                System.out.println("document data" + document.getData());
                List<Map<String, String>> ticketList = (List<Map<String, String>>) document.get("tickets");
                System.out.println(ticketList);

                UUID bookingRef = UUID.fromString((String) document.get("bookingId"));
                String timeString = (String) document.get("time");
                LocalDateTime time = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String customer = (String) document.get("customer");

                List<Ticket> tickets = new ArrayList<>();
                for (Map<String, String> ticket : ticketList) {
                    String ticketBookingRef = ticket.get("bookingRef");
                    String ticketCustomer = ticket.get("customer");
                    UUID seatId = UUID.fromString(ticket.get("seatId"));
                    UUID ticketId = UUID.fromString(ticket.get("ticketId"));
                    String trainCompany = ticket.get("trainCompany");
                    UUID trainId = UUID.fromString(ticket.get("trainId"));

                    Ticket newTicket = new Ticket(trainCompany, trainId, seatId, ticketId, ticketCustomer, ticketBookingRef);
                    tickets.add(newTicket);
                }

                return new Booking(bookingRef, time, tickets, customer);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException("Error when receiving booking");
    }

    public List<Booking> getAllBookings() {
        CollectionReference docRef = firestore.collection("bookingCollection");
        ApiFuture<QuerySnapshot> querySnapshot = docRef.get();

        try {
            List<Booking> bookings = new ArrayList<>();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()){
                System.out.println("LOOOOOP" + document);
                Booking newBooking = documentToBooking(document);
                bookings.add(newBooking);
            }
            return bookings;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Booking documentToBooking(DocumentSnapshot document) {
        List<Map<String, String>> ticketList = (List<Map<String, String>>) document.get("tickets");
        UUID bookingRef = UUID.fromString((String) document.get("bookingId"));
        String timeString = (String) document.get("time");
        LocalDateTime time = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String customer = (String) document.get("customer");

        List<Ticket> tickets = new ArrayList<>();
        for (Map<String, String> ticket : ticketList) {
            String ticketBookingRef = ticket.get("bookingRef");
            String ticketCustomer = ticket.get("customer");
            UUID seatId = UUID.fromString(ticket.get("seatId"));
            UUID ticketId = UUID.fromString(ticket.get("ticketId"));
            String trainCompany = ticket.get("trainCompany");
            UUID trainId = UUID.fromString(ticket.get("trainId"));

            Ticket newTicket = new Ticket(trainCompany, trainId, seatId, ticketId, ticketCustomer, ticketBookingRef);
            tickets.add(newTicket);
        }
        return new Booking(bookingRef, time, tickets, customer);
    }


}



