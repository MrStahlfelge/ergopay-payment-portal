package org.ergoplatform.ergopay.paymentportal.model

import javax.persistence.*

@Entity
@Table(
    indexes = [
        Index(name = "requestId", columnList = "requestId"),
        Index(name = "txId", columnList = "txId"),
        Index(name = "keepUntilMs", columnList = "keepUntilMs"),
    ]
)

class PaymentRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long, // DB ID
    val requestId: String, // Public Request ID to use with Rest Requests
    val nanoErg: Long, // nano erg value to pay to the recipient, or 0 if only token should be paid
    val tokenId: String?, // token ID to pay to the recipient, or null if none
    val tokenRawAmount: Long?, // raw amount of token to pay. note it is the raw amount
    val receiverAddress: String, // address to pay to
    val senderAddress: String?, // address to pay from, if known
    val message: String?, // message to attach to the transaction (optional)
    @Column(length=5000)
    val reducedTx: ByteArray?, // reduced tx, when already generated
    val txId: String?, // tx id of reduced ty, when already generated
    val state: Int, // last known state of this transaction
    val keepUntilMs: Long, // timestamp when this request can be purged
)