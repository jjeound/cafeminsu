package com.cafeminsu.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cafeminsu.data.local.prefs.UserPreferencesDataStore
import com.squareup.moshi.Moshi
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * 테스트용 [SelectedStoreHolder] 빌더.
 *
 * 프로덕션과 동일한 직렬화/복원 경로를 거치도록 실제 Preferences DataStore(임시 파일) 를 사용한다.
 * 매 호출마다 고유 임시 디렉터리를 써 DataStore 간 파일 충돌을 피한다. 동기 접근 계약
 * ([SelectedStoreHolder.current]/[SelectedStoreHolder.observe]/[SelectedStoreHolder.select]) 는
 * 인메모리로 즉시 반영되므로 기존 테스트 동작에 영향이 없다.
 */
internal fun selectedStoreHolderForTest(): SelectedStoreHolder {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val dir = Files.createTempDirectory("selected_store_prefs").toFile()
    val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
        File(dir, "user.preferences_pb")
    }
    return SelectedStoreHolder(
        preferences = UserPreferencesDataStore(dataStore),
        moshi = Moshi.Builder().build(),
        scope = scope,
    )
}
