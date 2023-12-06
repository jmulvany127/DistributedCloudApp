package be.kuleuven.distributedsystems.cloud.entities;
import com.google.gson.Gson;
import java.util.UUID;

public class Quote {

    private String trainCompany;
    private String trainId;
    private String seatId;

    public Quote() {
    }

    public Quote(String trainCompany, String trainId, String seatId) {
        this.trainCompany = trainCompany;
        this.trainId = trainId;
        this.seatId = seatId;
    }

    public String getTrainCompany() {
        return trainCompany;
    }

    public void setTrainCompany(String trainCompany) {
        this.trainCompany = trainCompany;
    }

    public String getTrainId() {
        return trainId;
    }

    public void setTrainId(String trainId) {
        this.trainId = trainId;
    }

    public String getSeatId() {
        return this.seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Quote)) {
            return false;
        }
        var other = (Quote) o;
        return this.trainCompany.equals(other.trainCompany)
                && this.trainId.equals(other.trainId)
                && this.seatId.equals(other.seatId);
    }

    @Override
    public int hashCode() {
        return this.trainCompany.hashCode() * this.trainId.hashCode() * this.seatId.hashCode();
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}