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
        DocumentReference docRef = firestore.collection("bookingCollection").document(booking.getId().toString());
        //extract fields from booking
        UUID id = booking.getId();
        LocalDateTime time = booking.getTime();
        List<Ticket> tickets = booking.getTickets();
        String customer = booking.getCustomer();

        //create tickets with fields as strings so they can be stored in db
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

        //create data object with fields of the booking to store in db
        Map<String, Object> docData = new HashMap<>();
        docData.put("bookingId", id.toString());
        docData.put("customer", customer);
        docData.put("time", time.toString());
        docData.put("tickets", ticketAsStrings);
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
                List<Map<String, String>> ticketList = (List<Map<String, String>>) document.get("tickets");
                UUID bookingRef = UUID.fromString((String) document.get("bookingId"));
                String timeString = (String) document.get("time");
                LocalDateTime time = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String customer = (String) document.get("customer");

                //list to store tickets from the db
                List<Ticket> tickets = new ArrayList<>();
                //extract ticket data from document
                for (Map<String, String> ticket : ticketList) {
                    String ticketBookingRef = ticket.get("bookingRef");
                    String ticketCustomer = ticket.get("customer");
                    UUID seatId = UUID.fromString(ticket.get("seatId"));
                    UUID ticketId = UUID.fromString(ticket.get("ticketId"));
                    String trainCompany = ticket.get("trainCompany");
                    UUID trainId = UUID.fromString(ticket.get("trainId"));
                    //create local version of the ticket with extracted data and add to the list
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
            //list to hold local bookings
            List<Booking> bookings = new ArrayList<>();
            //for each document create a local booking to be returned
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()){
                Booking newBooking = documentToBooking(document);
                bookings.add(newBooking);
            }
            return bookings;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Booking documentToBooking(DocumentSnapshot document) {
        //extract data from the fields of the document
        List<Map<String, String>> ticketList = (List<Map<String, String>>) document.get("tickets");
        UUID bookingRef = UUID.fromString((String) document.get("bookingId"));
        String timeString = (String) document.get("time");
        LocalDateTime time = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String customer = (String) document.get("customer");

        //list to store tickets from the db
        List<Ticket> tickets = new ArrayList<>();
        //extract ticket data from document
        for (Map<String, String> ticket : ticketList) {
            String ticketBookingRef = ticket.get("bookingRef");
            String ticketCustomer = ticket.get("customer");
            UUID seatId = UUID.fromString(ticket.get("seatId"));
            UUID ticketId = UUID.fromString(ticket.get("ticketId"));
            String trainCompany = ticket.get("trainCompany");
            UUID trainId = UUID.fromString(ticket.get("trainId"));
            //create local ticket with extracted data and add to the ticket list
            Ticket newTicket = new Ticket(trainCompany, trainId, seatId, ticketId, ticketCustomer, ticketBookingRef);
            tickets.add(newTicket);
        }
        return new Booking(bookingRef, time, tickets, customer);
    }
}