package com.cafeminsu.ui.feature.payment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.cafeminsu.data.platform.RealKakaoPayRedirectBridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 카카오페이 승인 후 `cafeminsu://kakaopay?pg_token=...` 딥링크를 수신해 `pg_token` 을 추출하고
 * [RealKakaoPayRedirectBridge] 로 넘긴 뒤 즉시 종료한다. 결제 화면(이전 화면)으로 자연스럽게 복귀한다.
 *
 * 딥링크 스킴/호스트를 화이트리스트로만 수락한다(SECURITY §6). pg_token 은 로그에 남기지 않는다(§3).
 */
@AndroidEntryPoint
class KakaoPayRedirectActivity : ComponentActivity() {
    @Inject
    lateinit var redirectBridge: RealKakaoPayRedirectBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleRedirect(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirect(intent)
        finish()
    }

    private fun handleRedirect(intent: Intent?) {
        redirectBridge.onPgTokenCaptured(intent?.data?.toPgTokenOrNull())
    }

    private fun Uri.toPgTokenOrNull(): String? {
        if (scheme != REDIRECT_SCHEME || host != REDIRECT_HOST) {
            return null
        }
        return getQueryParameter(PG_TOKEN_PARAM)
    }

    private companion object {
        const val REDIRECT_SCHEME = "cafeminsu"
        const val REDIRECT_HOST = "kakaopay"
        const val PG_TOKEN_PARAM = "pg_token"
    }
}
