package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.EmvTagSummary
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest

interface EmvProcessor {
    suspend fun readEmvData(request: SaleRequest): EmvTagSummary
}
