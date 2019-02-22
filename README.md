Account Service Lib
===================
Application built with the following (main) technologies:

* Scala
* SBT
* Akka Http
* Specs2

Description
-----------
A Scala library for building an Account Microservice with the following features:

* Signup
* Login
* Change password
* Reset password using token link sent via email

Prerequisites
-------------
The following applications are installed and running:

* [Scala 2.11.8](http://www.scala-lang.org/)
* [SBT](http://www.scala-sbt.org/)
    - For Mac:
      ```
      brew install sbt
      ```
* [MongoDB](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-os-x/)
    - For Mac:
    ```
    brew install mongodb
    ```
      
Testing
---------
- Run Unit tests
  ```
  sbt test
  ```
- Run Integration tests
  ```
  sbt it:test
  ```
  
- Run all tests
  ```
  sbt test it:test
  ```
  
- Run one test
  ```
  sbt test-only *AccountRoutesSpec
  ```

Code Coverage
-------------
SBT-scoverage a SBT auto plugin: https://github.com/scoverage/sbt-scoverage
- Run tests with coverage enabled by entering:
  ```
  sbt clean coverage test
  ```

After the tests have finished, find the coverage reports inside target/scala-2.11/scoverage-report