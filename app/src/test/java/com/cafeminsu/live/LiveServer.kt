package com.cafeminsu.live

import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import retrofit2.Retrofit

/**
 * Opt-in 라이브 스모크 테스트 게이팅 유틸.
 *
 * 평소 빌드/CI(`:app:testDebugUnitTest`)에서는 모든 라이브 테스트가 [assumeLiveServer]/[assumeLiveAuth]
 * 로 skip 되며, 명시적으로 `liveServer=true` + 베이스 URL(시스템 프로퍼티/환경변수)을 줄 때만 실행된다.
 * 토큰·PII 는 코드/로그에 하드코딩하지 않는다(`SECURITY.md`). 값은 시스템 프로퍼티 우선, 없으면 env.
 */
object LiveServer {
    private const val ENABLED_PROP = "liveServer"
    private const val BASE_URL_PROP = "liveServer.baseUrl"
    private const val BASE_URL_ENV = "LIVE_SERVER_BASE_URL"
    private const val TOKEN_PROP = "liveServer.token"
    private const val TOKEN_ENV = "LIVE_SERVER_TOKEN"

    fun liveServerEnabled(): Boolean =
        System.getProperty(ENABLED_PROP)?.trim().equals("true", ignoreCase = true)

    fun liveBaseUrl(): String? =
        propertyOrEnv(BASE_URL_PROP, BASE_URL_ENV)

    fun liveToken(): String? =
        propertyOrEnv(TOKEN_PROP, TOKEN_ENV)

    private fun propertyOrEnv(propertyKey: String, envKey: String): String? =
        (System.getProperty(propertyKey) ?: System.getenv(envKey))
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}

/** 게이트(플래그+baseUrl) 미설정 시 테스트를 skip 한다(실패가 아니라 무시). */
fun assumeLiveServer() {
    assumeTrue(
        "liveServer 플래그 또는 baseUrl 미설정 — 라이브 스모크 테스트 skip",
        LiveServer.liveServerEnabled() && LiveServer.liveBaseUrl() != null,
    )
}

/** 게이트 + 토큰 미설정 시 인증 라이브 테스트를 skip 한다. */
fun assumeLiveAuth() {
    assumeLiveServer()
    assumeTrue(
        "liveServer.token 미설정 — 인증 라이브 스모크 테스트 skip",
        LiveServer.liveToken() != null,
    )
}

/**
 * 앱과 동일한 직렬화·HTTPS 설정으로 호출하기 위해 [createOkHttpClient]/[createMoshi]/[createRetrofit]
 * (NetworkModule)을 재사용한다. baseUrl 은 환경값에서 읽고, [token] 이 있으면 Authorization 헤더를 붙인다.
 */
fun liveRetrofit(token: String? = null): Retrofit {
    val baseUrl = requireNotNull(LiveServer.liveBaseUrl()) {
        "liveBaseUrl is null — assumeLiveServer()/assumeLiveAuth() 를 먼저 호출하세요."
    }
    return createRetrofit(
        baseUrl = baseUrl,
        moshi = createMoshi(),
        okHttpClient = liveOkHttpClient(token),
    )
}

private fun liveOkHttpClient(token: String?): OkHttpClient =
    createOkHttpClient(
        debug = false,
        authorizationInterceptor = token?.let { bearer ->
            Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $bearer")
                        .build(),
                )
            }
        },
    )
