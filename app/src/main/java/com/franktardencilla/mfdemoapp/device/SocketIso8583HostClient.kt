package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.HostSaleResponse
import com.franktardencilla.mfdemoapp.domain.model.Iso8583Message
import com.franktardencilla.mfdemoapp.domain.model.Iso8583Packager
import com.franktardencilla.mfdemoapp.repository.HostConfigRepository
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SocketIso8583HostClient(
    private val hostConfigRepository: HostConfigRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HostClient {
    override suspend fun authorizeSale(requestMessage: Iso8583Message): HostSaleResponse {
        return withContext(ioDispatcher) {
            val hostConfig = hostConfigRepository.getHostConfig()
            val requestFrame = Iso8583Packager.packFrame(requestMessage)
            val errors = mutableListOf<String>()
            for (host in hostConfig.hosts) {
                val response = runCatching {
                    authorizeWithHost(
                        host = host,
                        port = hostConfig.port,
                        timeoutMillis = hostConfig.timeoutMillis,
                        requestFrame = requestFrame,
                        requestMessage = requestMessage
                    )
                }.getOrElse { error ->
                    errors += "$host:${hostConfig.port} ${error.message.orEmpty()}".trim()
                    null
                }
                if (response != null) {
                    return@withContext response
                }
            }
            error(
                "Tried ${hostConfig.hosts.joinToString { "$it:${hostConfig.port}" }}. " +
                    errors.joinToString(separator = " | ")
            )
        }
    }

    private fun authorizeWithHost(
        host: String,
        port: Int,
        timeoutMillis: Int,
        requestFrame: ByteArray,
        requestMessage: Iso8583Message
    ): HostSaleResponse {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMillis)
            socket.soTimeout = timeoutMillis
            socket.getOutputStream().use { output ->
                socket.getInputStream().use { input ->
                    output.write(requestFrame)
                    output.flush()

                    val lengthBytes = input.readExact(RESPONSE_LENGTH_BYTES)
                    val responseLength = ((lengthBytes[0].toInt() and 0xFF) shl 8) or
                        (lengthBytes[1].toInt() and 0xFF)
                    val responseBody = input.readExact(responseLength)
                    val responseMessage = Iso8583Packager.unpackFrame(lengthBytes + responseBody)

                    return HostSaleResponse(
                        requestSummary = requestMessage.toSummary(),
                        responseSummary = responseMessage.toSummary(),
                        responseMessage = responseMessage
                    )
                }
            }
        }
    }

    private fun java.io.InputStream.readExact(byteCount: Int): ByteArray {
        val buffer = ByteArray(byteCount)
        var offset = 0
        while (offset < byteCount) {
            val read = read(buffer, offset, byteCount - offset)
            if (read < 0) {
                error("Host closed the connection before sending a complete response.")
            }
            offset += read
        }
        return buffer
    }

    private fun Iso8583Message.toSummary() = com.franktardencilla.mfdemoapp.domain.model.IsoMessageSummary(
        mti = mti,
        stan = get(11),
        responseCode = get(39),
        authCode = get(38),
        redactedMessage = fields.toSortedMap().entries.joinToString(separator = " | ") { (field, value) ->
            "F${field.toString().padStart(3, '0')}=${redactField(field, value)}"
        }
    )

    private fun redactField(
        field: Int,
        value: String
    ): String {
        return when (field) {
            2, 35, 45, 52, 55, 64, 128 -> "[redacted length=${value.length}]"
            else -> value
        }
    }

    private companion object {
        const val RESPONSE_LENGTH_BYTES = 2
    }
}
