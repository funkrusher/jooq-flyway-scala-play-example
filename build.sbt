import com.typesafe.config.ConfigFactory

name := """jooq-flyway-scala-play-example"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(FlywayPlugin)

scalaVersion := "2.12.8"
val jooqVersion = "3.11.0"

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  guice,
  "org.flywaydb" %% "flyway-play" % "5.2.0",
  "org.mariadb.jdbc" % "mariadb-java-client" % "2.4.3",
  "org.jooq" % "jooq" % jooqVersion,
  "org.jooq" % "jooq-codegen-maven" % jooqVersion,
  "org.jooq" % "jooq-meta" % jooqVersion,
  "org.jooq" % "jooq-scala_2.12" % jooqVersion,
   specs2 % Test
)

//
// JOOQ
//
// generateJOOQ task (can be run from SBT-shell)
val generateJOOQ = taskKey[Seq[File]]("Generate jOOQ classes")
generateJOOQ := {
  val src = sourceManaged.value
  val cp = (dependencyClasspath in Compile).value
  val r = (runner in Compile).value
  val s = streams.value
  r.run(
    "org.jooq.codegen.GenerationTool",
    cp.files,
    Array("conf/testdb.xml"),
    s.log
  ).failed foreach (sys error _.getMessage)
  ((src / "main/generated") ** "*.scala").get
}
unmanagedSourceDirectories in Compile += sourceManaged.value / "main/generated"

//
// FLYWAY
//
// flyway can be forced not to use the compile-time classpath via "filesystem:"-syntax.
lazy val flywayDbConf = settingKey[(String, String, String)]("Typesafe config file with slick settings")
flywayDbConf := {
  val cfg = ConfigFactory.parseFile((resourceDirectory in Compile).value / "application.conf")
  val prefix = s"db.default"
  (cfg.getString(s"$prefix.url"), cfg.getString(s"$prefix.username"), cfg.getString(s"$prefix.password"))
}
flywayUrl := flywayDbConf.value._1
flywayUser := flywayDbConf.value._2
flywayPassword := flywayDbConf.value._3
flywayLocations := Seq("filesystem:conf/db/migration")
