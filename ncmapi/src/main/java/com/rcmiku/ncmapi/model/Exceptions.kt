package com.rcmiku.ncmapi.model

class NcmApiException(
    val code: Int,
    override val message: String = "NCM API error code: $code",
    val endpoint: String? = null,
    val retryable: Boolean = (code == 429 || code in 500..599)
) : RuntimeException(message)

class NcmHttpException(
    val statusCode: Int,
    override val message: String = "HTTP $statusCode error",
    val endpoint: String? = null,
    val retryable: Boolean = (statusCode == 429 || statusCode in 500..599)
) : RuntimeException(message)
