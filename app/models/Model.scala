package models

import play.api.libs.json._
import play.api.libs.json.Json
import com.mongodb.casbah.Imports._
import db.db.Mongo


/**
 * Created by IntelliJ IDEA.
 * User: alabbe
 * Date: 27/01/12
 * Time: 11:15
 * To change this template use File | Settings | File Templates.
 */

object Model {
   
   case class Signer(email:String, code:String, firstName:Option[String] = None, lastName:Option[String] = None , age:Option[Int] = None, city:Option[String] = None) {

      override def toString():String = {
         Some(firstName,lastName).map{ u =>
            u._1.map(f => f.head.toUpper + f.tail.toLowerCase) + " " + u._2.map(l => l.head.toUpper + ".")
         }.getOrElse("")
      }
   }
   object Signer extends Mongo("signers"){
      def create(signer:Signer):Signer = {
         val mongoSigner = MongoDBObject.newBuilder
         mongoSigner += "email" -> signer.email
         mongoSigner += "code" -> signer.code
         signer.firstName.map( x => mongoSigner += "firstName" -> x)
         signer.lastName.map( x => mongoSigner += "lastName" -> x)
         signer.city.map( x => mongoSigner += "city" -> x)
         signer.age.map( x => mongoSigner += "age" -> x)

         insert(mongoSigner.result)
         signer
      }

      def byCode(code:String):Option[Signer] = {
         val query  = MongoDBObject("code" -> code)
         val result = selectOne(query)
         (for {
            r <- result
            email <- r.getAs[String]("email")
            code <- r.getAs[String]("code")
         } yield(Signer(email, code))).orElse(None)
      }
   }

   implicit object SignerFormat extends Format[Signer] {
      def reads(json: JsValue) = Signer(
         (json \ "email").as[String],
         (json \ "code").as[String],
         (json \ "firstName").asOpt[String],
         (json \ "lastName").asOpt[String],
         (json \ "age").asOpt[Int],
         (json \ "city").asOpt[String]
      )
      def writes(o: Signer):JsValue = JsObject(List(
         "fullName" -> JsString(o.toString()),
         "city" -> o.firstName.map(JsString(_)).getOrElse(JsNull)
      ))
   }

}