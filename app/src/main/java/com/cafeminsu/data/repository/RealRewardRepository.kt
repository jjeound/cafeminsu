package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.mapper.emptyStampCard
import com.cafeminsu.data.mapper.toGifticon
import com.cafeminsu.data.mapper.toGifticons
import com.cafeminsu.data.mapper.toRepresentativeStampCard
import com.cafeminsu.data.mapper.toStampCard
import com.cafeminsu.data.mapper.useAmount
import com.cafeminsu.data.remote.GifticonApi
import com.cafeminsu.data.remote.GifticonDetailRes
import com.cafeminsu.data.remote.GifticonUseReq
import com.cafeminsu.data.remote.StampApi
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.data.remote.unwrap
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.repository.RewardRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class RealRewardRepository @Inject constructor(
    private val stampApi: StampApi,
    private val gifticonApi: GifticonApi,
    private val selectedStoreHolder: SelectedStoreHolder,
    private val sessionStateHolder: SessionStateHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RewardRepository {
    override fun observeStampCard(): Flow<AppResult<StampCard>> =
        flow {
            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    emit(auth)
                    return@flow
                }
            }

            emit(fetchStampCard())
        }.flowOn(ioDispatcher)

    override suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard> =
        withContext(ioDispatcher) {
            if (orderId.isBlank()) {
                return@withContext AppResult.Failure(DomainError.Validation("orderId"))
            }
            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext auth
            }

            fetchStampCard()
        }

    override fun observeGifticons(): Flow<AppResult<List<Gifticon>>> =
        flow {
            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    emit(auth)
                    return@flow
                }
            }

            emit(
                when (
                    val response = runCatchingToAppResult {
                        gifticonApi.getMyGifticons()
                    }
                ) {
                    is AppResult.Success -> response.data.unwrap { it.toGifticons() }
                    is AppResult.Failure -> response
                },
            )
        }.flowOn(ioDispatcher)

    override suspend fun getGifticon(id: String): AppResult<Gifticon> =
        withContext(ioDispatcher) {
            val gifticonId = id.toLongOrNull()
                ?: return@withContext AppResult.Failure(DomainError.NotFound)

            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext auth
            }

            when (val detail = fetchGifticonDetail(gifticonId)) {
                is AppResult.Success -> detail.data.toGifticon()
                is AppResult.Failure -> detail
            }
        }

    override suspend fun markGifticonUsed(id: String): AppResult<Gifticon> =
        withContext(ioDispatcher) {
            val gifticonId = id.toLongOrNull()
                ?: return@withContext AppResult.Failure(DomainError.NotFound)

            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext auth
            }

            val detail = when (val result = fetchGifticonDetail(gifticonId)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return@withContext result
            }
            val usedAmount = when (val result = detail.useAmount()) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return@withContext result
            }

            when (
                val response = runCatchingToAppResult {
                    gifticonApi.useGifticon(
                        gifticonId = gifticonId,
                        request = GifticonUseReq(
                            orderId = gifticonId,
                            usedAmount = usedAmount,
                        ),
                    )
                }
            ) {
                is AppResult.Success -> response.data.unwrap { it.toGifticon(previous = detail) }
                is AppResult.Failure -> response
            }
        }

    private suspend fun fetchStampCard(): AppResult<StampCard> {
        val selectedStoreId = selectedStoreHolder.current()?.id?.toLongOrNull()
        return if (selectedStoreId != null) {
            when (
                val response = runCatchingToAppResult {
                    stampApi.getStoreStamp(storeId = selectedStoreId)
                }
            ) {
                is AppResult.Success -> response.data.unwrap { it.toStampCard() }
                is AppResult.Failure ->
                    if (response.error == DomainError.NotFound) {
                        AppResult.Success(emptyStampCard(selectedStoreId))
                    } else {
                        response
                    }
            }
        } else {
            when (
                val response = runCatchingToAppResult {
                    stampApi.getMyStamps()
                }
            ) {
                is AppResult.Success -> response.data.unwrap { it.toRepresentativeStampCard() }
                is AppResult.Failure -> response
            }
        }
    }

    private suspend fun fetchGifticonDetail(gifticonId: Long): AppResult<GifticonDetailRes> =
        when (
            val response = runCatchingToAppResult {
                gifticonApi.getGifticon(gifticonId = gifticonId)
            }
        ) {
            is AppResult.Success -> response.data.unwrap { AppResult.Success(it) }
            is AppResult.Failure -> response
        }

    private fun ensureAuthenticated(): AppResult<Unit> {
        val authState = sessionStateHolder.authState.value
        if (authState !is AuthState.Authenticated) {
            return AppResult.Failure(DomainError.Unauthorized)
        }
        return AppResult.Success(Unit)
    }
}
