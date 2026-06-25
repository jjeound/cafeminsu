package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.mapper.toNfcCoupon
import com.cafeminsu.data.remote.NfcApi
import com.cafeminsu.data.remote.NfcClaimReq
import com.cafeminsu.data.remote.NfcClaimRes
import com.cafeminsu.data.remote.NfcErrorBody
import com.cafeminsu.data.remote.toDomainError
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.NfcCoupon
import com.cafeminsu.domain.repository.NfcCouponRepository
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Singleton
class RealNfcCouponRepository @Inject constructor(
    private val nfcApi: NfcApi,
    private val sessionStateHolder: SessionStateHolder,
    private val moshi: Moshi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NfcCouponRepository {
    override suspend fun claim(tagCode: String): AppResult<NfcCoupon> =
        withContext(ioDispatcher) {
            val normalizedTagCode = tagCode.trim()
            if (normalizedTagCode.isBlank()) {
                return@withContext AppResult.Failure(DomainError.Validation("tagCode"))
            }

            when (val auth = ensureAuthenticated()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return@withContext auth
            }

            when (val response = claimTag(normalizedTagCode)) {
                is AppResult.Success -> response.data.toNfcCoupon()
                is AppResult.Failure -> response
            }
        }

    private suspend fun claimTag(tagCode: String): AppResult<NfcClaimRes> =
        try {
            AppResult.Success(nfcApi.claim(NfcClaimReq(tagCode = tagCode)))
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            AppResult.Failure(throwable.toClaimDomainError())
        }

    // 400 두 케이스(NFC_TAG_INACTIVE/VALIDATION_FAILED)는 status 만으로 구분 불가하므로 body code 로 분기한다.
    // code 미존재/미지 값이면 HTTP status 폴백(toDomainError).
    private fun Throwable.toClaimDomainError(): DomainError {
        if (this !is HttpException) {
            return toDomainError()
        }
        return when (parseErrorCode()) {
            "NFC_CLAIM_COOLDOWN" -> DomainError.Payment("nfc-cooldown")
            "NFC_TAG_NOT_FOUND" -> DomainError.NotFound
            "NFC_TAG_INACTIVE" -> DomainError.Payment("nfc-inactive")
            "VALIDATION_FAILED" -> DomainError.Validation("tagCode")
            "UNAUTHORIZED", "INVALID_TOKEN", "EXPIRED_TOKEN" -> DomainError.Unauthorized
            else -> toDomainError()
        }
    }

    private fun HttpException.parseErrorCode(): String? {
        val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        if (body.isNullOrBlank()) {
            return null
        }
        return runCatching {
            moshi.adapter(NfcErrorBody::class.java).fromJson(body)?.code
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun ensureAuthenticated(): AppResult<Unit> {
        val authState = sessionStateHolder.authState.value
        if (authState !is AuthState.Authenticated) {
            return AppResult.Failure(DomainError.Unauthorized)
        }
        return AppResult.Success(Unit)
    }
}
