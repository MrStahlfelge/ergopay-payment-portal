package org.ergoplatform.ergopay.paymentportal.repository

import org.ergoplatform.ergopay.paymentportal.model.PaymentRequest
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface PaymentRequestRepo : PagingAndSortingRepository<PaymentRequest, Long> {
    fun findByRequestId(requestId: String): Optional<PaymentRequest>

    fun findByTxId(txId: String): Optional<PaymentRequest>

    fun deleteByKeepUntilMsLessThan(keepUntilMs: Long): Long
}