package actors

import models.Signer

import play.api._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator.Pushee
import play.Logger

import akka.actor._
import akka.actor.Actor._
import play.api.libs.concurrent._
import play.api.Play.current
import play.libs.Akka

/**
 * Created by IntelliJ IDEA.
 * User: alabbe
 * Date: 08/02/12
 * Time: 01:26
 */


   class SignatureWorker extends Actor {
	  import SignatureWorker._
      var signers : Option[Pushee[Signer]] = None

      def receive = {
         case Listen() => {
            lazy val channel: Enumerator[Signer] = Enumerator.pushee(
               	pushee => self ! Init(pushee),
				onComplete = self ! Quit()
            )
            Logger.info("New signers stream on")
            sender ! channel
         }

         case Quit() => {
            Logger.info("Signature stream stopped ...")
            signers = None
         }

         case Signed(signer) => {
            Logger.info("New signature : " + signer.toString)
            signers.map(_.push(signer))
         }
      }
   }
   object SignatureWorker {
      trait Event
      case class Listen() extends Event
      case class Quit() extends Event
      case class Signed(s:Signer)
	  case class Init(p:Pushee[Signer])
      lazy val ref = Akka.system.actorOf(Props[SignatureWorker])

}
