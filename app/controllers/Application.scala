package controllers

import play.api._
import models.Model._
import models._
import data.Form
import libs.oauth.{OAuth, ConsumerKey}
import play.api.libs.oauth._
import play.api.data._
import play.api.data.Forms._
import format.Formats._
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc._
import java.net.URLEncoder
import java.util.UUID

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
          println(signer.code)
          Redirect(routes.Application.share())
       }
    )
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
            println(signer.code)
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
}

object Twitter extends Controller {

   // OAuth

   val KEY = ConsumerKey("WsjQdxMVZ3Nasnhk1y2uDQ",
      "BNFz31YmhR63pL28kX7tqzgEvLhQQRtZXLjtfgvTZw")

   val TWITTER = OAuth(ServiceInfo(
      "https://api.twitter.com/oauth/request_token",
      "https://api.twitter.com/oauth/access_token",
      "https://api.twitter.com/oauth/authorize", KEY))

   def authenticate = Action { request =>
      request.queryString.get("oauth_verifier").flatMap(_.headOption).map { verifier =>
         val tokenPair = sessionTokenPair(request).get
         // We got the verifier; now get the access token, store it and back to index
         TWITTER.retrieveAccessToken(tokenPair, verifier) match {
            case Right(t) => {
               // We received the unauthorized tokens in the OAuth object - store it before we proceed
               Redirect(routes.Application.index).withSession("token" -> t.token, "secret" -> t.secret)
            }
            case Left(e) => throw e
         }
      }.getOrElse(
         TWITTER.retrieveRequestToken("http://localhost:9000/auth") match {
            case Right(t) => {
               // We received the unauthorized tokens in the OAuth object - store it before we proceed
               Redirect(TWITTER.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
            }
            case Left(e) => throw e
         })
   }

   def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
      for {
         token <- request.session.get("token")
         secret <- request.session.get("secret")
      } yield {
         RequestToken(token, secret)
      }
   }

   def tweet(status:String) = Action { implicit request =>
      val tokens = sessionTokenPair.get

      AsyncResult(
         WS.url("https://api.twitter.com/1/statuses/update.json")
            .sign(OAuthCalculator(Twitter.KEY, tokens))
            .post(URLEncoder.encode(status,"utf-8")).map(r => Ok(r.json \ "id") )
      )
   }
   
}