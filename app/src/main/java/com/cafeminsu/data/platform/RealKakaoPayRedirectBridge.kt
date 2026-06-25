package com.cafeminsu.data.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.payment.KakaoPayRedirectBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 카카오페이 ready 단계의 redirectUrl 을 외부 브라우저로 열고, 승인 후 `cafeminsu://kakaopay` 딥링크로
 * 돌아오는 `pg_token` 을 [KakaoPayRedirectActivity] 가 [onPgTokenCaptured] 로 넘겨주면 대기 중인 코루틴을
 * 재개한다. Activity/딥링크 의존은 플랫폼 레이어에만 둔다(도메인/데이터 PaymentRepository 는 Context 비종속).
 *
 * pg_token 은 로그/저장 대상이 아니다(SECURITY §3) — 메모리에서만 전달하고 즉시 폐기한다.
 */
@Singleton
class RealKakaoPayRedirectBridge @Inject constructor(
    @ApplicationContext private val context: Context,
) : KakaoPayRedirectBridge {
    private val lock = Any()
    private var pending: CancellableContinuation<AppResult<String>>? = null

    override suspend fun awaitPgToken(redirectUrl: String): AppResult<String> {
        if (redirectUrl.isBlank()) {
            return AppResult.Failure(DomainError.Payment("kakaopay-redirect-url"))
        }
        return suspendCancellableCoroutine { continuation ->
            synchronized(lock) { pending = continuation }
            continuation.invokeOnCancellation {
                synchronized(lock) {
                    if (pending === continuation) {
                        pending = null
                    }
                }
            }
            if (!launchRedirect(redirectUrl)) {
                resumePending(AppResult.Failure(DomainError.Payment("kakaopay-redirect-launch")))
            }
        }
    }

    /** 딥링크 캡처 결과. pg_token 이 없으면(사용자 취소·오류) 결제 실패로 재개한다. */
    fun onPgTokenCaptured(pgToken: String?) {
        val result = if (pgToken.isNullOrBlank()) {
            AppResult.Failure(DomainError.Payment("kakaopay-cancelled"))
        } else {
            AppResult.Success(pgToken)
        }
        resumePending(result)
    }

    private fun resumePending(result: AppResult<String>) {
        val continuation = synchronized(lock) {
            pending.also { pending = null }
        } ?: return
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }

    private fun launchRedirect(redirectUrl: String): Boolean =
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
}
