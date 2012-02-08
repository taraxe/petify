/*
package controllers

/**
 * Created by IntelliJ IDEA.
 * User: alabbe
 * Date: 07/02/12
 * Time: 22:47
 * To change this template use File | Settings | File Templates.
 */

import org.apache.commons.lang.StringUtils;


import play.Logger;
import play.Play;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import play.api.libs.concurrent._
import play.api.libs.akka._
import akka.dispatch.{ Future, Await }
import akka.actor.ActorSystem
import java.io.{FileNotFoundException, File}
import akka.actor._
import akka.util.duration._
import org.apache.commons.mail.{Email, EmailException}
import javax.mail._
import play.api.Play._

/**
 * Mail utils
 */
class Mail {

   val session:Session
   val asynchronousSend: Boolean = true;


   /**
    * Send an email
    */
   def send(email: Email): Promise[Boolean] = {
      val e = buildMessage(email);
      (configuration.getString("mail.smtp"), "DEV").map {
         /*case ("mock", Play.Mode.DEV)  => {
            Mock.send(e);
            new AkkaPromise[Boolean] {

            }
         }*/
         case _ => {
            e.setMailSession(getSession);
            sendMessage(e);
         }
      }
   }

   /**
    *
    */
   def buildMessage(email: Email): Email = {

      val from = configuration.getString("mail.smtp.from");
      if (email.getFromAddress == null && !StringUtils.isEmpty(from)) {
         email.setFrom(from);
      } else if (email.getFromAddress == null) {
         throw new MailException("Please define a 'from' email address", new NullPointerException);
      }
      if ((email.getToAddresses == null || email.getToAddresses.size == 0) &&
            (email.getCcAddresses == null || email.getCcAddresses.size == 0)  &&
            (email.getBccAddresses == null || email.getBccAddresses.size == 0))
      {
         throw new MailException("Please define a recipient email address", new NullPointerException);
      }
      if (email.getSubject == null) {
         throw new MailException("Please define a subject", new NullPointerException);
      }
      if (email.getReplyToAddresses == null || email.getReplyToAddresses.size == 0) {
         email.addReplyTo(email.getFromAddress.getAddress);
      }
      email;
   }

   def getSession: Session = {
      if (session == null) {
         val props = new Properties;
         // Put a bogus value even if we are on dev mode, otherwise JavaMail will complain
         props.put("mail.smtp.host", configuration.getString("mail.smtp.host", "localhost").get);

         var channelEncryption = "";
         configuration.getString("mail.smtp.protocol", "smtp").map {
            case Some("smtps") => channelEncryption = "starttls";
            case _ => channelEncryption = configuration.getString("mail.smtp.channel", "clear");
         }

         if (channelEncryption.equals("clear")) {
            props.put("mail.smtp.port", "25");
         } else if (channelEncryption.equals("ssl")) {
            // port 465 + setup yes ssl socket factory (won't verify that the server certificate is signed with a root ca.)
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "play.utils.YesSSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
         } else if (channelEncryption.equals("starttls")) {
            // port 25 + enable starttls + ssl socket factory
            props.put("mail.smtp.port", "25");
            props.put("mail.smtp.starttls.enable", "true");
            // can't install our socket factory. will work only with server that has a signed certificate
            // story to be continued in javamail 1.4.2 : https://glassfish.dev.java.net/issues/show_bug.cgi?id=5189
         }

         configuration.getString("mail.smtp.localhost").map {s =>
            props.put("mail.smtp.localhost", s);
         }

         configuration.getString("mail.smtp.socketFactory.class").map{ s =>
            props.put("mail.smtp.socketFactory.class",s);
         }
         configuration.getString("mail.smtp.port").map{ s =>
            props.put("mail.smtp.port", s);
         }
         val user = configuration.getString("mail.smtp.user");
         val password = configuration.getString("mail.smtp.passord");
         val authenticator = configuration.getString("mail.smtp.authenticator");
         session = null;

         if (authenticator != null) {
            props.put("mail.smtp.auth", "true");
            try {
               session = Session.getInstance(props, Play.classloader.loadClass(authenticator).newInstance.asInstanceOf[Authenticator]);
            } catch {
               case e:Exception => Logger.error("Cannot instanciate custom SMTP authenticator (%s)".format(authenticator),e);
            }
         }

         if (session == null) {
            if (user != null && password != null) {
               props.put("mail.smtp.auth", "true");
               session = Session.getInstance(props, new SMTPAuthenticator(user, password));
            } else {
               props.remove("mail.smtp.auth");
               session = Session.getInstance(props);
            }
         }

         if (Boolean.parseBoolean(configuration.getBoolean("mail.debug", false))) {
            session.setDebug(true);
         }
      }
      session;
   }

   /**
    * Send a JavaMail message
    *
    * @param msg An Email message
    */
   def sendMessage(msg: Email): Promise[Boolean] = {
      if (asynchronousSend) {
         (MailWorker.ref ? msg).mapTo[Boolean].asPromise
      } else {
         try {
            msg.setSentDate(new Date);
            msg.send;
            new AkkaPromise[Boolean]{
               
            }.mapTo[Boolean].asPromise;
         } catch {
            case e:MailException => {
               new MailException("Error while sending email", e);
               Logger.error("The email has not been sent",e);
               result.append("oops");
            }
         }
      }
   }

   case class SMTPAuthenticator(user: String, password: String) extends Authenticator {

      override def getPasswordAuthentication: PasswordAuthentication = {
         new PasswordAuthentication(user, password);
      }
   }

   class Mock {

      val emails = Map[String, String].empty;

      def getContent(message: Part): String = {

         if (message.getContent.isInstanceOf[String]) {
            return message.getContentType + ": " + message.getContent + " \n\t";
         } else if (message.getContent != null && message.getContent.isInstanceOf[Multipart]) {
            val part:Multipart = message.getContent.asInstanceOf[Multipart];
            val text = "";

            for (i <- 0 until part.getCount) {
               val bodyPart = part.getBodyPart(i);
               if (!Message.ATTACHMENT.equals(bodyPart.getDisposition)) {
                  text += getContent(bodyPart);
               } else {
                  text += "attachment: \n" +
                        "\t\t name: " + (StringUtils.isEmpty(bodyPart.getFileName) ? "none" : bodyPart.getFileName) + "\n" +
                        "\t\t disposition: " + bodyPart.getDisposition + "\n" +
                        "\t\t description: " +  (StringUtils.isEmpty(bodyPart.getDescription) ? "none" : bodyPart.getDescription)  + "\n\t";
               }
            }
            return text;
         }
         if (message.getContent != null && message.getContent.isInstanceOf[Part]) {
            if (!Message.ATTACHMENT.equals(message.getDisposition)) {
               return getContent(message.asInstanceOf[Part].getContent);
            } else {
               return "attachment: \n" +
                     "\t\t name: " + (StringUtils.isEmpty(message.getFileName) ? "none" : message.getFileName) + "\n" +
                     "\t\t disposition: " + message.getDisposition + "\n" +
                     "\t\t description: " + (StringUtils.isEmpty(message.getDescription) ? "none" : message.getDescription) + "\n\t";
            }
         }
         "";
      }


      def send(email: Email): Unit = {

         /*try {
            final StringBuffer content = new StringBuffer;
         Properties props = new Properties;
         props.put("mail.smtp.host", "myfakesmtpserver.com");

         Session session = Session.getInstance(props);
         email.setMailSession(session);

         email.buildMimeMessage;

         MimeMessage msg = email.getMimeMessage;
         msg.saveChanges;

         String body = getContent(msg);

         content.append("From Mock Mailer\n\tNew email received by");


         content.append("\n\tFrom: " + email.getFromAddress.getAddress);
         content.append("\n\tReplyTo: " + ((InternetAddress) email.getReplyToAddresses.get(0)).getAddress);
         content.append("\n\tTo: ");
         for (Object add : email.getToAddresses) {
            content.append(add.toString + ", ");
         }
         // remove the last ,
         content.delete(content.length - 2, content.length);
         if (email.getCcAddresses != null && !email.getCcAddresses.isEmpty) {
            content.append("\n\tCc: ");
            for (Object add : email.getCcAddresses) {
               content.append(add.toString + ", ");
            }
            // remove the last ,
            content.delete(content.length - 2, content.length);
         }
         if (email.getBccAddresses != null && !email.getBccAddresses.isEmpty) {
            content.append("\n\tBcc: ");
            for (Object add : email.getBccAddresses) {
               content.append(add.toString + ", ");
            }
            // remove the last ,
            content.delete(content.length - 2, content.length);
         }
         content.append("\n\tSubject: " + email.getSubject);
         content.append("\n\t" + body);

         content.append("\n");
         Logger.info(content.toString);

         for (Object add : email.getToAddresses) {
            content.append(", " + add.toString);
            emails.put(((InternetAddress) add).getAddress, content.toString);
         }

         } catch (Exception e) {
            Logger.error(e, "error sending mock email");
         }
*/
      }

      def getLastMessageReceivedBy(email: String): String = {
         emails.get(email);
      }

      def reset: Unit = {
         emails.clear;
      }
   }

   class MailWorker extends Actor {
      def receive = {
         case e:Email => {
            msg.setSentDate(new Date);
            msg.send;
            sender ! true;
         }
      }
   }

   object MailWorker {
      val system = ActorSystem("system")
      val ref = system.actorOf(Props[MailWorker], name= "mailWorker")
   }

   case class MailException(message:String, e:java.lang.Exception) extends RuntimeException

}

*/
