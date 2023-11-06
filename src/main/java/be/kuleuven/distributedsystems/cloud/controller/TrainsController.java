package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import be.kuleuven.distributedsystems.cloud.entities.Train;
import be.kuleuven.distributedsystems.cloud.entities.TrainFunctions;

import java.util.*;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.sendgrid.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

        // get json data from the the baseurl
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

        //get a list of all the times
        ResponseEntity<?> tempTimes = getTrainTimes(trainCompany, trainId);
        List<String> times = (List<String>) tempTimes.getBody();
        Optional<Seat> seat;

        // for each instance of a train by time, check all seats for match
        //return error if not found
        for (String time : times) {
            List<List<Seat>> seats = getAvailableSeats(trainCompany, trainId, time).getBody();

            seat = TrainFunctions.getSeatByID(seatId, seats);
                if (seat.isPresent()) {
                    return ResponseEntity.ok(seat); // HTTP 200 with the train as the response body
                }
        }
        String errorMessage = "Seat not found" ;
        return ResponseEntity.status(404).body(errorMessage); // HTTP 404 with the error message
    }


}



