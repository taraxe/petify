package controllers.social

import play.api.libs.oauth.ConsumerKey._
import play.api.libs.oauth.OAuth._
import play.api.libs.oauth.ServiceInfo._
import play.api.mvc.Action._
import play.api.mvc.Results._
import play.api.libs.oauth.RequestToken._
import play.api.mvc.AsyncResult._
import play.api.libs.oauth.OAuthCalculator._
import play.api.libs.ws._

import java.net.URLEncoder
import play.api.mvc.{Action, AsyncResult, RequestHeader, Controller}
import play.api.libs.oauth._

/**
 * Created by IntelliJ IDEA.
 * User: alabbe
 * Date: 07/02/12
 * Time: 18:25
 * To change this template use File | Settings | File Templates.
 */


object Twitter extends Controller {
   // OAuth

   val KEY = ConsumerKey("WsjQdxMVZ3Nasnhk1y2uDQ",
      "BNFz31YmhR63pL28kX7tqzgEvLhQQRtZXLjtfgvTZw")

   val TWITTER = OAuth(ServiceInfo(
      "https://api.twitter.com/oauth/request_token",
      "https://api.twitter.com/oauth/access_token",
      "https://api.twitter.com/oauth/authorize", KEY))

   def authenticate = Action {
      request =>
         request.queryString.get("oauth_verifier").flatMap(_.headOption).map {
            verifier =>
               val tokenPair = sessionTokenPair(request).get
               // We got the verifier; now get the access token, store it and back to index
               TWITTER.retrieveAccessToken(tokenPair, verifier) match {
                  case Right(t) => {
                     // We received the unauthorized tokens in the OAuth object - store it before we proceed
                     Redirect(controllers.routes.Application.index).withSession("token" -> t.token, "secret" -> t.secret)
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

   def tweet(status: String) = Action {
      implicit request =>
         val tokens = sessionTokenPair.get

         AsyncResult(
            WS.url("https://api.twitter.com/1/statuses/update.json")
                  .sign(OAuthCalculator(Twitter.KEY, tokens))
                  .post(URLEncoder.encode(status, "utf-8")).map(r => Ok(r.json \ "id"))
         )
   }
}

/*
object LinkedIn {
   def authenticate = Action { implicit request =>
      request.queryString.get("oauth_verifier").flatMap(_.headOption).map { verifier =>
      // We got the verifier; now get the access token, store it and back to index
         LINKEDIN.retrieveAccessToken(tokenPair, verifier) match {
            case Right(t) => {
               // We received the unauthorized tokens in the OAuth object - store it before we proceed
               Redirect(routes.Application.index).withSession("linkedintoken" -> t.token, "linkedinsecret" -> t.secret)
            }
            case Left(e) => {
               Logger.error("Error connecting to LinkedIn: " + e.getMessage);
               Redirect(routes.Authentication.login).flashing("error" -> Messages("linkedin.connect.error.message",e.getMessage))
            }
         }
      }.getOrElse(
         LINKEDIN.retrieveRequestToken("http://localhost:9000/auth") match {
            case Right(t) => {
               // We received the unauthorized tokens in the OAuth object - store it before we proceed
               Redirect(LINKEDIN.redirectUrl(t.token)).withSession("linkedintoken" -> t.token, "linkedinsecret" -> t.secret)
            }
            case Left(e) => {
               Logger.error("Error connecting to LinkedIn: " + e.getMessage);
               Redirect(routes.Authentication.login).flashing("error" -> Messages("linkedin.connect.error.message",e.getMessage))
            }
         }
      )
   }

   def findTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
      for {
         token <- request.session.get("linkedintoken")
         secret <- request.session.get("linkedinsecret")
      } yield {
         RequestToken(token, secret)
      }
   }

   /* This function must be called only inside an Authenticated statement */
   def tokenPair(implicit request: RequestHeader) = findTokenPair.get
}*/



