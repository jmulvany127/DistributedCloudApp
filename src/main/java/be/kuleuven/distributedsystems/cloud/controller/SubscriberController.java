package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.entities.*;
import java.util.*;
import java.time.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
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
        String rawQuotes = null;

        //parse json pubsub message to extract the Quotes json data
        try {
            JsonNode rootNode = objectMapper.readTree(body);
            if (rootNode.has("message") && rootNode.get("message").has("data")) {
                JsonNode dataNode = rootNode.get("message").get("data");
                data = objectMapper.readValue(dataNode.toString(), String.class);
                byte[] decodedBytes = Base64.getDecoder().decode(data);
                rawQuotes= new String(decodedBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        createBooking(rawQuotes);
        String Message = "Confirm Quote request received" ;
        return ResponseEntity.status(200).body(Message);
    }

    public Ticket deleteTicket(Ticket ticket) {
        String url = "https://" + ticket.getTrainCompany() + "/trains/" + ticket.getTrainId() + "/seats/" + ticket.getSeatId() + "/ticket/"
                     + ticket.getTicketId() + "?" + TrainsKey;
        return webClientBuilder.baseUrl(url).build().delete().retrieve().bodyToMono(Ticket.class).block();
    }

    public void createBooking(String rawMessage){
        //list of tickets to be turned into a booking
        List<Ticket> tickets = new ArrayList<>();

        Gson gson = new Gson();

        // Convert the received message into an array of JSON strings
        List<String> jsonObjects = gson.fromJson(rawMessage, new ArrayList<String>().getClass());

        List<Quote> quotes = new ArrayList<>();
        String email = "";

        // Iterate through each JSON string and convert it to a Quote object
        for (int i = 0; i < jsonObjects.size(); i++) {
            if (i < jsonObjects.size() - 1) {
                Quote quote = gson.fromJson(jsonObjects.get(i), Quote.class);
                quotes.add(quote);
            } else {
                // Last element is the email
                email = jsonObjects.get(i);
            }
        }
        String bookingRef = UUID.randomUUID().toString();
        //create a put request for every quote and store the resulting ticket
        try {
            for (Quote quote: quotes) {
                //if quote for our train use firestore function
                if((quote.getTrainCompany()).equals("Eurostar London")){
                    tickets.add(firestoreController.bookTicket(quote, email, bookingRef));
                }
                //else create URL from quote data and use a put request to retrive the ticket
                else {
                    Ticket ticket = webClientBuilder
                            .baseUrl("https://" + quote.getTrainCompany() + "/trains/" + quote.getTrainId() + "/seats/" + quote.getSeatId() + "/ticket?customer=" + email + "&bookingReference=" +
                                    bookingRef + "&" + TrainsKey)
                            .build().put().retrieve()
                            .bodyToMono(Ticket.class)
                            .retry(9)
                            .block();
                    tickets.add(ticket);
                }
            }
            //create booking from received tickets under the corresponding user and add to firestore
            Booking booking = new Booking(UUID.randomUUID().toString(), LocalDateTime.now().toString(), tickets, email);
            firestoreController.addBooking(booking);
        } catch (WebClientException e) {
            //if the tickets are not available due to someone else bookings them, release the previously booked tickets
            for (Ticket ticket : tickets) {
                deleteTicket(ticket);
            }
        }
    }
}