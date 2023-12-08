# Distributed Cloud Application

## Application Overview
This distributed cloud application is a Java-based web application developed to manage seat reservations for trains operated by various train companies. The application leverages cloud computing for resource outsourcing and utilizes the Spring Boot framework for streamlined development.

## Using the application
The application is deployed at the following link: https://fos-jm-cloud-app.ew.r.appspot.com. You can sign in with any email password combination, once the email is of valid email format. 


## Key Features
Here are the essential components and features of our distributed cloud application:

- **Reservation System**: The application facilitates customers in retrieving and reserving available trains and seats. We have adopted the Spring Boot framework to expedite the development process.

- **User Authentication**: We have implemented Firebase Authentication to ensure secure access and user authorization. Tokens are signed by Google App Engine and we manually verify them.

- **Indirect Communication**: Cloud Pub/Sub push subscriptions are employed to enable efficient communication and decoupling of background processes, enhancing scalability.

- **Fault Tolerance**: The application is designed with fault tolerance in mind, particularly in communication with train companies, to ensure continuous service even in the event of failures.

- **Data Persistence**: All bookings are stored in Cloud Firestore, providing reliable and scalable data storage. There is also an internal train company with all of its data stored in Cloud Firestore. 

- **ACID Properties**: We maintain Atomicity during the confirmation of quotes, ensuring that either all quotes are successfully reserved or none at all. Consistency, Isolation, and Durability properties are also upheld.
  
## Send Grid Notice
Our Sendgrid account has been deactivated and thus our feedback system is inoperable. Feedback emails will be sent from the application during the booking process however they will remain stuck in the processing phase on the SendGrid server and will never reach the customer.

