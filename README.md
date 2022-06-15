# ErgoPay Payment Portal

This is a ready to use payment portal application to use for websites and applications to process
payments on the Ergo blockchain. It supports payments both in Ergo (the blockchain's native token)
and every other token on Ergo.

You can deploy this project as it is on a hosting service supporting JVM applications (Heroku, 
Dokku on DigitalOcean, ...) and use its REST API from your website, application, game backend, ... 
But keep in mind that this configuration uses an in-memory db and will 
loose saved information on restart (which is not as bad as it sounds, see below), so you might want
to adapt the configuration. You might also want to check or change Explorer API and Node API URL 
constants.

If you already use Spring Boot on your server, you can integrate the Service class into your own 
project and directly use it without going through a REST API.

Or you use it as an example and built up your own code! Clone it and run it locally (Java 11 needed) 
by typing in 

   gradlew bootRun

You can find a deployed version of this service on TokenJay to integrate into your applications
without the need to host it yourself. TODO link to doc

## How the code is organized

If you are not familiar with Spring Boot, but you are most interested in Ergo-related code, directly
jump to the Service class. Spring is organized in the following way:

* Model classes define db entities
* Repository classes define the db access
* Controller classes define REST API endpoints
* Service classes define the actual business logic and are singletons.

