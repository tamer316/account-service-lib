resolvers += "jitpack" at "https://jitpack.io"

val microServiceLibV = "1.1.4"

libraryDependencies ++= {
  Seq(
    "com.github.tamer316" % "microservice-lib" % microServiceLibV,
    "com.github.tamer316" %% "email-service-client" % "1.0.0",
    "de.svenkubiak" % "jBCrypt" % "0.4",
    "com.github.tamer316" % "microservice-lib" % microServiceLibV classifier "tests"
  )
}

Revolver.settings
