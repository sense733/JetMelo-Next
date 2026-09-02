package com.rcmiku.ncmapi.api.radio

import com.rcmiku.ncmapi.model.ProgramRadioResponse
import com.rcmiku.ncmapi.model.RadioInfoResponse

object RadioApi {
    suspend fun programRadio(radioId: Long, limit: Int = 30, offset: Int = 0): Result<ProgramRadioResponse> {
        return Result.failure(IllegalStateException("Radio API not implemented"))
    }

    suspend fun radioInfo(radioId: Long): Result<RadioInfoResponse> {
        return Result.failure(IllegalStateException("Radio API not implemented"))
    }
}
