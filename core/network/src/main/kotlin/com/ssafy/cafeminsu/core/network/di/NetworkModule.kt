package com.ssafy.cafeminsu.core.network.di

import com.ssafy.cafeminsu.core.network.BuildConfig
import com.ssafy.cafeminsu.core.network.client.AuthClient
import com.ssafy.cafeminsu.core.network.client.GifticonClient
import com.ssafy.cafeminsu.core.network.client.MenuClient
import com.ssafy.cafeminsu.core.network.client.NotificationClient
import com.ssafy.cafeminsu.core.network.client.OwnerMenuClient
import com.ssafy.cafeminsu.core.network.client.OrderClient
import com.ssafy.cafeminsu.core.network.client.OwnerStoreClient
import com.ssafy.cafeminsu.core.network.client.PaymentClient
import com.ssafy.cafeminsu.core.network.client.OwnerPaymentClient
import com.ssafy.cafeminsu.core.network.client.StampClient
import com.ssafy.cafeminsu.core.network.client.StoreClient
import com.ssafy.cafeminsu.core.network.service.AuthService
import com.ssafy.cafeminsu.core.network.service.GifticonService
import com.ssafy.cafeminsu.core.network.service.MenuService
import com.ssafy.cafeminsu.core.network.service.NotificationService
import com.ssafy.cafeminsu.core.network.service.OwnerMenuService
import com.ssafy.cafeminsu.core.network.service.OrderService
import com.ssafy.cafeminsu.core.network.service.OwnerOrderService
import com.ssafy.cafeminsu.core.network.service.OwnerStoreService
import com.ssafy.cafeminsu.core.network.service.PaymentService
import com.ssafy.cafeminsu.core.network.service.OwnerPaymentService
import com.ssafy.cafeminsu.core.network.service.StampService
import com.ssafy.cafeminsu.core.network.service.StoreService
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addNetworkInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        },
                    )
                }
            }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        json: Json,
        okHttpClient: OkHttpClient,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL.requireBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService =
        retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun provideMenuService(retrofit: Retrofit): MenuService =
        retrofit.create(MenuService::class.java)

    @Provides
    @Singleton
    fun provideOwnerMenuService(retrofit: Retrofit): OwnerMenuService =
        retrofit.create(OwnerMenuService::class.java)

    @Provides
    @Singleton
    fun provideOrderService(retrofit: Retrofit): OrderService =
        retrofit.create(OrderService::class.java)

    @Provides
    @Singleton
    fun provideOwnerOrderService(retrofit: Retrofit): OwnerOrderService =
        retrofit.create(OwnerOrderService::class.java)

    @Provides
    @Singleton
    fun providePaymentService(retrofit: Retrofit): PaymentService =
        retrofit.create(PaymentService::class.java)

    @Provides
    @Singleton
    fun provideOwnerPaymentService(retrofit: Retrofit): OwnerPaymentService =
        retrofit.create(OwnerPaymentService::class.java)

    @Provides
    @Singleton
    fun provideStoreService(retrofit: Retrofit): StoreService =
        retrofit.create(StoreService::class.java)

    @Provides
    @Singleton
    fun provideOwnerStoreService(retrofit: Retrofit): OwnerStoreService =
        retrofit.create(OwnerStoreService::class.java)

    @Provides
    @Singleton
    fun provideNotificationService(retrofit: Retrofit): NotificationService =
        retrofit.create(NotificationService::class.java)

    @Provides
    @Singleton
    fun provideGifticonService(retrofit: Retrofit): GifticonService =
        retrofit.create(GifticonService::class.java)

    @Provides
    @Singleton
    fun provideStampService(retrofit: Retrofit): StampService =
        retrofit.create(StampService::class.java)

    @Provides
    @Singleton
    fun provideAuthClient(service: AuthService): AuthClient = AuthClient(service)

    @Provides
    @Singleton
    fun provideMenuClient(service: MenuService): MenuClient = MenuClient(service)

    @Provides
    @Singleton
    fun provideOwnerMenuClient(service: OwnerMenuService): OwnerMenuClient = OwnerMenuClient(service)

    @Provides
    @Singleton
    fun provideOrderClient(service: OrderService): OrderClient = OrderClient(service)

    @Provides
    @Singleton
    fun providePaymentClient(service: PaymentService): PaymentClient = PaymentClient(service)

    @Provides
    @Singleton
    fun provideOwnerPaymentClient(service: OwnerPaymentService): OwnerPaymentClient =
        OwnerPaymentClient(service)

    @Provides
    @Singleton
    fun provideStoreClient(service: StoreService): StoreClient = StoreClient(service)

    @Provides
    @Singleton
    fun provideOwnerStoreClient(service: OwnerStoreService): OwnerStoreClient = OwnerStoreClient(service)

    @Provides
    @Singleton
    fun provideNotificationClient(service: NotificationService): NotificationClient =
        NotificationClient(service)

    @Provides
    @Singleton
    fun provideGifticonClient(service: GifticonService): GifticonClient = GifticonClient(service)

    @Provides
    @Singleton
    fun provideStampClient(service: StampService): StampClient = StampClient(service)
}

private fun String.requireBaseUrl(): String {
    require(isNotBlank()) { "BASE_URL is required." }
    return if (endsWith('/')) this else "$this/"
}
