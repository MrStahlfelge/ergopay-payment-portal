package org.ergoplatform.ergopay.paymentportal.service

import org.ergoplatform.appkit.*
import org.ergoplatform.ergopay.paymentportal.model.PaymentRequest
import org.ergoplatform.ergopay.paymentportal.repository.PaymentRequestRepo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class PaymentService(
    private val paymentRequestRepo: PaymentRequestRepo,
    private val nodeService: NodeService,
    private val explorerApiService: ExplorerApiService,
) {
    val waitTimeCreatedRequests = 1000L * 60 * 30 // 30 minutes
    val minNanoErgValue = Parameters.MinChangeValue
    val numConfirmationsNeeded =
        2 // number of confirmations for a transaction to be regarded executed

    fun addPaymentRequest(createPaymentRequest: CreatePaymentRequest): String {
        // check if payment request is valid
        with(createPaymentRequest) {
            if (nanoErg < minNanoErgValue) {
                throw IllegalArgumentException("nanoErg value must be >= $minNanoErgValue")
            }
            if (tokenId != null && (tokenRawAmount == null || tokenRawAmount <= 0)) {
                throw java.lang.IllegalArgumentException("tokenId set, but tokenRawAmount is null")
            }
            tokenId?.let { ErgoToken(tokenId, tokenRawAmount!!) }
            Address.create(receiverAddress)
            senderAddress?.let { Address.create(senderAddress) }
        }

        // checks done, calc a new ID and save to DB
        val requestId = generateRequestId()
        val paymentRequest = PaymentRequest(
            id = 0,
            requestId,
            createPaymentRequest.nanoErg,
            createPaymentRequest.tokenId,
            createPaymentRequest.tokenRawAmount,
            receiverAddress = createPaymentRequest.receiverAddress,
            senderAddress = createPaymentRequest.senderAddress,
            message = createPaymentRequest.message,
            reducedTx = null,
            txId = null,
            state = PaymentRequestState.CREATED.ordinal,
            keepUntilMs = System.currentTimeMillis() + waitTimeCreatedRequests,
        )

        paymentRequestRepo.save(paymentRequest)

        return requestId
    }

    fun generateRequestId(length: Int = 10): String {
        val chars = ('A'..'Z')
        val rnd = SecureRandom()
        return List(length) { chars.elementAt(rnd.nextInt(chars.count())) }.joinToString("")
    }

    fun getReducedTransactionForRequest(
        requestId: String,
        senderAddressString: String?
    ): PaymentRequest {
        val paymentRequest: PaymentRequest =
            paymentRequestRepo.findByRequestId(requestId).orElse(null)
                ?: throw IllegalArgumentException("No payment request $requestId found.")

        // payment request already created
        if (paymentRequest.reducedTx != null)
            return paymentRequest

        // no payment request yet, check if we have a sender
        if (paymentRequest.senderAddress == null && senderAddressString == null) {
            throw IllegalArgumentException("No sender address known")
        }

        val tokensToSpend = if (paymentRequest.tokenId == null) emptyList()
        else listOf(ErgoToken(paymentRequest.tokenId, paymentRequest.tokenRawAmount!!))

        val senderAddress = Address.create(paymentRequest.senderAddress ?: senderAddressString)

        val reducedTx = nodeService.getErgoClient().execute { ctx ->

            val unsignedTx = BoxOperations
                .createForSender(senderAddress, ctx)
                .withAmountToSpend(paymentRequest.nanoErg)
                .withTokensToSpend(tokensToSpend)
                .buildTxWithDefaultInputs { txB ->
                    val outBoxBuilder: OutBoxBuilder = txB.outBoxBuilder()
                        .value(paymentRequest.nanoErg)
                        .contract(Address.create(paymentRequest.receiverAddress).toErgoContract())

                    if (tokensToSpend.isNotEmpty()) outBoxBuilder.tokens(
                        *tokensToSpend.toTypedArray()
                    )
                    if (paymentRequest.message != null) {
                        outBoxBuilder.registers(*BoxAttachmentPlainText.buildForText(paymentRequest.message).outboxRegistersForAttachment)
                    }
                    val newBox = outBoxBuilder.build()
                    txB.outputs(newBox)

                    txB
                }

            ctx.newProverBuilder().build().reduce(unsignedTx, 0)
        }

        val serializedReducedTx = reducedTx.toBytes()
        val savedPaymentRequest = PaymentRequest(
            id = paymentRequest.id,
            requestId = paymentRequest.requestId,
            nanoErg = paymentRequest.nanoErg,
            tokenId = paymentRequest.tokenId,
            tokenRawAmount = paymentRequest.tokenRawAmount,
            receiverAddress = paymentRequest.receiverAddress,
            senderAddress = senderAddress.toString(),
            message = paymentRequest.message,
            reducedTx = serializedReducedTx,
            txId = reducedTx.id,
            state = PaymentRequestState.WAITING.ordinal,
            keepUntilMs = System.currentTimeMillis() + waitTimeCreatedRequests,
        )
        paymentRequestRepo.save(savedPaymentRequest)

        return savedPaymentRequest
    }

    fun getPaymentState(requestId: String): PaymentRequestStateResponse {
        val paymentRequest: PaymentRequest =
            paymentRequestRepo.findByRequestId(requestId).orElse(null)
                ?: throw IllegalArgumentException("No payment request $requestId found.")

        return when (paymentRequest.state) {
            PaymentRequestState.CREATED.ordinal -> {
                // user did not fetch the reduced tx yet
                PaymentRequestStateResponse(requestId, PaymentRequestState.CREATED, null)
            }
            PaymentRequestState.WAITING.ordinal -> {
                // we are waiting - that means we should check if transaction was executed
                // or if it was invalidated

                // is transaction executed?
                val txConfirmed = try {
                    val txInfo = explorerApiService.getTransactionInfo(paymentRequest.txId!!)
                    txInfo.numConfirmations >= numConfirmationsNeeded
                } catch (t: Throwable) {
                    false
                }

                val invalid = if (txConfirmed) false else {
                    // check if inputs are still valid
                    try {
                        val coldClient =
                            ColdErgoClient(NetworkType.MAINNET, Parameters.ColdClientMaxBlockCost)
                        coldClient.execute { ctx ->
                            val reducedTx = ctx.parseReducedTransaction(paymentRequest.reducedTx)
                            reducedTx.inputBoxesIds.forEach { boxId ->
                                if (explorerApiService.getBoxInformation(boxId).spentTransactionId != null)
                                    return@execute true
                            }
                            return@execute false
                        }
                    } catch (t: Throwable) {
                        // in case of an error, we don't know more than before
                        false
                    }
                }

                val newRequestState = if (txConfirmed) PaymentRequestState.EXECUTED
                else if (invalid) PaymentRequestState.INVALID
                else PaymentRequestState.WAITING

                paymentRequestRepo.save(
                    PaymentRequest(
                        id = paymentRequest.id,
                        requestId = paymentRequest.requestId,
                        nanoErg = paymentRequest.nanoErg,
                        tokenId = paymentRequest.tokenId,
                        tokenRawAmount = paymentRequest.tokenRawAmount,
                        receiverAddress = paymentRequest.receiverAddress,
                        senderAddress = paymentRequest.senderAddress,
                        message = paymentRequest.message,
                        reducedTx = paymentRequest.reducedTx,
                        txId = paymentRequest.txId,
                        state = newRequestState.ordinal,
                        keepUntilMs = System.currentTimeMillis() + waitTimeCreatedRequests,
                    )
                )

                return PaymentRequestStateResponse(
                    requestId,
                    newRequestState,
                    paymentRequest.txId
                )
            }
            PaymentRequestState.EXECUTED.ordinal -> {
                return PaymentRequestStateResponse(
                    requestId,
                    PaymentRequestState.EXECUTED,
                    paymentRequest.txId
                )
            }
            else -> {
                // Invalid or something that should not be here
                return PaymentRequestStateResponse(
                    requestId,
                    PaymentRequestState.INVALID,
                    paymentRequest.txId
                )
            }
        }
    }

    @Scheduled(fixedRate = 1000L * 60 * 10, initialDelay = 1000L * 60)
    fun deleteOldRequests() {
        // purge old payment requests from DB to keep it small
        paymentRequestRepo.deleteByKeepUntilMsLessThan(System.currentTimeMillis())
    }
}

class CreatePaymentRequest(
    val nanoErg: Long, // nano erg value to pay to the recipient, or minNanoErgValue if only token should be paid
    val tokenId: String?, // token ID to pay to the recipient, or null if none
    val tokenRawAmount: Long?, // raw amount of token to pay. note it is the raw amount
    val receiverAddress: String, // address to pay to
    val senderAddress: String?, // address to pay from, if known
    val message: String?, // message to attach to the transaction (optional)
)

class PaymentRequestStateResponse(
    val requestId: String,
    val paymentRequestState: PaymentRequestState,
    val txId: String?,
)

enum class PaymentRequestState {
    CREATED, // request sent by dapp, but no ergopay request from wallet app
    WAITING, // ergopay request sent to wallet app and waiting to be submitted and included into a block
    EXECUTED, // payment included into a block
    INVALID, // ergopay request sent to wallet app but impossible to be included into a block
}