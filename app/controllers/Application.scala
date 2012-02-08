package controllers

import models._
import actors._
import actors.SignatureWorker._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import java.util.UUID
import play.api.libs.Comet
import play.Logger
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import akka.util.duration._

object Application extends Controller {
  
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
          Logger.debug(signer.code)
          Redirect(routes.Application.share())
       }
    )
  }

   def stream = Action {
      AsyncResult {
         (SignatureWorker.ref ? (SignatureWorker.Listen(),5.seconds)).mapTo[Enumerator[String]].asPromise.map { chunks =>
            Ok.stream(chunks &> Comet(callback = "parent.signIt"))
         }
      }
   }

  def share() = Action { implicit request =>
   import play.api.Play.current
   Ok(views.html.share())
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

            SignatureWorker.ref ! signer
            Logger.debug(signer.code)
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

  def sendEmail(signer:Signer) = {
      views.html.mail(signer).text
  }
}
