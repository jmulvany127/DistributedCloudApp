package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.entities.*;

import java.awt.print.Book;
import java.net.http.HttpResponse;
import java.util.*;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.sendgrid.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.threeten.bp.LocalTime;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static be.kuleuven.distributedsystems.cloud.auth.SecurityFilter.getUser;

@RestController
//RequestMapping("api/")
public class TrainsController {

    private final WebClient.Builder webClientBuilder;
    public final ObjectMapper objectMapper;
    private final String ReliableTrainCompany = "https://reliabletrains.com/?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE";
    private final String ReliableTrains = "https://reliabletrains.com/trains?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE";
    private final String TrainsKey = "key=JViZPgNadspVcHsMbDFrdGg0XXxyiE";

    //key = traincompany name, value = link to their trains, for managing new train companies
    private static final Map<String, String> trainCompanies = new HashMap<>();
    private static final ArrayList<Booking> bookings = new ArrayList<>();

    public TrainsController(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;

        String ReliableTrainCompany = "reliabletrains.com";
        trainCompanies.put(ReliableTrainCompany, ReliableTrains);
    }

    //returns all the json data from a given URL
    public String getjson(String URL) {
        WebClient webClient = webClientBuilder.baseUrl(URL).build();

        //retrieves the jsondataas a string, note as the string is built the thread is blocked
        String jsonData = webClient.get().retrieve().bodyToMono(String.class).block();

        return jsonData;
    }

    //gets all trains, only reliabletraincompany for now
    @GetMapping("api/getTrains")
    public ResponseEntity<List<Train>> getallTrains() {

        // get json data from the baseurl
        String jsonData = getjson(ReliableTrains);

        // Extract train objects from json data
        List<Train> allTrains = TrainFunctions.extractTrains(jsonData);

        return ResponseEntity.ok(allTrains);
    }

    //return a single train by its ID, if not found return 404 error
    @GetMapping("api/getTrain")
    public ResponseEntity<?> getTrain(String trainCompany, String trainId) {
        //gets the traincompany/trains URL form hashmap, then use to get json data
        String trainsURL = trainCompanies.get(trainCompany);
        String jsonData = getjson(trainsURL);

        //returns the train if found in jsondata, if not returns an empty optional
        Optional<Train> train = TrainFunctions.getTrainByID( trainId, jsonData);

        //checks if train found, if not give error
        if (train.isPresent()) {
            return ResponseEntity.ok(train); // HTTP 200 with the train as the response body
        }else {
            String errorMessage = "Train not found" ;
            return ResponseEntity.status(404).body(errorMessage); // HTTP 404 with the error message
        }
    }

    //return a string list of a specific trains times , if train not found return 404 error
    @GetMapping("api/getTrainTimes")
    public ResponseEntity<?> getTrainTimes(String trainCompany, String trainId) {
        //gets the traincompany/trains URL form hashmap, then use to get json data
        String trainsURL = trainCompanies.get(trainCompany);
        String jsonData = getjson(trainsURL);
        //get the train object by ID
        Optional<Train> train = TrainFunctions.getTrainByID(trainId, jsonData);

        //checks if train found, if not give error
        if (train.isPresent()) {
            //build the URL to acess times, then get raw json data
            String timesURL = "https://" + trainCompany + "/trains/" + trainId +"/times?" + TrainsKey;
            String timesJsonData = getjson(timesURL);
            //get the list of times from the raw json data
            List<String> trainTimes = TrainFunctions.extractTrainTimes(timesJsonData);
            return ResponseEntity.ok(trainTimes);

        } else {
            String errorMessage = "Train not found";
            return ResponseEntity.status(404).body(errorMessage); // HTTP 404 with the error message
        }
    }

    //gets all available seats, divides them by class and sorts by order and number
    @GetMapping("api/getAvailableSeats")
    public ResponseEntity<List<List<Seat>>> getAvailableSeats(String trainCompany, String trainId, String time) {
        //build the URL to acess seats, then get raw json data
        String seatsURL = "https://"+trainCompany+"/trains/"+trainId+"/seats?time="+time+"&available=true&"+TrainsKey;
        String seatsJsonData = getjson(seatsURL);
        //extracts a list of unsorted seass
        List<Seat> seats = TrainFunctions.extractSeats(seatsJsonData);
        //split seats into two lists by class, then sort by number, then sort by letter
        List<List<Seat>> classSeats = TrainFunctions.sortSeats(seats);

        return ResponseEntity.ok(classSeats);//JsonData
    }

    // get an individual seat by its id
    @GetMapping("api/getSeat")
    public ResponseEntity<?> getSeat(String trainCompany, String trainId, String seatId) {

        // get a list of all the times
        ResponseEntity<?> tempTimes = getTrainTimes(trainCompany, trainId);
        List<String> times = (List<String>) tempTimes.getBody();
        Optional<Seat> seat;

        // for each instance of a train by time, check all seats for match
        // return error if not found
        for (String time : times) {
            List<List<Seat>> seats = getAvailableSeats(trainCompany, trainId, time).getBody();

            seat = TrainFunctions.getSeatByID(seatId, seats);
                if (seat.isPresent()) {
                    return ResponseEntity.ok(seat); // HTTP 200 with the seat as the response body
                }
        }
        String errorMessage = "Seat not found" ;
        return ResponseEntity.status(404).body(errorMessage); // HTTP 404 with the error message
    }

    //take a list of quotes (tentative tickets), make tickets out of them, return them together as one booking
    @PostMapping("api/confirmQuotes")
    public ResponseEntity<?> confirmQuotes(@RequestBody ArrayList<Quote> quotes) {
        //create list for tickets, generate booking referenece and get the user
        List<Ticket> tickets = new ArrayList<>();
        UUID bookingRef = UUID.randomUUID();
        User user = getUser();

        //for each quote, create a ticket
        quotes.stream().forEach(quote -> {
            Ticket newTicket = new Ticket(quote.getTrainCompany(), quote.getTrainId(), quote.getSeatId(), UUID.randomUUID(), user.getEmail(), bookingRef.toString());
            tickets.add(newTicket);
        });

        //create a booking out of the tickets and add it to the bookings stored locally
        Booking booking = new Booking(UUID.randomUUID(), LocalDateTime.now(), tickets, user.getEmail());
        bookings.add(booking);

        String successMsg = "Successfully submitted";
        return ResponseEntity.status(204).body(successMsg);
    }

    // get all the bookings from a specific user
    @GetMapping("api/getBookings")
    public ResponseEntity<?> getBookings() {
        //get the users email
        String email = getUser().getEmail();
        List<Booking> bookingList = new ArrayList<>();

        //check for bookings of the current user, adding them to list to be returned
        for (Booking booking : bookings) {
            if (Objects.equals(booking.getCustomer(), email)){
                bookingList.add(booking);
            }
        }

        return ResponseEntity.ok(bookingList);
    }

    // returns all the bookings
    @GetMapping("api/getAllBookings")
    public ResponseEntity<?> getAllBookings() {
        List<Booking> bookingList = new ArrayList<>(bookings);
        return ResponseEntity.ok(bookingList);
    }

    //blank function just to check authority, should be easy to complete
    @GetMapping("api/getBestCustomers")
    public ResponseEntity<?> getBestCustomers() {
        System.out.println("in getBestCustomers");
        ArrayList<String> customers = new ArrayList<>();

        String fakeCustomer = "fake customer";
        customers.add(fakeCustomer);
        return ResponseEntity.ok(customers);
    }
}



