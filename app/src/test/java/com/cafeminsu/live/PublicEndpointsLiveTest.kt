package com.cafeminsu.live

import com.cafeminsu.data.remote.MenuApi
import com.cafeminsu.data.remote.StoreApi
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Response
import retrofit2.http.GET

/**
 * 공개(public) 엔드포인트 라이브 스모크. 게이트 미설정 시 [assumeLiveServer] 로 전부 skip.
 * 단언은 "2xx + 역직렬화 성공 + 필수 필드 형식" 수준 — 서버 데이터는 가변이므로 값을 하드코딩하지 않는다.
 * 읽기(GET) 전용 — 서버 상태를 바꾸지 않는다.
 */
class PublicEndpointsLiveTest {
    @Test
    fun healthReturns2xx() = runBlocking {
        assumeLiveServer()
        val response = healthApi().health()
        assertTrue("health 가 2xx 가 아님: ${response.code()}", response.isSuccessful)
    }

    @Test
    fun searchStoresParsesStoreSearchRes() = runBlocking {
        assumeLiveServer()
        val result = storeApi().searchStores()
        assertNotNull("StoreSearchRes.stores 가 null", result.stores)
        result.stores.orEmpty().forEach { assertNotNull("store id 가 null", it.id) }
    }

    @Test
    fun getStoreParsesStoreDetailRes() = runBlocking {
        assumeLiveServer()
        val storeId = firstStoreId()
        val detail = storeApi().getStore(storeId)
        assertNotNull("StoreDetailRes.id 가 null", detail.id)
        assertNotNull("StoreDetailRes.name 가 null", detail.name)
    }

    @Test
    fun listStoreMenusParsesMenuListItemRes() = runBlocking {
        assumeLiveServer()
        val storeId = firstStoreId()
        val menus = menuApi().listByStore(storeId)
        assertNotNull("menu 목록 역직렬화 실패", menus)
        menus.forEach { assertNotNull("menu id 가 null", it.id) }
    }

    @Test
    fun getMenuParsesMenuDetailRes() = runBlocking {
        assumeLiveServer()
        val storeId = firstStoreId()
        val menus = menuApi().listByStore(storeId)
        val menuId = menus.firstOrNull()?.id
        assumeTrue("매장에 메뉴가 없어 메뉴 상세 검증 skip", menuId != null)
        val detail = menuApi().getMenu(menuId!!)
        assertNotNull("MenuDetailRes.id 가 null", detail.id)
        assertNotNull("MenuDetailRes.name 가 null", detail.name)
    }

    private suspend fun firstStoreId(): Long {
        val storeId = storeApi().searchStores().stores?.firstOrNull()?.id
        assumeTrue("매장 목록이 비어 id 의존 검증 skip", storeId != null)
        return storeId!!
    }

    private fun storeApi(): StoreApi = liveRetrofit().create(StoreApi::class.java)

    private fun menuApi(): MenuApi = liveRetrofit().create(MenuApi::class.java)

    private fun healthApi(): HealthApi = liveRetrofit().create(HealthApi::class.java)

    /** 헬스체크는 DTO 가 없어 raw 응답으로 2xx 만 확인한다(`health` 경로, `api/` 접두사 없음). */
    private interface HealthApi {
        @GET("health")
        suspend fun health(): Response<ResponseBody>
    }
}
