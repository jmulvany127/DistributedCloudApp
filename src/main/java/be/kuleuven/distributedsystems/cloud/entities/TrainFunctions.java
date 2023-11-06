package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import be.kuleuven.distributedsystems.cloud.entities.Train;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.sendgrid.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
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
public class TrainFunctions {
    private final WebClient.Builder webClientBuilder;
    public final ObjectMapper objectMapper;
    private final String ReliableTrainCompany = "https://reliabletrains.com/?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE";
    private final String ReliableTrains = "https://reliabletrains.com/trains?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE";
    private final String TrainEg = "https://reliabletrains.com/trains/c3c7dec3-4901-48ce-970d-dd9418ed9bcf?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE";

    public TrainFunctions(WebClient.Builder webClientBuilder, ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;

    }
    public static List<Train> extractTrains(String jsonData) {
        List<Train> trains = new ArrayList<>();
        //takes json data and converts it to objects
        ObjectMapper objectMapper = new ObjectMapper();
        //prevnts the mapper from failing when it envounters an unkown json field
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            // Parse the json data into a json node (from the jackson library)
            JsonNode rootNode = objectMapper.readTree(jsonData);

            // Check if the json contains "_embedded" and "trains" elements
            if (rootNode.has("_embedded") && rootNode.get("_embedded").has("trains")) {
                JsonNode trainsNode = rootNode.get("_embedded").get("trains");

                for (JsonNode trainNode : trainsNode) {
                    // Deserialize each train json object into a train object and add to trains
                    Train train = objectMapper.readValue(trainNode.toString(), Train.class);
                    trains.add(train);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trains;
    }

    public static List<Seat> extractSeats(String jsonData) {
        List<Seat> seats = new ArrayList<>();
        //takes json data and converts it to objects
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        //prevnts the mapper from failing when it envounters an unkown json field
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            // Parse the JSON data into a JSON node
            JsonNode rootNode = objectMapper.readTree(jsonData);
            //gets the embedded node and then the seats node from the root
            JsonNode seatsNode = rootNode.get("_embedded").get("seats");
            //not sure if neccessayr will test
            if (rootNode.has("_embedded") && rootNode.get("_embedded").has("seats")) {
                for (JsonNode seatNode : seatsNode) {
                    // Deserialize each seat jsonobject into a seat object and add to seats
                    Seat seat = objectMapper.readValue(seatNode.toString(), Seat.class);
                    seats.add(seat);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return seats;
    }

    //divides seats list by class, then sorts by number and then letter
    public static List<List<Seat>> sortSeats(List<Seat> seats) {
        List<Seat> firstClass = new ArrayList<>();
        List<Seat> secondClass = new ArrayList<>();
        //divides all seats into two new lists
        for (Seat seat : seats) {
            if (seat.getType().equals("1st class")) {
                firstClass.add(seat);
            } else if (seat.getType().equals("2nd class")) {
                secondClass.add(seat);
            }
        }
        //creates list of lists, sort then add class based lists
        List<List<Seat>> splitSeats =new ArrayList<>();
        splitSeats.add(orderSeats(firstClass));
        splitSeats.add(orderSeats(secondClass));
        return splitSeats;
    }

    //sorts cseats by number and then letter
    public static List<Seat> orderSeats(List<Seat> seats){
        //sorts by number
        Comparator<Seat> seatNumComparator= new Comparator<Seat>() {
            @Override
            public int compare(Seat seat1, Seat seat2) {
                //isolate seat number in string
                String seat1Name = seat1.getName().replaceAll("[a-zA-Z]", "");
                String seat2Name = seat2.getName().replaceAll("[a-zA-Z]", "");

                int seat1Num = Integer.parseInt(seat1Name);
                int seat2Num = Integer.parseInt(seat2Name);
                //Compare in ascending order
                return seat1Num-seat2Num;
            }
        };
        Collections.sort(seats, seatNumComparator);

        //sort by letter
        Comparator<Seat> seatLetterComparator= new Comparator<Seat>() {
            @Override
            public int compare(Seat seat1, Seat seat2) {
                String seat1Name = seat1.getName();
                String seat2Name = seat2.getName();
                // if seat number is equal the sort by letter, if not leave as
                if (seat1Name.replaceAll("[a-zA-Z]", "").equals(seat2Name.replaceAll("[a-zA-Z]", "")) ){
                    //isolate seat letter in string
                    seat1Name = seat1Name.replaceAll("[1-9]","");
                    seat2Name = seat2Name.replaceAll("[1-9]","");

                    //compare in ascending order
                    return seat1Name.compareTo(seat2Name);
                }else{return 0;}
            }
        };
        Collections.sort(seats, seatLetterComparator);
        return seats;
    }








    public static List<String> extractTrainTimes(String jsonData) {
        List<String> times = new ArrayList<>();
        //takes json data and converts it to objects
        ObjectMapper objectMapper = new ObjectMapper();
        // List to store LocalDateTime objects
        List<LocalDateTime> dateTimeList = new ArrayList<>();
        // DateTimeFormatter to parse the date time strings
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        try {
            // Parse the json data into a jsonnode
            JsonNode rootNode = objectMapper.readTree(jsonData);
            // retrieves the string list arrAY Under the emvbedded key
            JsonNode stringList = rootNode.at("/_embedded/stringList");

            ///get time as a string, parse to date time object and then sort by date and time
            for(JsonNode jsonTime: stringList){
                String stringTime = jsonTime.asText();
                LocalDateTime time = LocalDateTime.parse(stringTime, formatter);
                dateTimeList.add(time);
            }
            Collections.sort(dateTimeList);

            //convert back to string
            for (LocalDateTime dateTime : dateTimeList) {
                String formattedDateTime = dateTime.format(formatter);
                times.add(formattedDateTime);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return times;
    }

// returns a train object by its ID from json data, or empty optional of not
    public static  Optional<Train> getTrainByID( String trainId, String jsonData){
        // Extract train objects from json data
        List<Train> allTrains = TrainFunctions.extractTrains(jsonData);
        // check for matching train, if not found return empty optional
        for (Train train : allTrains) {
            if (train.getTrainId().toString().equals(trainId)) {
                return Optional.of(train);
            }
        }
        return Optional.empty();
    }

    // returns a seat object by its ID from a list of lists of seats, or empty optional of not
    public static  Optional<Seat> getSeatByID( String seatId, List<List<Seat>> seats){
        if (seats == null){return Optional.empty();};
        //check for each list and each seat
        for (List<Seat> splitSeats: seats) {
            for (Seat seat : splitSeats) {

                if (seat.getSeatId().toString().equals(seatId)) {
                    return Optional.of(seat);
                }
            }
        }
        return Optional.empty();
    }

}
