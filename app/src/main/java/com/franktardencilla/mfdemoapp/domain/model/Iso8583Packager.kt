package com.franktardencilla.mfdemoapp.domain.model

import java.nio.charset.Charset
import java.util.BitSet

object Iso8583Packager {
    private val charset = Charset.forName("ISO-8859-1")

    fun pack(message: Iso8583Message): ByteArray {
        val bitmap = BitSet(129)
        message.fields.keys.forEach { field ->
            bitmap.set(field)
        }
        val hasSecondaryBitmap = message.fields.keys.any { it > 64 }
        if (hasSecondaryBitmap) {
            bitmap.set(1)
        }

        val payload = StringBuilder()
            .append(message.mti)
            .append(bitmapHex(bitmap, 1))
        if (hasSecondaryBitmap) {
            payload.append(bitmapHex(bitmap, 65))
        }

        message.fields.toSortedMap().forEach { (field, rawValue) ->
            val spec = IsoFieldSpec.of(field)
            val value = spec.formatValue(rawValue)
            if (spec.lengthDigits > 0) {
                payload.append(value.length.toString().padStart(spec.lengthDigits, '0'))
            }
            payload.append(value)
        }

        return payload.toString().toByteArray(charset)
    }

    fun packFrame(
        message: Iso8583Message,
        tpduHex: String = DEFAULT_TPDU_HEX
    ): ByteArray {
        val tpdu = hexToBytes(tpduHex)
        val isoPayload = pack(message)
        val body = tpdu + isoPayload
        require(body.size <= 0xFFFF) {
            "ISO8583 frame is too large."
        }
        return byteArrayOf(
            ((body.size shr 8) and 0xFF).toByte(),
            (body.size and 0xFF).toByte()
        ) + body
    }

    fun unpackFrame(frame: ByteArray): Iso8583Message {
        require(frame.size >= 7) {
            "ISO8583 frame must contain 2 length bytes, 5 TPDU bytes, and an ISO payload."
        }
        val length = ((frame[0].toInt() and 0xFF) shl 8) or (frame[1].toInt() and 0xFF)
        require(length == frame.size - 2) {
            "ISO8583 frame length does not match received data."
        }
        return unpack(frame.copyOfRange(7, frame.size))
    }

    fun unpack(payload: ByteArray): Iso8583Message {
        val data = payload.toString(charset)
        require(data.length >= 20) {
            "ISO8583 payload is too short."
        }
        var offset = 0
        val mti = data.substring(offset, offset + 4)
        offset += 4

        val primaryBitmap = data.substring(offset, offset + BITMAP_HEX_LENGTH)
        offset += BITMAP_HEX_LENGTH
        val bitmap = bitmapFromHex(primaryBitmap, 1)
        if (bitmap.get(1)) {
            val secondaryBitmap = data.substring(offset, offset + BITMAP_HEX_LENGTH)
            offset += BITMAP_HEX_LENGTH
            bitmap.or(bitmapFromHex(secondaryBitmap, 65))
        }

        val fields = sortedMapOf<Int, String>()
        for (field in 2..128) {
            if (!bitmap.get(field)) continue
            val spec = IsoFieldSpec.of(field)
            val length = if (spec.lengthDigits > 0) {
                val lengthText = data.substring(offset, offset + spec.lengthDigits)
                offset += spec.lengthDigits
                lengthText.toInt()
            } else {
                spec.fixedLength
            }
            fields[field] = data.substring(offset, offset + length)
            offset += length
        }

        return Iso8583Message(
            mti = mti,
            fields = fields
        )
    }

    private fun bitmapHex(
        bitmap: BitSet,
        startField: Int
    ): String {
        val builder = StringBuilder(BITMAP_HEX_LENGTH)
        for (nibbleStart in startField until startField + 64 step 4) {
            var value = 0
            for (bit in 0..3) {
                if (bitmap.get(nibbleStart + bit)) {
                    value = value or (1 shl (3 - bit))
                }
            }
            builder.append(value.toString(16).uppercase())
        }
        return builder.toString()
    }

    private fun bitmapFromHex(
        hex: String,
        startField: Int
    ): BitSet {
        val bitmap = BitSet(129)
        hex.forEachIndexed { index, character ->
            val value = character.digitToInt(16)
            for (bit in 0..3) {
                if ((value and (1 shl (3 - bit))) != 0) {
                    bitmap.set(startField + index * 4 + bit)
                }
            }
        }
        return bitmap
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0 && hex.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }) {
            "Hex value must contain only complete hex bytes."
        }
        return ByteArray(hex.length / 2) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private data class IsoFieldSpec(
        val fixedLength: Int = 0,
        val lengthDigits: Int = 0
    ) {
        fun formatValue(value: String): String {
            if (lengthDigits > 0) {
                return value
            }
            return value.padEnd(fixedLength).take(fixedLength)
        }

        companion object {
            fun of(field: Int): IsoFieldSpec {
                return when (field) {
                    2, 32, 33, 35, 44, 45 -> IsoFieldSpec(lengthDigits = 2)
                    48, 54, 55, 60, 61, 62, 63, 90, 95, 100, 102, 103, 120, 121, 122, 123 -> {
                        IsoFieldSpec(lengthDigits = 3)
                    }
                    3 -> IsoFieldSpec(fixedLength = 6)
                    4 -> IsoFieldSpec(fixedLength = 12)
                    7 -> IsoFieldSpec(fixedLength = 10)
                    11 -> IsoFieldSpec(fixedLength = 6)
                    12 -> IsoFieldSpec(fixedLength = 6)
                    13 -> IsoFieldSpec(fixedLength = 4)
                    14 -> IsoFieldSpec(fixedLength = 4)
                    22 -> IsoFieldSpec(fixedLength = 3)
                    24 -> IsoFieldSpec(fixedLength = 3)
                    25 -> IsoFieldSpec(fixedLength = 2)
                    37 -> IsoFieldSpec(fixedLength = 12)
                    38 -> IsoFieldSpec(fixedLength = 6)
                    39 -> IsoFieldSpec(fixedLength = 2)
                    41 -> IsoFieldSpec(fixedLength = 8)
                    42 -> IsoFieldSpec(fixedLength = 15)
                    49 -> IsoFieldSpec(fixedLength = 3)
                    52, 64, 128 -> IsoFieldSpec(fixedLength = 16)
                    else -> IsoFieldSpec(lengthDigits = 3)
                }
            }
        }
    }

    private const val BITMAP_HEX_LENGTH = 16
    private const val DEFAULT_TPDU_HEX = "6000000000"
}
