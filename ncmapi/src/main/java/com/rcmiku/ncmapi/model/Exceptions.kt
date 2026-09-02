package com.rcmiku.ncmapi.model

class NcmApiException(
    val code: Int,
    override val message: String = "NCM API error code: $code"
) : RuntimeException(message)

class NcmHttpException(
    val statusCode: Int,
    override val message: String = "HTTP $statusCode error"
) : RuntimeException(message)
