package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.MenuItem
import kotlinx.coroutines.flow.Flow

/**
 * 홈(M-01)의 "오늘의 추천" 단일 메뉴를 노출한다. 서버 추천이 없거나(빈 결과) 매장 미선택이면
 * [AppResult.Success] 의 데이터가 `null` 이고, 호출 실패는 [AppResult.Failure] 로 전달된다.
 * 어느 경우든 홈은 기존 메뉴 파생 추천으로 폴백할 수 있다.
 */
interface RecommendationRepository {
    fun observeTodayRecommendation(): Flow<AppResult<MenuItem?>>
}
