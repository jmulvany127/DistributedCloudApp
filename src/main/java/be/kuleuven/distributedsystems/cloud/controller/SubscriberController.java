package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.entities.*;
import java.util.*;
import java.time.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClientException;


//operates as local subscriber in emulated pub sub system
@RestController
public class SubscriberController {
    private final WebClient.Builder webClientBuilder;
    private final FirestoreController firestoreController;
    private final String TrainsKey = "key=JViZPgNadspVcHsMbDFrdGg0XXxyiE";

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

    public Ticket deleteTicket(Ticket ticket) {
        String url = "https://" + ticket.getTrainCompany() + "/trains/" + ticket.getTrainId() + "/seats/" + ticket.getSeatId() + "/ticket/"
                     + ticket.getTicketId() + "?" + TrainsKey;
        return webClientBuilder.baseUrl(url).build().delete().retrieve().bodyToMono(Ticket.class).block();
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
        try {
            for (String ticketUrl: ticketsUrlList) {
                Ticket ticket = webClientBuilder
                        .baseUrl(ticketUrl)
                        .build().put().retrieve()
                        .bodyToMono(Ticket.class)
                        .retry(9)
                        .block();
                tickets.add(ticket);
            }
            //create booking from received tickets under the corresponding userand add to temp local list
            Booking booking = new Booking(UUID.randomUUID().toString(), LocalDateTime.now().toString(), tickets, userEmail);
            firestoreController.addBooking(booking);
        } catch (WebClientException e) {
            //if the tickets are not available due to someone else bookings them, release the previously booked tickets
            for (Ticket ticket : tickets) {
                deleteTicket(ticket);
            }
        }
    }
}