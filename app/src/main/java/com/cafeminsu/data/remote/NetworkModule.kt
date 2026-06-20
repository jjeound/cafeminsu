package com.cafeminsu.data.remote

import com.cafeminsu.BuildConfig
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.auth.SessionTokenStore
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private const val CONNECT_TIMEOUT_SECONDS = 10L
private const val READ_TIMEOUT_SECONDS = 15L

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenStore: SessionTokenStore,
        sessionStateHolder: SessionStateHolder,
        @Unauthenticated authApi: AuthApi,
    ): OkHttpClient =
        createOkHttpClient(
            debug = BuildConfig.DEBUG,
            authorizationInterceptor = AuthorizationInterceptor(tokenStore),
            authenticator = SessionAuthenticator(
                tokenStore = tokenStore,
                authApi = authApi,
                sessionStateHolder = sessionStateHolder,
            ),
        )

    @Provides
    @Singleton
    @Unauthenticated
    fun provideUnauthenticatedOkHttpClient(): OkHttpClient =
        createOkHttpClient(debug = BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = createMoshi()

    @Provides
    @Singleton
    fun provideRetrofit(
        moshi: Moshi,
        okHttpClient: OkHttpClient,
    ): Retrofit =
        createRetrofit(
            baseUrl = BuildConfig.BASE_URL,
            moshi = moshi,
            okHttpClient = okHttpClient,
        )

    @Provides
    @Singleton
    @Unauthenticated
    fun provideUnauthenticatedRetrofit(
        moshi: Moshi,
        @Unauthenticated okHttpClient: OkHttpClient,
    ): Retrofit =
        createRetrofit(
            baseUrl = BuildConfig.BASE_URL,
            moshi = moshi,
            okHttpClient = okHttpClient,
        )

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideOrderApi(retrofit: Retrofit): OrderApi =
        retrofit.create(OrderApi::class.java)

    @Provides
    @Singleton
    fun providePaymentApi(retrofit: Retrofit): PaymentApi =
        retrofit.create(PaymentApi::class.java)

    @Provides
    @Singleton
    @Unauthenticated
    fun provideStoreApi(
        @Unauthenticated retrofit: Retrofit,
    ): StoreApi =
        retrofit.create(StoreApi::class.java)

    @Provides
    @Singleton
    @Unauthenticated
    fun provideMenuApi(
        @Unauthenticated retrofit: Retrofit,
    ): MenuApi =
        retrofit.create(MenuApi::class.java)

    @Provides
    @Singleton
    @Unauthenticated
    fun provideUnauthenticatedAuthApi(
        @Unauthenticated retrofit: Retrofit,
    ): AuthApi =
        retrofit.create(AuthApi::class.java)
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Unauthenticated

internal fun createOkHttpClient(
    debug: Boolean,
    authorizationInterceptor: Interceptor? = null,
    authenticator: Authenticator? = null,
): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    authorizationInterceptor?.let(builder::addInterceptor)

    if (debug) {
        builder.addInterceptor(
            HttpLoggingInterceptor { message ->
                HttpLoggingInterceptor.Logger.DEFAULT.log(message.redactSensitiveNetworkValues())
            }.apply {
                redactHeader("Authorization")
                redactHeader("Refresh-Token")
                level = HttpLoggingInterceptor.Level.BODY
            },
        )
    }

    authenticator?.let(builder::authenticator)

    return builder.build()
}

internal fun createMoshi(): Moshi = Moshi.Builder().build()

internal fun createRetrofit(
    baseUrl: String,
    moshi: Moshi,
    okHttpClient: OkHttpClient,
): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl.asRetrofitBaseUrl())
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

private fun String.asRetrofitBaseUrl(): String {
    val normalized = trim()
    require(normalized.isNotBlank()) {
        "BASE_URL is blank. Real network repositories must stay disabled until BASE_URL is set."
    }
    return if (normalized.endsWith("/")) normalized else "$normalized/"
}

internal fun String.redactSensitiveNetworkValues(): String =
    replace(SensitiveHeaderRegex, "\$1<redacted>")
        .replace(SensitiveJsonTokenRegex, "\$1<redacted>\$2")

private val SensitiveHeaderRegex = Regex(
    pattern = "^((?:Authorization|Refresh-Token):\\s*).*$",
    options = setOf(RegexOption.IGNORE_CASE),
)

private val SensitiveJsonTokenRegex = Regex(
    pattern = "(\"(?:accessToken|refreshToken)\"\\s*:\\s*\")[^\"]*(\")",
    options = setOf(RegexOption.IGNORE_CASE),
)
