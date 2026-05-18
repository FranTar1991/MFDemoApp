package com.franktardencilla.mfdemoapp.domain.model

enum class CardEntryMode(
    val displayName: String,
    val posEntryMode: String
) {
    CONTACT("Contact chip", "051"),
    CONTACTLESS("Contactless", "071"),
    CONTACTLESS_MAGSTRIPE("Contactless magstripe", "911"),
    MAGSTRIPE("Magstripe", "022"),
    MANUAL("Manual", "012"),
    FALLBACK("Fallback", "802")
}
