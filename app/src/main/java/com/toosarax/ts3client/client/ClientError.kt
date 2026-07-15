package com.toosarax.ts3client.client

sealed class ClientError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class InvalidInput(message: String) : ClientError(message)
    class PermissionDenied(message: String) : ClientError(message)
    class IdentityFailure(message: String, cause: Throwable? = null) : ClientError(message, cause)
    class DnsFailure(message: String, cause: Throwable? = null) : ClientError(message, cause)
    class Timeout(message: String, cause: Throwable? = null) : ClientError(message, cause)
    class ServerRejection(message: String, cause: Throwable? = null) : ClientError(message, cause)
    class UnsupportedCodec(message: String) : ClientError(message)
    class AudioInitialization(message: String, cause: Throwable? = null) : ClientError(message, cause)
    class ConnectionLoss(message: String, cause: Throwable? = null) : ClientError(message, cause)
    class Unexpected(message: String, cause: Throwable? = null) : ClientError(message, cause)

    companion object {
        fun fromThrowable(throwable: Throwable): ClientError {
            if (looksLikeIdentityRejection(throwable)) {
                return IdentityFailure(IDENTITY_REJECTED)
            }
            val message = throwable.message?.takeIf { it.isNotBlank() } ?: throwable.javaClass.simpleName
            return when (throwable) {
                is java.net.UnknownHostException -> DnsFailure("Could not resolve host", throwable)
                is java.net.SocketTimeoutException -> Timeout("Connection timed out", throwable)
                is java.util.concurrent.TimeoutException -> Timeout("Connection timed out", throwable)
                is java.io.IOException -> ServerRejection(message, throwable)
                else -> Unexpected(message, throwable)
            }
        }

        fun looksLikeIdentityRejection(throwable: Throwable): Boolean {
            var current: Throwable? = throwable
            while (current != null) {
                if (current is ArrayIndexOutOfBoundsException) return true
                val message = current.message.orEmpty()
                if (current is java.util.concurrent.TimeoutException &&
                    message.contains("CONNECTED", ignoreCase = true)
                ) {
                    return true
                }
                current = current.cause
            }
            return false
        }

        const val IDENTITY_REJECTED = "identity_rejected"
        const val IDENTITY_REJECTED_IMPORTED = "identity_rejected_imported"
    }
}
