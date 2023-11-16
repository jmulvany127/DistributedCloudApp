package be.kuleuven.distributedsystems.cloud.controller;


import be.kuleuven.distributedsystems.cloud.entities.*;
import be.kuleuven.distributedsystems.cloud.controller.*;

import java.util.*;
import java.time.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.stream.Collectors.groupingBy;

//operates as local subscriber in emulated pub sub system
@RestController
public class SubscriberController {
    //@RequestBody String body
    private final WebClient.Builder webClientBuilder;
    private final FirestoreController firestoreController;

    //to be stored on firestore
    private static final ArrayList<Booking> bookings = new ArrayList<>();

    @Autowired
    public SubscriberController(WebClient.Builder webClientBuilder, FirestoreController firestoreController) {
        this.webClientBuilder = webClientBuilder;
        this.firestoreController = firestoreController;
    }

    //receives a json String containing an embedded pub sub message
    @PostMapping ("/subscription")
    public ResponseEntity<?> subscriber(@RequestBody String body) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        String data = null;
        String rawticketsUrls = null;

        //parse json pubsub message to extract the tickets
        try {
            JsonNode rootNode = objectMapper.readTree(body);
            if (rootNode.has("message") && rootNode.get("message").has("data")) {
                JsonNode dataNode = rootNode.get("message").get("data");
                data = objectMapper.readValue(dataNode.toString(), String.class);
                byte[] decodedBytes = Base64.getDecoder().decode(data);
                rawticketsUrls= new String(decodedBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        createBooking(rawticketsUrls);
        String errorMessage = "Confirm Quote request received" ;
        return ResponseEntity.status(200).body(errorMessage);
    }

    public void createBooking(String rawTicketsUrls){
        //list of tickets to be turned into a booking
        List<Ticket> tickets = new ArrayList<>();

        // Format recieved string and extract user email and a list of ticketURLS from the raw URLs string
        String rawTicketsUrlsTrimmed = rawTicketsUrls.substring(1, rawTicketsUrls.length() - 1);
        String[] ticketsUrlArray = rawTicketsUrlsTrimmed .split(",\\s");
        List<String> ticketsUrlList =  new ArrayList<>(Arrays.asList(ticketsUrlArray));
        String userEmail = ticketsUrlList.get(ticketsUrlList.size()-1);
        ticketsUrlList.remove(ticketsUrlList.size()-1);

        //create a put request for every ticket URL and store the resulting ticket
        for (String ticketUrl: ticketsUrlList) {
            Ticket ticket = webClientBuilder
                    .baseUrl(ticketUrl)
                    .build().put().retrieve()
                    .bodyToMono(Ticket.class)
                    .retry(3)
                    .block();
            tickets.add(ticket);
        }
        
        //create booking from received tickets under the corresponding userand add to temp local list
        Booking booking = new Booking(UUID.randomUUID(), LocalDateTime.now(), tickets, userEmail);
        firestoreController.addBooking(booking);
    }

    //get bookings from booking list for a particular user
//    public static List<Booking>getBookings(String userEmail){
//
//        List<Booking> bookingList = new ArrayList<>();
//
//        //check for bookings of the current user, adding them to list to be returned
//        for (Booking booking : bookings) {
//            if (Objects.equals(booking.getCustomer(), userEmail)){
//                bookingList.add(booking);
//            }
//        }
//        System.out.println(bookingList);
//        return bookingList;
//
//    }
}






