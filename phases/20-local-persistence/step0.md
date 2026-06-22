# Step 0 — DataStore(Preferences) 기반 영구 설정 저장 + 3개 소비자 배선

인메모리라 프로세스 종료 시 사라지는 단순 설정/상태를 **Preferences DataStore**로 영구 저장한다.
대상 3가지: ① 선택 매장, ② 점주 영업중 토글, ③ 앱 플래그(온보딩 표시 여부·고객 마지막 탭).
`docs/ARCHITECTURE.md`(레이어/UDF/에러), `docs/SECURITY.md`, `CLAUDE.md`(토큰·매직넘버·TDD)를 따른다.

## ⚠ 보안 (엄수)
- **인증 토큰/세션은 절대 평문 DataStore에 저장하지 않는다.** 토큰은 기존 `EncryptedSessionTokenStore`
  (`EncryptedSharedPreferences`) 그대로 유지한다. 이 step에서 토큰 저장을 건드리지 않는다.
- 저장하는 값은 비민감 설정뿐(매장 id/이름, 영업 토글, UI 플래그). PII·결제정보·토큰 저장/로깅 금지.

## 현재 코드 (반드시 일관 유지)
- `data/repository/SelectedStoreHolder.kt`: `@Singleton @Inject constructor()`, 인메모리
  `MutableStateFlow<Store?>`. `observe()`/`current()`/`select(store)`. 4개 Real 리포가 주입해서 씀
  (`RealMenuRepository`/`RealStoreRepository`/`RealOrderRepository`/`RealRewardRepository`).
- `domain/model/Store.kt`: `Store(id, name, address, phone, distanceMeters, latitude, longitude,
  status: StoreStatus, closingTimeLabel: String?, amenities: List<StoreAmenity>)` — Moshi 직렬화 가능한 단순 data class.
- `data/auth/RealOwnerAuthProvider.kt`: `setStoreOpen(open)`는 인메모리 `ownerProfile`만 갱신(서버 엔드포인트 없음).
  `login()`은 `OwnerProfile`을 새로 채운다. `OwnerProfile.isStoreOpen: Boolean` 필드 존재.
- `di/AppScopeModule.kt`: `@ApplicationScope CoroutineScope`(앱 스코프) 제공. `di/DispatcherModule.kt`: `@IoDispatcher`.
- `core/AppResult.kt`. Moshi는 이미 의존성에 있음(`libs.moshi`, `moshi.kotlin.codegen` ksp).
- `androidx.datastore.preferences` 의존성은 **이미 build.gradle.kts에 선언**돼 있으나 코드에서 미사용.

## ⚠ TDD — 테스트를 먼저 작성하라 (TDD 가드 훅 주의)
`scripts/hooks/tdd-guard.sh`는 **새 `src/main/.../X.kt` 파일을 쓰기 전에** 같은 패키지의
`src/test/.../XTest.kt`(또는 `src/androidTest`)가 **이미 존재**해야 허용한다. 새 구현 파일마다 대응 테스트를 먼저 만들어라.
- DataStore는 **JVM 단위 테스트로 검증 가능**하다: 테스트에서
  `PreferenceDataStoreFactory.create(scope = TestScope(...), produceFile = { tempFolder.newFile() })`로
  실제 `DataStore<Preferences>`를 만들어 주입한다(`@get:Rule TemporaryFolder`, `kotlinx-coroutines-test` 이미 있음).
- 작성할 테스트:
  - `UserPreferencesDataStoreTest`: 기본값(미설정 시 selectedStore=null·ownerStoreOpen=false·onboardingShown=false),
    저장→Flow 방출, 덮어쓰기, clear.
  - `SelectedStoreHolderTest`: `select(store)` 후 **새 인스턴스가 같은 DataStore로 복원**(rehydrate)되는지,
    `current()`/`observe()`가 복원값을 주는지(가짜/실 DataStore 사용). 직렬화 깨진 값은 null 폴백.
  - `RealOwnerAuthProviderTest`(기존 있으면 확장): `setStoreOpen(true)` 후 DataStore에 영속되고,
    재로그인 시 영속된 토글이 `OwnerProfile.isStoreOpen`에 반영되는지.

## 만들 것
### 1) Preferences DataStore — `data/local/prefs/`
- `di/DataStoreModule.kt`: `@Provides @Singleton fun provideUserPreferences(@ApplicationContext context, @ApplicationScope scope): DataStore<Preferences>`
  = `PreferenceDataStoreFactory.create(scope = scope, produceFile = { context.preferencesDataStoreFile("cafeminsu_user_prefs") })`.
  (스코프에 IO 디스패처를 더해도 됨.)
- `UserPreferencesDataStore`(@Singleton, 주입된 `DataStore<Preferences>` 사용). **명명된 Preferences.Key 상수**(매직 문자열 금지):
  - `observeSelectedStore(): Flow<String?>` + `suspend setSelectedStore(json: String?)`  // Store JSON
  - `observeOwnerStoreOpen(): Flow<Boolean>` + `suspend setOwnerStoreOpen(open: Boolean)`
  - `observeOnboardingShown(): Flow<Boolean>` + `suspend setOnboardingShown(shown: Boolean)`
  - `observeLastCustomerTab(): Flow<String?>` + `suspend setLastCustomerTab(route: String?)`
  - 읽기는 `data.map { it[KEY] ?: 기본값 }`, IOException은 `emptyPreferences()`로 복구.

### 2) 선택 매장 영속화 — `SelectedStoreHolder` 개조
- 생성자에 `UserPreferencesDataStore`, `Moshi`, `@ApplicationScope scope`(rehydrate용) 주입.
- 초기화 시 `scope.launch`로 `observeSelectedStore()` 첫 값을 읽어 Moshi로 `Store?` 역직렬화 후
  인메모리 StateFlow에 복원(앱 재시작 시 선택 매장 유지). 깨진 JSON은 무시(null).
- `select(store)`: 인메모리 갱신 + `scope.launch { prefs.setSelectedStore(moshi-json) }`로 영속.
- `current()`/`observe()` 시그니처·동작은 그대로(동기 접근 유지) — 4개 Real 리포 무파손.

### 3) 점주 영업중 토글 영속화 — `RealOwnerAuthProvider`
- `UserPreferencesDataStore` 주입. `setStoreOpen(open)`: 인메모리 프로필 갱신 + `prefs.setOwnerStoreOpen(open)` 영속.
- `login()` 성공 시 영속된 `observeOwnerStoreOpen()` 값을 읽어 `ownerProfile.copy(isStoreOpen = persisted)`로 반영.
- (Mock 점주 provider는 이 step에서 건드리지 않아도 됨 — BASE_URL 게이트로 Real만 쓰일 때 적용.)

### 4) 앱 플래그 — 최소 배선
- `onboardingShown`/`lastCustomerTab`는 위 DataStore에 정의·테스트까지 한다.
- **저파급 배선만**: 고객 하단탭 셸이 탭 변경 시 `setLastCustomerTab(route)`로 저장하도록 연결(복원은
  기존 네비/테스트를 깨지 않는 선에서만; 위험하면 저장만 하고 복원은 후속으로 둔다). 온보딩 플래그는 정의만 하고
  스플래시/네비 흐름을 바꾸지 않는다(화면 흐름 변경은 범위 밖).

## 하지 말 것
- 토큰/세션을 평문 DataStore로 옮기기 금지(`EncryptedSessionTokenStore` 유지). PII/토큰 로깅 금지.
- `SelectedStoreHolder`의 `current()`/`observe()`/`select()` 시그니처 변경 금지(소비자 4곳 무파손).
- 네비게이션 그래프/스플래시 흐름 대수술 금지(앱 플래그는 최소 배선). 새 화면 추가 금지.
- hex 리터럴·새 색/토큰·매직 문자열/넘버 금지(Preferences.Key는 명명 상수). 예외 전파 금지(읽기는 안전 폴백).
- 기존 테스트 무파손.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공
  (`UserPreferencesDataStoreTest`/`SelectedStoreHolderTest`/`RealOwnerAuthProviderTest` 포함, 기존 무파손).
- 선택 매장과 점주 영업 토글이 DataStore에 저장되고, **새 인스턴스/앱 재시작 시 복원**됨을 테스트로 확인.
- 통과하면 step 0 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
