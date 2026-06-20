package com.cafeminsu.data.remote

import com.cafeminsu.BuildConfig
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
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
    fun provideOkHttpClient(): OkHttpClient =
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
}

internal fun createOkHttpClient(debug: Boolean): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    if (debug) {
        builder.addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            },
        )
    }

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
