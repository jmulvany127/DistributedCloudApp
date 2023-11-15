package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.TicketsTopic;
import be.kuleuven.distributedsystems.cloud.entities.*;
import be.kuleuven.distributedsystems.cloud.TicketsTopic.*;

import java.awt.print.Book;
import java.lang.reflect.Array;
import java.net.http.HttpResponse;
import java.util.*;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.sendgrid.Response;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.http.HttpStatus;
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
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static be.kuleuven.distributedsystems.cloud.auth.SecurityFilter.getUser;
import static java.util.stream.Collectors.groupingBy;

@RestController
public class SubscriberController {

/*    @PostMapping ("/subscription")
    public void subscription(@RequestBody PubsubMessage message){
        System.out.println(message);
    }*/

}

