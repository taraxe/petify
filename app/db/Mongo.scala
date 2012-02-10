package db

/**
 * Created by IntelliJ IDEA.
 * User: alabbe
 * Date: 27/01/12
 * Time: 14:11
 * To change this template use File | Settings | File Templates.
 */

package db

import play.Logger
import play.api.{ Configuration, Play }
import play.api.Play.current
import com.mongodb.casbah.Imports._

class Mongo(tableName: String) {
   import Mongo._

   def insert(model: MongoDBObject) = db(tableName) += model

   def update(key: MongoDBObject, model: MongoDBObject) = db(tableName).findAndModify(key, model)

   //def remove(tableName: String, key: MongoDBObject, model: MongoDBObject) = db(tableName).findAndRemove(key, model)

   def count = db(tableName).count

   def clear() = db(tableName).dropCollection()

   def selectAll() = db(tableName).find()

   def selectBy(model: MongoDBObject) = db(tableName).find(model)

   def selectOne(model: MongoDBObject) = db(tableName).findOne(model)
}

object Mongo {

   val DB_NAME = "petify"

   lazy val db = {
      val config   = Configuration.load()
      val host     = config.getString("mongo.host").get
      val port     = config.getInt("mongo.port").get
      val dbName   = config.getString("mongo.db").get
      val username = config.getString("mongo.username").getOrElse("")
      val password = config.getString("mongo.password").getOrElse("")

      val co = MongoConnection(host, port)
      val database = co(dbName)
      database.authenticate(username, password)
      database
   }

   def clearAll() = db.dropDatabase()
}
