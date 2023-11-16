# Distributed Cloud Application

## Application Overview
This distributed cloud application is a Java-based web application developed to manage seat reservations for trains operated by various train companies. The application leverages cloud computing for resource outsourcing and utilizes the Spring Boot framework for streamlined development.

## Running the application
- **Firebase Emulator**: Run the firebase-tools-instant-win executable from the project root directory. Run the following command in the terminal promt to start the Firebase Emulator suite: 
firebase emulators:start --project demo-distributed-systems-kul

- **Pub/Sub**: First register the topic by running the TicketsTopic file. After this run the TicketSubscription file to register the subscription to the topic. 

- **Application**: The emulator and pub/sub services are now running so the main application can be run. This is done using spring-boot:run command. 

- **Use**: Everything is now running and you can use the application by visiting http://localhost:8080/ in your browser.

## Key Features
Here are the essential components and features of our distributed cloud application:

- **Reservation System**: The application facilitates customers in retrieving and reserving available trains and seats. We have adopted the Spring Boot framework to expedite the development process.

- **User Authentication**: We have implemented Firebase Authentication to ensure secure access and user authorization.

- **Indirect Communication**: Cloud Pub/Sub push subscriptions are employed to enable efficient communication and decoupling of background processes, enhancing scalability.

- **Fault Tolerance**: The application is designed with fault tolerance in mind, particularly in communication with train companies, to ensure continuous service even in the event of failures.

- **Data Persistence**: All bookings are stored in Cloud Firestore, providing reliable and scalable data storage.

- **ACID Properties**: We maintain Atomicity during the confirmation of quotes, ensuring that either all quotes are successfully reserved or none at all. Consistency, Isolation, and Durability properties are also upheld.
