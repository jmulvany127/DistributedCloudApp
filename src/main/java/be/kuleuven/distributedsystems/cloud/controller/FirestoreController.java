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
        docData.put("bookingId", id);
        docData.put("customer", customer);
        docData.put("time", time);
        docData.put("tickets", tickets);
        //store in db
        docRef.set(docData);
    }

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

    public void addTrainInfo() {
            //TODO, check if data has been initalised first, dont add twice
            CollectionReference colRef = firestore.collection("OurTrain");
            Train ourTrain = getTrain("data.json");
            List<Seat> seats = getSeats("data.json");

            DocumentReference trainDocRef = colRef.document(ourTrain.getName());
            ApiFuture<WriteResult> result = trainDocRef.set(ourTrain, SetOptions.merge());

            Map<String, List<Seat>> seatsGrouped = groupSeats(seats);

            for (Map.Entry<String, List<Seat>> entry : seatsGrouped.entrySet()) {
                List<Seat> seatsAtTime = entry.getValue();
                CollectionReference seatRef = trainDocRef.collection(entry.getKey());

                for (Seat seat : seatsAtTime) {
                    seatRef.add(seat);
                }
            }
    }

    private Map<String, List<Seat>> groupSeats(List<Seat> seats) {
        Map<String, List<Seat>> seatsGrouped = new HashMap<>();
        for (Seat seat : seats) {
            String time = seat.getTime();
            seatsGrouped.computeIfAbsent(time, k -> new ArrayList<>()).add(seat);
        }
        return seatsGrouped;
    }

    public Train getTrain(String fileName) {
        JsonObject jsonObject = getJsonObject("data.json");
        JsonArray trainsArray = jsonObject.getAsJsonArray("trains");
        JsonObject ourTrain = trainsArray.get(0).getAsJsonObject();

        Gson gson = new Gson();
        return gson.fromJson(ourTrain, Train.class);
    }

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

    public JsonObject getJsonObject(String fileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        Gson gson = new Gson();
        return gson.fromJson(new InputStreamReader(inputStream), JsonObject.class);
    }

    //TODO check / implement this function
    private boolean dataInitialised() {
        CollectionReference collectionReference = firestore.collection("OurTrain");
        ApiFuture<DocumentSnapshot> future = collectionReference.document("train").get();

        try {
            return !future.get().exists();
        } catch (ExecutionException | InterruptedException e) {
            return true;
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