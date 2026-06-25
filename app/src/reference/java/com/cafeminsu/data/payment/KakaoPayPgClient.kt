package com.cafeminsu.data.payment

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.KakaoPayApi
import com.cafeminsu.data.remote.KakaoPayApproveReq
import com.cafeminsu.data.remote.KakaoPayReadyReq
import com.cafeminsu.data.remote.runCatchingToAppResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 카카오페이 PgClient 구현. ready → (리다이렉트로 pg_token 캡처) → approve 흐름을 PgClient 뒤에 둔다.
 * 멱등 식별은 기존과 동일하게 merchantUid 로 유지하고, approve 결과 paymentToken 은 verify 의 impUid 슬롯에 들어간다.
 * tid/pg_token/paymentToken 등 PG 식별자는 로그에 남기지 않는다(SECURITY §3).
 */
@Singleton
class KakaoPayPgClient @Inject constructor(
    private val kakaoPayApi: KakaoPayApi,
    private val redirectBridge: KakaoPayRedirectBridge,
) : PgClient {
    override suspend fun authorize(
        merchantUid: String,
        amount: Int,
    ): AppResult<String> {
        if (merchantUid.isBlank()) {
            return AppResult.Failure(DomainError.Validation("merchantUid"))
        }
        if (amount <= 0) {
            return AppResult.Failure(DomainError.Validation("amount"))
        }

        val ready = when (
            val result = runCatchingToAppResult {
                kakaoPayApi.ready(
                    request = KakaoPayReadyReq(merchantUid = merchantUid, amount = amount),
                )
            }
        ) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        val tid = ready.tid
            ?: return AppResult.Failure(DomainError.Payment("kakaopay-ready"))
        val redirectUrl = ready.redirectUrl
            ?: return AppResult.Failure(DomainError.Payment("kakaopay-ready"))

        val pgToken = when (val result = redirectBridge.awaitPgToken(redirectUrl)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }

        val approve = when (
            val result = runCatchingToAppResult {
                kakaoPayApi.approve(
                    request = KakaoPayApproveReq(
                        tid = tid,
                        pgToken = pgToken,
                        merchantUid = merchantUid,
                    ),
                )
            }
        ) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }

        return approve.paymentToken
            ?.let { paymentToken -> AppResult.Success(paymentToken) }
            ?: AppResult.Failure(DomainError.Payment("kakaopay-approve"))
    }
}
