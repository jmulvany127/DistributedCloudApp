package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.entities.*;

import java.io.IOException;
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
    private final String TrainsKey = "KEY_HIDDEN";
    String failSubject = "Train Booking failed";
    String failMessage = " Hello,\n Unfortunately your desired booking could not be completed as the requested seats became unavailable. Please revisit our website, https://fos-jm-cloud-app.ew.r.appspot.com, and attempt another booking. \n Apologies, \n The Book Trains Team .\n";
    String requestSubject = "Train Booking request";
    String requestMessage = "Hello,\n We have received your booking request and are securing your tickets. A confirmation of your booking will be sent shortly. \n Many thanks, \n The Book Trains Team ";
    String confirmationSubject = "Train Booking confirmed";
    String confirmationMessage = "Hello,\n Congratulations, we have secured your required tickets and you booking has been confirmed. Please revisit our website, https://fos-jm-cloud-app.ew.r.appspot.com/account, to view your booking and enjoy your trip! \n Many thanks, \n The Book Trains Team ";



    @Autowired
    public SubscriberController(WebClient.Builder webClientBuilder, FirestoreController firestoreController) {
        this.webClientBuilder = webClientBuilder;
        this.firestoreController = firestoreController;
    }

    //receives a json String containing an embedded pub sub message
    @PostMapping ("/subscription")
    public ResponseEntity<?> subscriber(@RequestBody String body) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String data = null;
        String rawQuotes = null;
        String userEmail = "";
        List<Quote> quotes = new ArrayList<>();

        //parse json pubsub message to extract the Quotes json data
        JsonNode rootNode = objectMapper.readTree(body);
        if (rootNode.has("message") && rootNode.get("message").has("data")) {
            JsonNode dataNode = rootNode.get("message").get("data");
            data = objectMapper.readValue(dataNode.toString(), String.class);
            byte[] decodedBytes = Base64.getDecoder().decode(data);
            rawQuotes= new String(decodedBytes);
        }
        Gson gson = new Gson();
        // Convert the received message into an array of JSON strings
        List<String> jsonObjects = gson.fromJson(rawQuotes, new ArrayList<String>().getClass());

        // Iterate through each JSON string and convert it to a Quote object
        for (int i = 0; i < jsonObjects.size(); i++) {
            if (i < jsonObjects.size() - 1) {
                Quote quote = gson.fromJson(jsonObjects.get(i), Quote.class);
                quotes.add(quote);
            } else {
                // Last element is the email
                userEmail = jsonObjects.get(i);
            }
        }
        //send User eail notifying them that there booking request has been received
        SendGridController.sendEmail( userEmail, requestSubject, requestMessage);
        //book the tickets
        createBooking(quotes, userEmail);
        String Message = "Confirm Quote request received" ;
        return ResponseEntity.status(200).body(Message);
    }

    //"unbook" the tickets when a rollback is needed
    public Ticket deleteTicket(Ticket ticket) {
        String url = "https://" + ticket.getTrainCompany() + "/trains/" + ticket.getTrainId() + "/seats/" + ticket.getSeatId() + "/ticket/"
                     + ticket.getTicketId() + "?" + TrainsKey;
        return webClientBuilder.baseUrl(url).build().delete().retrieve().bodyToMono(Ticket.class).block();
    }

    //take a list of quotes and a user email and make a booking
    public void createBooking(List<Quote> quotes, String userEmail) throws IOException {
        //list of tickets to be turned into a booking
        List<Ticket> tickets = new ArrayList<>();
        String bookingRef = UUID.randomUUID().toString();

        //create a put request for every quote and store the resulting ticket
        try {
            for (Quote quote: quotes) {
                //if quote for our train use firestore function
                if((quote.getTrainCompany()).equals("Eurostar London")){
                    // check tickets and see if ticket is already booked by the same user if so, don't book it again to ensure
                    // fault tolerance for crashes during booking process. also ensure that the original bookingid from before the crash is used
                    String previousBookRef = firestoreController.checkTicket(quote, userEmail);
                    if (previousBookRef == null) {
                        tickets.add(firestoreController.bookTicket(quote,userEmail,bookingRef));
                    } else {
                        bookingRef = previousBookRef;
                        // MUST ADD TO TICKETS LIST AGAIN
                    }
                } else {
                    Ticket ticket = webClientBuilder
                            .baseUrl("https://" + quote.getTrainCompany() + "/trains/" + quote.getTrainId() + "/seats/" + quote.getSeatId() + "/ticket?customer=" + userEmail + "&bookingReference=" +
                                    bookingRef + "&" + TrainsKey)
                            .build().put().retrieve()
                            .bodyToMono(Ticket.class)
                            .retry(9)
                            .block();
                    tickets.add(ticket);
                }
            }
            //create booking from received tickets under the corresponding user and add to firestore
            Booking booking = new Booking(UUID.randomUUID().toString(), LocalDateTime.now().toString(), tickets, userEmail);
            firestoreController.addBooking(booking);
            //send user email confirming there booking has been made
            SendGridController.sendEmail( userEmail, confirmationSubject, confirmationMessage);
        } catch (RuntimeException e) {
            SendGridController.sendEmail( userEmail, failSubject, failMessage);
            //if the tickets are not available due to someone else bookings them, release the previously booked tickets
            for (Ticket ticket : tickets) {
                if (Objects.equals(ticket.getTrainCompany(), "Eurostar London")) {
                    firestoreController.deleteTicket(ticket);
                } else {
                    deleteTicket(ticket);
                }
            }
        }
    }
}