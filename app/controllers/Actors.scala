package actors

import akka.actor.Actor
import models.Signer
import play.api.libs.iteratee.Enumerator
import play.Logger
import akka.actor.ActorSystem._
import akka.actor.Props._
import akka.actor._
import akka.actor.Actor._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import akka.actor.{Props, ActorSystem, Actor}


/**
 * Created by IntelliJ IDEA.
 * User: alabbe
 * Date: 08/02/12
 * Time: 01:26
 * To change this template use File | Settings | File Templates.
 */


   class SignatureWorker extends Actor {

      var listeners = Seq.empty[PushEnumerator[Signer]]

      def receive = {

         case SignatureWorker.Listen() => {
            lazy val channel: PushEnumerator[Signer] = Enumerator.imperative[Signer](
               onComplete = self ! SignatureWorker.Quit(channel)
            )
            listeners = listeners :+ channel
            Logger.info("New signers stream on")
            sender ! channel
         }

         case SignatureWorker.Quit(channel) => {
            Logger.info("Signature stream stopped ...")
            listeners = listeners.filterNot(_ == channel)
         }

         case SignatureWorker.Signed(signer) => {
            Logger.info("New signature : " + signer.toString)
            listeners.foreach(_.push(signer))
         }
      }
   }
   object SignatureWorker {
      trait Event
      case class Listen() extends Event
      case class Quit(channel:PushEnumerator[Signer]) extends Event
      case class Signed(s:Signer) extends Event
      lazy val system = ActorSystem("system")
      lazy val ref = system.actorOf(Props[SignatureWorker], name= "myWorker")

}
