package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.CardEntryMode

interface EmvCardReader {
    suspend fun searchCard(
        entryModes: List<CardEntryMode>,
        timeoutSeconds: Int
    ): CardSearchResult

    suspend fun stopSearch()
}

sealed interface CardSearchResult {
    data class Detected(val card: DetectedCard) : CardSearchResult
    data class Failed(val message: String) : CardSearchResult
    data object Timeout : CardSearchResult
    data object Canceled : CardSearchResult
}

data class DetectedCard(
    val entryMode: CardEntryMode,
    val description: String
)
