package controllers

import models._
import play.api.mvc._

import actors.SignatureWorker
import actors.SignatureWorker._

import play.api.data._
import play.api.data.Forms._

import java.util.UUID
import play.Logger

import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.Comet

import akka.util.duration._
import akka.util.Timeout

import play.api.libs.json._
import play.api.libs.json.Json._
import akka.pattern.ask

object Application extends Controller {
  val TOKEN_KEY = "token"

  def index = Action {
    import play.api.Play.current
    Ok(views.html.index(petitionForm))
  }
   
  def sign() = Action { implicit request =>
    import play.api.Play.current
    petitionForm.bindFromRequest().fold(
       error => BadRequest(views.html.index(petitionForm)),
       email => {
          val signer: Signer = Signer(email, UUID.randomUUID().toString)
          Signer.create(signer)
          val confirmURL: String = "http://" + request.host + routes.Application.confirm(signer.code).url
          Logger.debug(confirmURL)
          //Logger.debug(toEmail(signer))
          Redirect(routes.Application.share()).withSession(TOKEN_KEY->signer.code)
       }
    )
  }

   def stream = Action {
		val cometEnumeratee = Comet(callback = "window.parent.signIt")(Comet.CometMessage[Signer](signer => {
           Logger.debug("converting to json")
           toJson(signer).toString
        }))
		
      AsyncResult {
		implicit val timeout = Timeout(5 second)
         (SignatureWorker.ref ? Listen()).mapTo[Enumerator[Signer]].asPromise.map {
			chunks => {
               Logger.debug("un chunk")
               Ok.stream(chunks &> cometEnumeratee)
            }
         }
      }
   }

  def share() = Action { implicit request =>
   import play.api.Play.current
   request.session.get(TOKEN_KEY).toRight(Redirect(routes.Application.index()))
                                  .right
                                  .map(s =>{
                                     //val confirmURL: String = "http://" + request.host + routes.Application.confirm(s).url
                                     Ok(views.html.share())
                                  })
                                  .fold(identity,identity)
                             }
   
  def confirm(code: String) = Action { implicit request =>
     import play.api.Play.current
     Signer.byCode(code).map( signer => Ok(views.html.confirm(confirmationForm.fill(signer), code)))
                        .getOrElse(NotFound)
     //request.body.get("code").headOption.map()
  }
   
  def doConfirm(code:String) = Action { implicit request =>
     import play.api.Play.current
     confirmationForm.bindFromRequest().fold(
         formWithErrors => BadRequest(views.html.confirm(formWithErrors, code)),
         signer => {
            //todo update the signer
            signer.update()
            SignatureWorker.ref ! signer
            Logger.debug("New signature : " + signer.format)
            Redirect(routes.Application.share())
         }
     )
  }

  val petitionForm:Form[String] = Form("email" -> email)
   
  val confirmationForm:Form[Signer] = Form(
      mapping(
         "signer.email" -> email,
         "signer.code" -> ignored("ALREADY_USED"),
         "signer.firstName" -> some(text),
         "signer.lastName" -> some(text),
         "signer.age" -> some(number (min = 18)),
         "signer.city" -> some(text)
      )(Signer.apply)(Signer.unapply)
  
  ) 
  def some[A](mapping:Mapping[A]) = {
     optional(mapping) verifying ("This field is required", _.isDefined)
  }

  def toEmail(signer:Signer) = {
     import play.api.Play.current
     views.html.mail(signer).toString
  }
}
