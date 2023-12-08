package be.kuleuven.distributedsystems.cloud.controller;
// using SendGrid's Java Library
// https://github.com/sendgrid/sendgrid-java
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.IOException;

//Function to send Sendgrid emails, takes user email, subject line and body
public class SendGridController{
    public static void sendEmail (String userEmail,String subject, String message) throws IOException {
        //set sender/receiver, and create mail
        Email from = new Email("joe.mulvany@student.kuleuven.be");
        Email to = new Email(userEmail);
        Content content = new Content("text/plain", message);
        Mail mail = new Mail(from, subject, to, content);

        //set sendgrid Key and create a new sendgrid request object type
        //Nite that since our sendgrid account is deactivated these emails will not be sent to the recipient howver they will be received by sendgrid as a middle man however will remain in processing here
        SendGrid sg = new SendGrid("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");
        Request request = new Request();
        //Send email to sendGrid and get server response
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
            System.out.println(response.getHeaders());
        } catch (IOException ex) {
            throw ex;
        }
    }


    }
