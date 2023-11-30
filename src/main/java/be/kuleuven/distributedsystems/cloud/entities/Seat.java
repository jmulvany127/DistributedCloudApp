package be.kuleuven.distributedsystems.cloud.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public class Seat {
    private String trainCompany;
    private String trainId;
    private String seatId;
    private String time;
    private String type;
    private String name;
    private double price;

    public Seat() {
    }

    public Seat(String trainCompany, String trainId, String seatId, String time, String type, String name, double price) {
        this.trainCompany = trainCompany;
        this.trainId = trainId;
        this.seatId = seatId;
        this.time = time;
        this.type = type;
        this.name = name;
        this.price = price;
    }

    public void setTrainCompany(String trainCompany) {
        this.trainCompany = trainCompany;
    }

    public void setTrainId(String trainId) {
        this.trainId = trainId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public String getTrainCompany() {
        return trainCompany;
    }

    public String getTrainId() {
        return trainId;
    }

    public String getSeatId() {
        return this.seatId;
    }

    public String getTime() {
        return this.time;
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public double getPrice() {
        return this.price;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Seat)) {
            return false;
        }
        var other = (Seat) o;
        return this.trainCompany.equals(other.trainCompany)
                && this.trainId.equals(other.trainId)
                && this.seatId.equals(other.seatId);
    }

    @Override
    public int hashCode() {
        return this.trainCompany.hashCode() * this.trainId.hashCode() * this.seatId.hashCode();
    }
}
