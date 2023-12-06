package be.kuleuven.distributedsystems.cloud.controller;
// using SendGrid's Java Library
// https://github.com/sendgrid/sendgrid-java
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.IOException;

public class SendGridController{
    public static void sendEmail (String userEmail,String subject, String message) throws IOException {
        Email from = new Email("joe.mulvany@student.kelueven.be");
        Email to = new Email(userEmail);
        Content content = new Content("text/plain", message);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid("SG.8b7EWdwrTUWaTuvmixT31Q.Ro87mYTg6t4Yd0-Wi98kuedDT62hcCt5hZARvzDs1ewcfI0g.Ngv6U_isnDcIKnKR1nCYeHIGZZgCoYAysAIIr4keduA");
        Request request = new Request();
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
