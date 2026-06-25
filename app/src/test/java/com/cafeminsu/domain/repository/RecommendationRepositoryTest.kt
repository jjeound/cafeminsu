package com.cafeminsu.domain.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.MenuItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationRepositoryTest {
    @Test
    fun exposesTodayRecommendationContract() = runBlocking {
        val recommended = MenuItem(
            id = "101",
            categoryId = "커피",
            name = "민수 시그니처 라떼",
            description = "고소한 헤이즐넛 시럽",
            basePrice = 5_500,
            imageUrl = null,
            isSoldOut = false,
            options = emptyList(),
        )
        val repository = object : RecommendationRepository {
            override fun observeTodayRecommendation(): Flow<AppResult<MenuItem?>> =
                flowOf(AppResult.Success(recommended))
        }

        repository.observeTodayRecommendation().test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            assertEquals(recommended, (result as AppResult.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun allowsNullRecommendation() = runBlocking {
        val repository = object : RecommendationRepository {
            override fun observeTodayRecommendation(): Flow<AppResult<MenuItem?>> =
                flowOf(AppResult.Success(null))
        }

        repository.observeTodayRecommendation().test {
            assertEquals(AppResult.Success(null), awaitItem())
            awaitComplete()
        }
    }
}
