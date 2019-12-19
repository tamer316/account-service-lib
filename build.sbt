name := "account-service-lib"
organization := "dev.tamer"
version := "1.0.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  scalastyleFailOnError := true
)

val unitSuffixes = List("Spec", "Test")
val integrationSuffixes = unitSuffixes.map("Integration" + _)

def integrationTestFilter(name: String): Boolean = integrationSuffixes.exists(name.endsWith)
def unitTestFilter(name: String): Boolean =
  unitSuffixes.exists(s => name.endsWith(s) && !integrationTestFilter(name))

lazy val IntTest = config("it") extend Test

lazy val root = (project in file("."))
  .configs(IntTest)
  .settings(commonSettings: _*)
  .settings(inConfig(IntTest)(Defaults.testTasks): _*)
  .settings(
    testOptions in Test := Seq(Tests.Filter(unitTestFilter)),
    testOptions in IntTest := Seq(Tests.Filter(integrationTestFilter))
  )

parallelExecution in IntTest := false

// Enable publishing the jar produced by `test:package`
publishArtifact in (Test, packageBin) := true

// Enable publishing the test API jar
publishArtifact in (Test, packageDoc) := true

// Enable publishing the test sources jar
publishArtifact in (Test, packageSrc) := true
