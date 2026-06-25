package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.data.mapper.toMenuItem
import com.cafeminsu.data.remote.MenuApi
import com.cafeminsu.data.remote.RecommendationApi
import com.cafeminsu.data.remote.Unauthenticated
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.repository.RecommendationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Singleton
class RealRecommendationRepository @Inject constructor(
    private val recommendationApi: RecommendationApi,
    @Unauthenticated
    private val menuApi: MenuApi,
    private val selectedStoreHolder: SelectedStoreHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecommendationRepository {
    override fun observeTodayRecommendation(): Flow<AppResult<MenuItem?>> =
        flow { emit(loadRecommendation()) }.flowOn(ioDispatcher)

    private suspend fun loadRecommendation(): AppResult<MenuItem?> {
        // 추천은 매장 종속이다. 선택 매장이 없으면 호출하지 않고 null 로 폴백한다.
        val storeId = selectedStoreHolder.current()?.id?.toLongOrNull()
            ?: return AppResult.Success(null)

        // 추천 응답은 menuId 만 제공하므로 표시 정보는 메뉴 상세로 보강한다(첫 추천 1건).
        val menuId = when (
            val response = runCatchingToAppResult {
                recommendationApi.getTodayRecommendation(storeId)
            }
        ) {
            is AppResult.Success ->
                response.data.recommendations
                    .orEmpty()
                    .firstNotNullOfOrNull { it.menuId }
                    ?: return AppResult.Success(null)
            is AppResult.Failure -> return response
        }

        return when (
            val response = runCatchingToAppResult { menuApi.getMenu(menuId) }
        ) {
            is AppResult.Success -> response.data.toMenuItem()
            is AppResult.Failure -> response
        }
    }
}
