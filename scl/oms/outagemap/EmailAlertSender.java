/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scl.oms.outagemap;

import java.io.*;
import java.net.InetAddress;
import java.util.Properties;
import java.util.Date;

import javax.mail.*;
import javax.mail.internet.*;
import com.sun.mail.smtp.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EmailAlertSender {

    public static void send(String messageSubject, String messageBody) throws Exception {
        if (!Config.INSTANCE.isEmailAlerts()) {
            return;
        }        
        
        Logger log = Log.getLogger();
        
        Properties properties = System.getProperties();
        properties.put("mail.smtp.user",Config.INSTANCE.getEmailUser());
        properties.put("mail.smtp.host",Config.INSTANCE.getSmtpMtaHost());
        properties.put("mail.smtp.port",Config.INSTANCE.getSmtpPort());
        properties.put("mail.smtp.from",Config.INSTANCE.getEmailOriginator()); //SMTP MAIL command
        properties.put("mail.smtp.ehlo",true);
        properties.put("mail.smtp.sendpartial",true); //partial failure causes a SendFailedException

        Session session = Session.getInstance(properties, null);
        
        Message email = new MimeMessage(session);
        email.setFrom(new InternetAddress(Config.INSTANCE.getEmailOriginator()));
        email.setReplyTo(InternetAddress.parse(Config.INSTANCE.getEmailOriginator()));
        email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(Config.INSTANCE.getEmailRecipient(), false));
        email.setSubject(messageSubject + " (" + Config.INSTANCE.getEnvironmentLabel() + ")");
        email.setText(messageBody);
        email.setHeader("X-Mailer", "OutageMapCreator");
        email.setSentDate(new Date());
        
        SMTPTransport transport = (SMTPTransport)session.getTransport("smtp");
        transport.connect();
        transport.sendMessage(email, email.getAllRecipients());
        log.log(Level.INFO, "Email response: ",transport.getLastServerResponse()); // this needs to be logged.
        transport.close();
    }
}