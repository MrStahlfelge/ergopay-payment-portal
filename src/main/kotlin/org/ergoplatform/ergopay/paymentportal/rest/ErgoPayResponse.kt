package org.ergoplatform.ergopay.paymentportal.rest

import com.fasterxml.jackson.annotation.JsonInclude

class ErgoPayResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var message: String? = null

    @JsonInclude(JsonInclude.Include.NON_NULL)
    var messageSeverity: Severity? = null

    @JsonInclude(JsonInclude.Include.NON_NULL)
    var address: String? = null

    @JsonInclude(JsonInclude.Include.NON_NULL)
    var reducedTx: String? = null

    @JsonInclude(JsonInclude.Include.NON_NULL)
    var replyTo: String? = null

    enum class Severity {
        NONE, INFORMATION, WARNING, ERROR
    }
}