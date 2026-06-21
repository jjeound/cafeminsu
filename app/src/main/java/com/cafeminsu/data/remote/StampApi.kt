package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

interface StampApi {
    @GET("api/stamps")
    suspend fun getMyStamps(): BaseResponse<List<StampSummaryRes>>

    @GET("api/stamps/{storeId}")
    suspend fun getStoreStamp(
        @Path("storeId") storeId: Long,
    ): BaseResponse<StampDetailRes>
}

@JsonClass(generateAdapter = true)
data class StampSummaryRes(
    val storeId: Long?,
    val storeName: String?,
    val count: Int?,
)

@JsonClass(generateAdapter = true)
data class StampDetailRes(
    val storeId: Long?,
    val storeName: String?,
    val count: Int?,
    val histories: List<HistoryItem>?,
)

@JsonClass(generateAdapter = true)
data class HistoryItem(
    val earnedCount: Int?,
    val createdAt: String?,
)
