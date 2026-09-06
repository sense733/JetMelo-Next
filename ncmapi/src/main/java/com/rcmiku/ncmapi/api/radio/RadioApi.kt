package com.rcmiku.ncmapi.api.radio

import com.rcmiku.ncmapi.model.ProgramRadioResponse
import com.rcmiku.ncmapi.model.RadioInfoResponse

/**
 * 电台相关 API 接口。
 *
 * 当前为 Fail-Fast 占位桩，未接驳远端真实网络协议，调用均直接返回 [Result.failure]。
 * 调用方须显式处理失败场景；上层 ProgramRadioScreenViewModel 与 RadioPagingSource 已集成错误流与降级重试机制。
 */
object RadioApi {

    /**
     * 获取电台详情信息。
     *
     * @param radioId 电台 ID
     * @return [Result.failure] 占位异常
     */
    suspend fun radioInfo(radioId: Long): Result<RadioInfoResponse> {
        return Result.failure(IllegalStateException("Radio API not implemented"))
    }

    /**
     * 获取电台节目分页列表。
     *
     * @param radioId 电台 ID
     * @param limit 分页拉取大小，默认 30
     * @param offset 列表偏移量，默认 0
     * @return [Result.failure] 占位异常
     */
    suspend fun programRadio(radioId: Long, limit: Int = 30, offset: Int = 0): Result<ProgramRadioResponse> {
        return Result.failure(IllegalStateException("Radio API not implemented"))
    }
}
