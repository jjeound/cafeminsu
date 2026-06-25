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
        @Body request: OrderCreateReq,
    ): OrderCreateRes

    @GET("api/orders/{orderId}")
    suspend fun getOrder(
        @Path("orderId") orderId: Long,
    ): OrderDetailRes

    // 목록 응답은 이제 상세(OrderDetailRes, items 포함)와 동일한 형태다 → 단건 보강(getOrder) 불필요.
    @GET("api/orders/my")
    suspend fun getMyOrders(
        @Query("status") status: String? = null,
        @Query("page") page: Int = DefaultOrderPage,
        @Query("size") size: Int = DefaultOrderPageSize,
    ): List<OrderDetailRes>

    // 홈 "다시 주문하기"용 최근 주문 — 역시 OrderDetailRes(items 포함) 배열.
    @GET("api/orders/my/recent")
    suspend fun getRecentOrders(): List<OrderDetailRes>
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

const val DefaultOrderPage = 0
const val DefaultOrderPageSize = 20
