package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OrderApi {
    @POST("api/orders")
    suspend fun createOrder(
        @Query("userId") userId: Long,
        @Body request: OrderCreateReq,
    ): BaseResponse<OrderCreateRes>

    @GET("api/orders/{orderId}")
    suspend fun getOrder(
        @Path("orderId") orderId: Long,
        @Query("userId") userId: Long,
    ): BaseResponse<OrderDetailRes>

    @GET("api/orders/my")
    suspend fun getMyOrders(
        @Query("userId") userId: Long,
        @Query("status") status: String? = null,
        @Query("page") page: Int = DefaultOrderPage,
        @Query("size") size: Int = DefaultOrderPageSize,
    ): BaseResponse<List<OrderListItemRes>>
}

@JsonClass(generateAdapter = true)
data class OrderCreateReq(
    val storeId: Long,
    val orderType: String,
    val orderMethod: String,
    val items: List<Item>,
)

@JsonClass(generateAdapter = true)
data class Item(
    val menuId: Long,
    val quantity: Int,
    val optionIds: List<Long>?,
)

@JsonClass(generateAdapter = true)
data class OrderCreateRes(
    val orderId: Long?,
    val orderNumber: String?,
    val totalAmount: Int?,
    val status: String?,
)

@JsonClass(generateAdapter = true)
data class OrderDetailRes(
    val orderId: Long?,
    val orderNumber: String?,
    val storeId: Long?,
    val storeName: String?,
    val orderType: String?,
    val orderMethod: String?,
    val status: String?,
    val totalAmount: Int?,
    val cancelReason: String?,
    val items: List<ItemRes>?,
    val payment: Map<String, Any?>?,
    val createdAt: String?,
)

@JsonClass(generateAdapter = true)
data class ItemRes(
    val menuId: Long?,
    val menuName: String?,
    val quantity: Int?,
    val unitPrice: Int?,
    val options: List<OptionRes>?,
    val subtotal: Int?,
)

@JsonClass(generateAdapter = true)
data class OrderListItemRes(
    val orderId: Long?,
    val orderNumber: String?,
    val storeName: String?,
    val totalAmount: Int?,
    val status: String?,
    val createdAt: String?,
)

const val DefaultOrderPage = 0
const val DefaultOrderPageSize = 20
