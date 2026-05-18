package com.franktardencilla.mfdemoapp.domain.model

data class HostConfig(
    val primaryHost: String = DEFAULT_PRIMARY_HOST,
    val fallbackHost: String = "",
    val port: Int = DEFAULT_PORT,
    val timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS
) {
    val hosts: List<String>
        get() = listOf(primaryHost, fallbackHost)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

    init {
        require(primaryHost.isNotBlank()) {
            "Primary host is required."
        }
        require(port in MIN_PORT..MAX_PORT) {
            "Port must be between $MIN_PORT and $MAX_PORT."
        }
        require(timeoutMillis in MIN_TIMEOUT_MILLIS..MAX_TIMEOUT_MILLIS) {
            "Timeout must be between 1000 and 30000 ms."
        }
    }

    companion object {
        const val DEFAULT_PRIMARY_HOST = "10.0.2.2"
        const val DEFAULT_PORT = 8001
        const val DEFAULT_TIMEOUT_MILLIS = 5000
        private const val MIN_PORT = 1
        private const val MAX_PORT = 65535
        private const val MIN_TIMEOUT_MILLIS = 1000
        private const val MAX_TIMEOUT_MILLIS = 30000
    }
}
