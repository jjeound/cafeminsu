package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// 점주 메뉴 쓰기 작업(생성·판매토글·이미지 업로드)은 인증이 필요하다.
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

    // 메뉴 대표 이미지 업로드(점주 전용). multipart/form-data, 파트명 file.
    // 반환된 imageUrl 을 메뉴 등록(createMenu)의 imageUrl 로 사용한다.
    @Multipart
    @POST("api/images/menu")
    suspend fun uploadMenuImage(
        @Part file: MultipartBody.Part,
    ): ImageUploadRes
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

@JsonClass(generateAdapter = true)
data class ImageUploadRes(
    val imageUrl: String?,
)
