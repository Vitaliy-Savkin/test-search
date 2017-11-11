import scala.language.postfixOps

lazy val commonSettings: Seq[Setting[_]] =
  Seq(
    organization := "com.playtech.bit",
    scalaVersion := "2.12.3",
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-http-core" % "10.0.10",
			"de.heikoseeberger" %% "akka-http-play-json" % "1.18.1")
  )

lazy val `test-search` = project
  .in(file("."))
	.disablePlugins(AssemblyPlugin)
  .aggregate(client, master, worker, test)

lazy val client = project
	.settings(
		commonSettings
	)

lazy val master = project
	.dependsOn(client)
	.settings(
		commonSettings
	)

lazy val worker = project
	.dependsOn(client)
	.settings(
	  commonSettings
	)

lazy val test = project
  .dependsOn(client, master, worker)
  .disablePlugins(AssemblyPlugin)
  .configs(IntegrationTest)
	.settings(
	  Defaults.itSettings ++ commonSettings,
		libraryDependencies ++= Seq(
		  "org.scalatest" %% "scalatest" % "3.0.4" % "it"
		)
	)


