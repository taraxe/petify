import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "petify"
    val appVersion      = "1.0"

   val appDependencies = Seq(
      "com.mongodb.casbah" % "casbah_2.9.0-1" % "2.1.5.0"
      // Add your project dependencies here,
   )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
