# ErgoPay Payment Portal

This is a ready to use payment portal application to use for websites and applications to process
payments on the Ergo blockchain. It supports payments both in Ergo (the blockchain's native token)
and every other token on Ergo.

You can deploy this project as it is on a hosting service supporting JVM applications (Heroku, 
Dokku on DigitalOcean, ...) and use its REST API from your website, application, game backend, ... 
But keep in mind that this configuration uses an in-memory db and will 
loose saved information on restart, so you might want
to adapt the configuration. You might also want to check or change Explorer API and 
[Node API URL](https://github.com/MrStahlfelge/ergopay-payment-portal/blob/master/src/main/kotlin/org/ergoplatform/ergopay/paymentportal/service/NodeService.kt) 
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
jump to the  [Service class](https://github.com/MrStahlfelge/ergopay-payment-portal/blob/master/src/main/kotlin/org/ergoplatform/ergopay/paymentportal/service/PaymentService.kt). 

Spring is organized in the following way:

* Model classes define db entities
* Repository classes define the db access
* Controller classes define REST API endpoints
* Service classes define the actual business logic and are singletons.

## The API endpoints
Defined in [PaymentPortalController](https://github.com/MrStahlfelge/ergopay-payment-portal/blob/master/src/main/kotlin/org/ergoplatform/ergopay/paymentportal/rest/PaymentPortalController.kt)

### POST payment/addrequest

Request body is a json-encoded `CreatePaymentRequest` 

```
class CreatePaymentRequest(
    val nanoErg: Long, // nano erg value to pay to the recipient, or min amount if only token should be paid
    val tokenId: String?, // token ID to pay to the recipient, or null if none
    val tokenRawAmount: Long?, // raw amount of token to pay. note it is the raw amount
    val receiverAddress: String, // address to pay to
    val senderAddress: String?, // address to pay from, if known
    val message: String?, // message to attach to the transaction (optional)
)
```

Example body to request 1 ERG:

    {
    "nanoErg": 1000000000,
    "receiverAddress": "9g8gaARC3N8j9v97wmnFkhDMxHHFh9PEzVUtL51FGSNwTbYEnnk"
    }

Example body to request 1 SigUSD:

    {
    "nanoErg": 1000000,
    "receiverAddress": "9g8gaARC3N8j9v97wmnFkhDMxHHFh9PEzVUtL51FGSNwTbYEnnk",
    "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
    "tokenRawAmount": 100
    }

The request will respond with the request ID and an ErgoPay URL:

    {
    "requestId": "MSWHPMIDDZ",
    "ergoPayUrl": "ergopay://localhost:8080/payment/getrequest/MSWHPMIDDZ?sender=#P2PK_ADDRESS#"
    }

Use the request ID for calls to the state endpoint (see below). The `ergoPayUrl` is the link you 
should present your user by QR code and as a button-type link in case the user has an ErgoPay-compatible
wallet application installed on the device he is using your website/app with.

### GET payment/state/{requestId}

Use this to retrieve the state of the transaction. A response will look like this:

    {
    "requestId": "MSWHPMIDDZ",
    "paymentRequestState": "WAITING",
    "txId": "4bd18c0ddf5cfd2be6885c8e111bf75f28c4c5dcc5961f3b83a5141a047928df"
    }

Transaction ID is the Ergo transaction ID that can be reviewed in Explorer. Note that this transaction 
ID is set before the transaction is executed.

PaymentRequestState is one of the following states:

    CREATED, // request sent by dapp, but no ergopay request from wallet app
    WAITING, // ergopay request sent to wallet app and waiting to be submitted and included into a block
    EXECUTED, // payment included into a block
    INVALID, // ergopay request sent to wallet app but impossible to be included into a block


Please note that request IDs will be purged after some time (default configuration 30 minutes), so 
make sure to persist the data on your side.