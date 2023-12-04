package be.kuleuven.distributedsystems.cloud.controller;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.IOException;

public class SendGridController {
    public static void main(String[] args) throws IOException {
        Email from = new Email("fionnanmurchu.osullivan@student.kuleuven.be");
        String subject = "Sending with Twilio SendGrid is Fun";
        Email to = new Email("fionnanmurchu.osullivan@student.kuleuven.be");
        Content content = new Content("text/plain", "and easy to do anywhere, even with Java");
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid("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");
        //"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"
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