package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OwnerOrderApi {
    @GET("api/stores/my")
    suspend fun getMyStores(): List<MyStoreRes>

    @GET("api/stores/{storeId}/orders")
    suspend fun getStoreOrders(
        @Path("storeId") storeId: Long,
        @Query("status") status: String? = null,
        @Query("date") date: String? = null,
    ): List<StoreOrderItemRes>

    @PATCH("api/orders/{orderId}/accept")
    suspend fun acceptOrder(
        @Path("orderId") orderId: Long,
    ): OrderStatusRes?

    @PATCH("api/orders/{orderId}/ready")
    suspend fun readyOrder(
        @Path("orderId") orderId: Long,
    ): OrderStatusRes?

    @PATCH("api/orders/{orderId}/complete")
    suspend fun completeOrder(
        @Path("orderId") orderId: Long,
    ): OrderStatusRes?

    @POST("api/orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Path("orderId") orderId: Long,
        @Body request: OrderCancelReq,
    )
}

@JsonClass(generateAdapter = true)
data class MyStoreRes(
    val id: Long?,
    val name: String?,
    val imageUrl: String?,
)

@JsonClass(generateAdapter = true)
data class StoreOrderItemRes(
    val orderId: Long?,
    val orderNumber: String?,
    val status: String?,
    val totalAmount: Int?,
    val items: List<MenuSummary>?,
    val createdAt: String?,
)

@JsonClass(generateAdapter = true)
data class MenuSummary(
    val menuId: Long?,
    val menuName: String?,
    val quantity: Int?,
)

@JsonClass(generateAdapter = true)
data class OrderStatusRes(
    val status: String?,
)

@JsonClass(generateAdapter = true)
data class OrderCancelReq(
    val reason: String,
)
