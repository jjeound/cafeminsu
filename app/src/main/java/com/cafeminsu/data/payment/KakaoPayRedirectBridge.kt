package com.cafeminsu.data.payment

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 카카오페이 ready 단계가 내려준 redirectUrl 을 사용자 인증 화면으로 띄우고, 승인 후 콜백되는 `pg_token` 을
 * 캡처한다. 실구현(커스텀 탭 + 딥링크 `pg_token` 캡처)은 step1 범위이고, 이 step 에서는 인터페이스만 둔다.
 */
interface KakaoPayRedirectBridge {
    suspend fun awaitPgToken(redirectUrl: String): AppResult<String>
}

/**
 * step0 기본 바인딩. 키게이트가 false(기본)이면 KakaoPayPgClient 자체가 주입되지 않으므로 호출되지 않는다.
 * 키게이트가 true 인데 실구현(step1)이 아직 없으면 낙관적 성공 없이 실패를 돌려준다.
 */
@Singleton
class NoOpKakaoPayRedirectBridge @Inject constructor() : KakaoPayRedirectBridge {
    override suspend fun awaitPgToken(redirectUrl: String): AppResult<String> =
        AppResult.Failure(DomainError.Payment("kakaopay-redirect-not-implemented"))
}
