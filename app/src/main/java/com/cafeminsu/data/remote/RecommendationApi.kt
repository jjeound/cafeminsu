package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

interface RecommendationApi {
    // 라이브 스펙(/v3/api-docs): 인증(Bearer) 필요. 추천 항목은 menuId 만 표시에 쓸 수 있으므로
    // 메뉴 상세(`api/menus/{menuId}`)로 보강한다.
    @GET("api/stores/{storeId}/recommendations/today")
    suspend fun getTodayRecommendation(
        @Path("storeId") storeId: Long,
    ): TodayRecommendationRes
}

@JsonClass(generateAdapter = true)
data class TodayRecommendationRes(
    val recommendations: List<RecommendationItemRes>?,
)

// 라이브 스펙의 추천 항목은 제네릭 `Item{menuId, quantity, optionIds}` 스키마다.
// 표시 정보(이름·가격 등)는 menuId 로 메뉴 상세를 조회해 보강한다.
@JsonClass(generateAdapter = true)
data class RecommendationItemRes(
    val menuId: Long?,
    val quantity: Int? = null,
    val optionIds: List<Long>? = null,
)
