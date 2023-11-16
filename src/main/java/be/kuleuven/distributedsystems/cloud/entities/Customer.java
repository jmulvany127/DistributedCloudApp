package be.kuleuven.distributedsystems.cloud.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Customer {
    private String customer;
    private int numberOfTickets;

    public Customer(String customer, int numberOfTickets) {
        this.customer = customer;
        this.numberOfTickets = numberOfTickets;
    }

    public String getCustomer(){ return this.customer; }
    public int getNumberOfTickets() { return this.numberOfTickets; }

    public void addTickets(int number) {
        numberOfTickets = numberOfTickets + number;
    }
}