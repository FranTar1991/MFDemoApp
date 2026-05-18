package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.HostSaleResponse
import com.franktardencilla.mfdemoapp.domain.model.Iso8583Message

interface HostClient {
    suspend fun authorizeSale(requestMessage: Iso8583Message): HostSaleResponse
}
