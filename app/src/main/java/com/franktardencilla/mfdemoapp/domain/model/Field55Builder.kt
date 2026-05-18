package com.franktardencilla.mfdemoapp.domain.model

object Field55Builder {
    private val field55TagOrder = listOf(
        "9F02",
        "5F2A",
        "95",
        "9A",
        "9C",
        "9F26",
        "9F27",
        "9F10",
        "4F",
        "84",
        "5F24"
    )

    fun build(summary: EmvTagSummary): Field55Data {
        val tagsByName = summary.tags.associateBy { tag ->
            tag.tag.uppercase()
        }
        val includedTags = field55TagOrder.mapNotNull { tagName ->
            tagsByName[tagName]?.takeIf { tag ->
                tag.value.isHexByteString()
            }
        }

        require(includedTags.isNotEmpty()) {
            "Field 55 requires at least one valid EMV TLV tag."
        }

        return Field55Data(
            tlvHex = includedTags.joinToString(separator = "") { tag ->
                "${tag.tag.uppercase()}${tag.value.tlvLengthHex()}${tag.value.uppercase()}"
            },
            includedTags = includedTags.map { tag -> tag.tag.uppercase() }
        )
    }

    private fun String.isHexByteString(): Boolean {
        return length % 2 == 0 && HEX_PATTERN.matches(this)
    }

    private fun String.tlvLengthHex(): String {
        val byteLength = length / 2
        require(byteLength <= MAX_SINGLE_BYTE_LENGTH) {
            "Long-form TLV lengths are not supported yet."
        }
        return byteLength.toString(radix = 16).padStart(2, '0').uppercase()
    }

    private const val MAX_SINGLE_BYTE_LENGTH = 127
    private val HEX_PATTERN = Regex("^[0-9A-Fa-f]+$")
}
