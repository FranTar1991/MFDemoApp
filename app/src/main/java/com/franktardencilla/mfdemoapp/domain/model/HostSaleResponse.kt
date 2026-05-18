package com.franktardencilla.mfdemoapp.domain.model

data class HostSaleResponse(
    val requestSummary: IsoMessageSummary,
    val responseSummary: IsoMessageSummary,
    val responseMessage: Iso8583Message
) {
    val isApproved: Boolean
        get() = responseSummary.responseCode == APPROVED_RESPONSE_CODE

    private companion object {
        const val APPROVED_RESPONSE_CODE = "00"
    }
}
