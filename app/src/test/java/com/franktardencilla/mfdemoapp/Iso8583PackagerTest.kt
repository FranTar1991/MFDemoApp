package com.franktardencilla.mfdemoapp

import com.franktardencilla.mfdemoapp.domain.model.Iso8583Message
import com.franktardencilla.mfdemoapp.domain.model.Iso8583Packager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Iso8583PackagerTest {
    @Test
    fun packFrame_buildsSimulatorCompatibleSaleFrame() {
        val message = Iso8583Message(
            mti = "0200",
            fields = mapOf(
                3 to "000000",
                4 to "000000001234",
                11 to "000123",
                22 to "051",
                24 to "001",
                41 to "DEMO920",
                42 to "MFDemoMerchant",
                49 to "840",
                55 to "9F0206000000001234",
                64 to "A1B2C3D4E5F60708"
            )
        )

        val frame = Iso8583Packager.packFrame(message)
        val bodyLength = ((frame[0].toInt() and 0xFF) shl 8) or (frame[1].toInt() and 0xFF)
        val text = frame.decodeToString()

        assertEquals(frame.size - 2, bodyLength)
        assertTrue(text.contains("0200"))
        assertTrue(text.contains("000000"))
        assertTrue(text.contains("000000001234"))
        assertTrue(text.contains("000123"))
        assertTrue(text.contains("0189F0206000000001234"))
        assertTrue(text.contains("A1B2C3D4E5F60708"))
    }

    @Test
    fun pack_setsBitmapForRequiredSaleFieldsIncludingIccAndMac() {
        val message = Iso8583Message(
            mti = "0200",
            fields = mapOf(
                3 to "000000",
                4 to "000000001234",
                7 to "0520143540",
                11 to "000123",
                12 to "143540",
                13 to "0520",
                22 to "051",
                24 to "001",
                25 to "00",
                41 to "DEMO920",
                42 to "MFDemoMerchant",
                49 to "840",
                55 to "9F0206000000001234",
                64 to "A1B2C3D4E5F60708"
            )
        )

        val payload = Iso8583Packager.pack(message).decodeToString()

        assertEquals("0200", payload.substring(0, 4))
        assertEquals("3238058000C08201", payload.substring(4, 20))
        assertTrue(payload.contains("0189F0206000000001234"))
        assertTrue(payload.endsWith("A1B2C3D4E5F60708"))
    }

    @Test
    fun unpackFrame_readsHostAuthorizationResponseFields() {
        val response = Iso8583Message(
            mti = "0210",
            fields = mapOf(
                3 to "000000",
                4 to "000000001234",
                11 to "000123",
                37 to "12345678    ",
                38 to "654321",
                39 to "00",
                41 to "DEMO920",
                49 to "840"
            )
        )

        val unpacked = Iso8583Packager.unpackFrame(
            Iso8583Packager.packFrame(response)
        )

        assertEquals("0210", unpacked.mti)
        assertEquals("000123", unpacked.get(11))
        assertEquals("654321", unpacked.get(38))
        assertEquals("00", unpacked.get(39))
    }

    @Test
    fun unpackFrame_rejectsFrameWithInvalidLengthHeader() {
        val response = Iso8583Message(
            mti = "0210",
            fields = mapOf(
                3 to "000000",
                11 to "000123",
                39 to "00"
            )
        )
        val frame = Iso8583Packager.packFrame(response).copyOf()
        frame[1] = (frame[1] + 1).toByte()

        assertThrows(IllegalArgumentException::class.java) {
            Iso8583Packager.unpackFrame(frame)
        }
    }

    @Test
    fun unpack_rejectsMalformedVariableLengthField() {
        val payload = "020040000000000000001A".toByteArray(Charsets.ISO_8859_1)

        assertThrows(RuntimeException::class.java) {
            Iso8583Packager.unpack(payload)
        }
    }
}
