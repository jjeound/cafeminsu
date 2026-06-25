package com.cafeminsu.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 비민감 앱 설정·상태를 Preferences DataStore 에 영구 저장한다.
 *
 * 저장 대상은 선택 매장(JSON 직렬화 문자열), 점주 영업중 토글, 온보딩 표시 여부,
 * 고객 마지막 탭처럼 프로세스 종료 시 사라지면 안 되는 단순 값뿐이다.
 * 인증 토큰·세션·PII·결제정보는 절대 여기에 저장하지 않는다(토큰은 EncryptedSessionTokenStore).
 *
 * 읽기는 [IOException] 발생 시 [emptyPreferences] 로 복구해 예외를 전파하지 않는다.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun observeSelectedStore(): Flow<String?> = read { it[SelectedStoreKey] }

    suspend fun setSelectedStore(json: String?) {
        dataStore.edit { prefs ->
            if (json == null) {
                prefs.remove(SelectedStoreKey)
            } else {
                prefs[SelectedStoreKey] = json
            }
        }
    }

    fun observeOwnerStoreOpen(): Flow<Boolean> = read { it[OwnerStoreOpenKey] ?: DefaultOwnerStoreOpen }

    /** 점주 로그인 시 "이전에 명시적으로 저장한 토글"만 반영하기 위한 nullable 조회(미설정이면 null). */
    suspend fun readOwnerStoreOpenOrNull(): Boolean? = read { it[OwnerStoreOpenKey] }.first()

    suspend fun setOwnerStoreOpen(open: Boolean) {
        dataStore.edit { it[OwnerStoreOpenKey] = open }
    }

    fun observeOnboardingShown(): Flow<Boolean> = read { it[OnboardingShownKey] ?: DefaultOnboardingShown }

    suspend fun setOnboardingShown(shown: Boolean) {
        dataStore.edit { it[OnboardingShownKey] = shown }
    }

    fun observeLastCustomerTab(): Flow<String?> = read { it[LastCustomerTabKey] }

    suspend fun setLastCustomerTab(route: String?) {
        dataStore.edit { prefs ->
            if (route == null) {
                prefs.remove(LastCustomerTabKey)
            } else {
                prefs[LastCustomerTabKey] = route
            }
        }
    }

    fun observeOrderStatusNotification(): Flow<Boolean> =
        read { it[OrderStatusNotificationKey] ?: DefaultOrderStatusNotification }

    suspend fun setOrderStatusNotification(enabled: Boolean) {
        dataStore.edit { it[OrderStatusNotificationKey] = enabled }
    }

    fun observePromotionNotification(): Flow<Boolean> =
        read { it[PromotionNotificationKey] ?: DefaultPromotionNotification }

    suspend fun setPromotionNotification(enabled: Boolean) {
        dataStore.edit { it[PromotionNotificationKey] = enabled }
    }

    fun observeMarketingNotification(): Flow<Boolean> =
        read { it[MarketingNotificationKey] ?: DefaultMarketingNotification }

    suspend fun setMarketingNotification(enabled: Boolean) {
        dataStore.edit { it[MarketingNotificationKey] = enabled }
    }

    private fun <T> read(selector: (Preferences) -> T): Flow<T> =
        dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map(selector)

    private companion object {
        val SelectedStoreKey = stringPreferencesKey("selected_store_json")
        val OwnerStoreOpenKey = booleanPreferencesKey("owner_store_open")
        val OnboardingShownKey = booleanPreferencesKey("onboarding_shown")
        val LastCustomerTabKey = stringPreferencesKey("last_customer_tab")
        val OrderStatusNotificationKey = booleanPreferencesKey("notify_order_status")
        val PromotionNotificationKey = booleanPreferencesKey("notify_promotion")
        val MarketingNotificationKey = booleanPreferencesKey("notify_marketing")

        const val DefaultOwnerStoreOpen = false
        const val DefaultOnboardingShown = false
        const val DefaultOrderStatusNotification = true
        const val DefaultPromotionNotification = true
        const val DefaultMarketingNotification = false
    }
}
