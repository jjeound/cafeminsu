package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.data.mock.MockData
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.repository.RecommendationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class MockRecommendationRepository @Inject constructor() : RecommendationRepository {
    // 현 동작과 유사하게 대표(첫 비품절) 메뉴 1건을 추천으로 노출한다.
    override fun observeTodayRecommendation(): Flow<AppResult<MenuItem?>> =
        flowOf(AppResult.Success(MockData.menuItems.firstOrNull { !it.isSoldOut }))
}
