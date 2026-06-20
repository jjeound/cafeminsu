package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StoreApi {
    @GET("api/stores")
    suspend fun searchStores(
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = DefaultStorePage,
        @Query("size") size: Int = DefaultStorePageSize,
    ): BaseResponse<StoreSearchRes>

    @GET("api/stores/{storeId}")
    suspend fun getStore(
        @Path("storeId") storeId: Long,
    ): BaseResponse<StoreDetailRes>
}

@JsonClass(generateAdapter = true)
data class StoreSearchRes(
    val stores: List<StoreSearchItem>?,
    val total: Long?,
)

@JsonClass(generateAdapter = true)
data class StoreSearchItem(
    val id: Long?,
    val name: String?,
    val address: String?,
    val imageUrl: String?,
)

@JsonClass(generateAdapter = true)
data class StoreDetailRes(
    val id: Long?,
    val name: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val phone: String?,
    val businessHours: String?,
    val imageUrl: String?,
)

const val DefaultStorePage = 0
const val DefaultStorePageSize = 20
