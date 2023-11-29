package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import be.kuleuven.distributedsystems.cloud.entities.Train;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        DocumentReference docRef = firestore.collection("bookingCollection").document(booking.getId());
        //extract fields from booking
        String id = booking.getId();
        String time = booking.getTime();
        List<Ticket> tickets = booking.getTickets();
        String customer = booking.getCustomer();

        //create data object with fields of the booking to store in db
        Map<String, Object> docData = new HashMap<>();
        docData.put("bookingReference", id);
        docData.put("customer", customer);
        docData.put("time", time);
        docData.put("tickets", tickets);
        //store in db
        docRef.set(docData);
    }

    // function to return all bookings from the firestore
    public List<Booking> getAllBookings() {
        CollectionReference docRef = firestore.collection("bookingCollection");
        ApiFuture<QuerySnapshot> querySnapshot = docRef.get();

        try {
            //list to hold local bookings
            List<Booking> bookings = new ArrayList<>();
            //for each document create a local booking to be returned
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()){
                Booking newBooking = document.toObject(Booking.class);
                bookings.add(newBooking);
            }
            return bookings;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    // function to add the Eurostar London train to firestore
    public void addTrainInfo() {
            CollectionReference colRef = firestore.collection("OurTrain");
            Train ourTrain = getTrain("data.json");
            List<Seat> seats = getSeats("data.json");

            // create a new train
            DocumentReference trainDocRef = colRef.document(ourTrain.getName());
            ApiFuture<WriteResult> result = trainDocRef.set(ourTrain, SetOptions.merge());

            // group seats by train time
            Map<String, List<Seat>> seatsGrouped = groupSeats(seats);

            // add seats to the
            for (Map.Entry<String, List<Seat>> entry : seatsGrouped.entrySet()) {
                List<Seat> seatsAtTime = entry.getValue();
                CollectionReference seatRef = trainDocRef.collection(entry.getKey());

                for (Seat seat : seatsAtTime) {
                    seatRef.add(seat);
                }
            }
    }

    // function to group seats by time, seat data is stored by train time in firestore
    private Map<String, List<Seat>> groupSeats(List<Seat> seats) {
        Map<String, List<Seat>> seatsGrouped = new HashMap<>();
        for (Seat seat : seats) {
            String time = seat.getTime();
            seatsGrouped.computeIfAbsent(time, k -> new ArrayList<>()).add(seat);
        }
        return seatsGrouped;
    }

    // functino to get seats from json file
    public Train getTrain(String fileName) {
        JsonObject jsonObject = getJsonObject("data.json");
        JsonArray trainsArray = jsonObject.getAsJsonArray("trains");
        JsonObject ourTrain = trainsArray.get(0).getAsJsonObject();

        Gson gson = new Gson();
        return gson.fromJson(ourTrain, Train.class);
    }

    // function to extract seats from json file
    public List<Seat> getSeats(String fileName) {
        JsonObject jsonObject = getJsonObject("data.json");
        JsonArray trainsArray = jsonObject.getAsJsonArray("trains");

        Gson gson = new Gson();
        List<Seat> seats = new ArrayList<>();

        for (JsonElement trainElement : trainsArray) {
            JsonObject trainObject = trainElement.getAsJsonObject();
            JsonArray seatsArray = trainObject.getAsJsonArray("seats");

            for (JsonElement seatElement : seatsArray) {
                Seat seat = gson.fromJson(seatElement, Seat.class);
                seats.add(seat);
            }
        }
        return seats;
    }

    // function to get a JsonObject from .json file
    public JsonObject getJsonObject(String fileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        Gson gson = new Gson();
        return gson.fromJson(new InputStreamReader(inputStream), JsonObject.class);
    }

    // function to check if Eurostar London train data has been put into firestore
    public boolean dataInitialised() {
        DocumentReference docRef = firestore.collection("OurTrain").document("Eurostar London");
        try {
            //get snapshot of the document, returning false if it doesn't exist
            DocumentSnapshot docSnapShot = docRef.get().get();
            if (docSnapShot.exists()) {
                return true;
            } else {
                return false;
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    //DONT THINK WE NEED TO USE BUT KEEPING FOR THE MOMENT
    public Booking getBooking(String bookingId) {
        DocumentReference docRef = firestore.collection("bookingCollection").document(bookingId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            //get document from the db
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                //extract data from different fields of the document
                List<Ticket> ticketList = (List<Ticket>) document.get("tickets");
                String bookingRef = (String) document.get("bookingId");
                String timeString = (String) document.get("time");
                String time = String.valueOf(LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                String customer = (String) document.get("customer");

                return new Booking(bookingRef, time, ticketList, customer);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Error when receiving booking");
    }

}