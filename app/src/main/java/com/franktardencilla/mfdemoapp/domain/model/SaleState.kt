package com.franktardencilla.mfdemoapp.domain.model

enum class SaleState(
    val displayName: String
) {
    IDLE("Idle"),
    CHECKING_READINESS("Checking readiness"),
    WAITING_FOR_CARD("Waiting for card"),
    CARD_DETECTED("Card detected"),
    READING_EMV("Reading EMV"),
    EMV_DATA_READY("EMV data ready"),
    WAITING_FOR_HOST("Waiting for host"),
    APPROVED("Approved"),
    DECLINED("Declined"),
    CANCELED("Canceled"),
    ERROR("Error")
}
