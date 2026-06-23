package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

// 점주 메뉴 쓰기 작업(생성·판매토글)은 인증이 필요하다.
// 목록 조회는 public 인 기존 `MenuApi.listByStore` 를 재사용한다(중복 정의 금지).
interface OwnerMenuApi {
    @POST("api/stores/{storeId}/menus")
    suspend fun createMenu(
        @Path("storeId") storeId: Long,
        @Body request: MenuCreateReq,
    ): MenuCreateRes

    @PATCH("api/menus/{menuId}/availability")
    suspend fun setAvailability(
        @Path("menuId") menuId: Long,
        @Body request: MenuAvailabilityReq,
    )
}

@JsonClass(generateAdapter = true)
data class MenuCreateReq(
    val name: String,
    val description: String,
    val price: Int,
    val category: String,
    val imageUrl: String?,
    val isAvailable: Boolean,
)

@JsonClass(generateAdapter = true)
data class MenuCreateRes(
    val menuId: Long?,
)

@JsonClass(generateAdapter = true)
data class MenuAvailabilityReq(
    val isAvailable: Boolean,
)
