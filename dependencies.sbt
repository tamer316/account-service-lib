resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= {
  Seq(
    "com.github.tamer316" %% "microservice-lib" % "1.0.1",
    "com.github.tamer316" %% "email-service-client" % "1.0.0",
    "de.svenkubiak" % "jBCrypt" % "0.4",
    "com.github.tamer316" %% "microservice-lib" % "1.0.1" classifier "tests"
  )
}

Revolver.settings