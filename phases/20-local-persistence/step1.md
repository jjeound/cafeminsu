# Step 1 — Room 토대 배선 + 매장 목록 오프라인 캐시

로컬 캐시 DB(Room)를 처음으로 배선하고, **매장 목록을 read-through 캐시**한다. 네트워크 성공 시
캐시에 write-through, 네트워크 실패 시 캐시가 있으면 그것을 오프라인 폴백으로 보여준다.
`docs/ARCHITECTURE.md`(레이어: ui→domain←data, domain은 안드로이드 비종속), `CLAUDE.md`(캐시는 Room·TDD), `docs/SECURITY.md`.

## 현재 코드 (반드시 일관 유지)
- Room 의존성은 `gradle/libs.versions.toml`에 **선언만** 됨(`room-runtime`/`room-ktx`/`room-compiler`, room=2.6.1),
  `app/build.gradle.kts`에는 **아직 추가 안 됨**. `ksp` 플러그인은 이미 적용됨(`alias(libs.plugins.ksp)`).
- `data/repository/RealStoreRepository.kt`:
  - `observeNearbyStores(query)`: `flow { emit(fetchStores(query)); 좌표 보강 시 한 번 더 emit }.flowOn(io)`.
    `fetchStores`는 `storeApi.searchStores(...)` 결과를 `AppResult<List<Store>>`로.
  - `getStore(id)`/`selectStore(id)`/`observeSelectedStore()` 존재. `@Unauthenticated StoreApi`, `SelectedStoreHolder`, `@IoDispatcher`.
- `domain/model/Store.kt`(step0 참조, 단순 data class). `core/AppResult.kt`, `core/DomainError.kt`.
- Real 리포는 `BuildConfig.BASE_URL` 키 게이트로만 활성(없으면 Mock). 캐시는 **Real 리포에만** 적용한다(Mock 무변경).

## ⚠ Room 배선
- `app/build.gradle.kts` dependencies에 추가: `implementation(libs.room.runtime)`, `implementation(libs.room.ktx)`,
  `ksp(libs.room.compiler)`. (catalog alias는 이미 존재.)
- Room **계측 DAO 테스트를 컴파일**하려면 androidTest 의존성이 필요하다. catalog(`libs.versions.toml`)에 추가:
  `androidx-test-core`, `androidx-test-ext-junit`, `androidx-test-runner`(androidx.test 1.5.x/ext-junit 1.1.5),
  `room-testing`(version.ref=room). build.gradle.kts에 `androidTestImplementation(...)`로 배선
  (+필요 시 `testInstrumentationRunner` 확인). AC는 androidTest를 **컴파일만** 한다(기기 실행 X).

## ⚠ TDD — 테스트 먼저 (TDD 가드 훅)
새 `src/main/.../X.kt`마다 사전에 대응 테스트가 있어야 한다(step0 참조). 분배 원칙:
- **순수 매퍼 + 리포 캐시 로직은 JVM 단위 테스트**(`src/test`)로 — 핵심 검증을 여기서 한다.
- **Room DAO/Database 실동작은 `src/androidTest`** in-memory Room 테스트로(컴파일만 AC 대상, 실행은 후속 기기검증).
- 작성할 테스트:
  - `StoreCacheMapperTest`(unit): `Store ↔ StoreEntity` 왕복 보존(amenities 리스트 직렬화 포함).
  - `RealStoreRepositoryTest`(unit): **가짜 `StoreLocalDataSource`로** — (a) API 성공 시 캐시에 write-through 후 데이터 emit,
    (b) API 실패 + 캐시 존재 시 **캐시를 `AppResult.Success`로 오프라인 폴백** emit, (c) API 실패 + 캐시 없음 시 `AppResult.Failure` 그대로.
    (MockK로 `StoreApi`/`StoreLocalDataSource` 대체. 기존 좌표 보강 동작 무파손.)
  - `StoreDaoTest`(androidTest): `Room.inMemoryDatabaseBuilder(...).build()`로 upsert→query 라운드트립, replace 시맨틱.

## 만들 것
### 1) DB 토대 — `data/local/db/`
- `CafeDatabase: RoomDatabase`(`@Database(entities=[StoreEntity::class], version = 1, exportSchema = false)`),
  `abstract fun storeDao(): StoreDao`. 향후 step에서 엔티티/버전 증가.
- `di/DatabaseModule.kt`: `@Provides @Singleton` `CafeDatabase`(= `Room.databaseBuilder(context, CafeDatabase::class.java,
  "cafeminsu-cache.db").fallbackToDestructiveMigration().build()` — 캐시라 파괴적 마이그레이션 허용, 주석으로 사유 명시) + `storeDao()` 제공.
- `@TypeConverters`로 `List<StoreAmenity>` 등 컬렉션은 문자열 직렬화(또는 엔티티에 직렬화 컬럼). 단순하게.

### 2) 매장 캐시 — `data/local/store/`
- `StoreEntity`(@Entity tableName="stores", `@PrimaryKey id`, name/address/phone/distanceMeters/lat/lng/
  status(문자열)/closingTimeLabel/amenities(직렬화)). enum은 name 문자열로 저장.
- `StoreDao`(@Dao 인터페이스): `@Upsert suspend fun upsertAll(stores: List<StoreEntity>)`,
  `@Query("SELECT * FROM stores") suspend fun getAll(): List<StoreEntity>`,
  `@Query("DELETE FROM stores") suspend fun clear()`(필요 시).
- `StoreCacheMapper.kt`: 순수 함수 `StoreEntity.toStore()` / `Store.toStoreEntity()`(+리스트 확장).
- `StoreLocalDataSource`(인터페이스) + `RoomStoreLocalDataSource`(@Inject, `StoreDao` 위임):
  `suspend fun cachedStores(): List<Store>`, `suspend fun replaceStores(stores: List<Store>)`.
  리포는 **DAO가 아니라 이 인터페이스에 의존**(단위테스트에서 가짜로 대체).

### 3) RealStoreRepository 통합 (write-through + 오프라인 폴백)
- `observeNearbyStores(query)`: `fetchStores` **성공** 시 `localDataSource.replaceStores(data)`로 캐시 갱신 후 emit
  (기존 좌표 보강 2차 emit 유지). **실패** 시 `cachedStores()`가 비어있지 않으면 `AppResult.Success(cached)` 오프라인 폴백 emit,
  비었으면 기존 `AppResult.Failure` emit.
- `StoreLocalDataSource`를 생성자에 주입. `getStore`/`selectStore`는 그대로(원하면 상세도 캐시 가능하나 이 step은 목록만).

## 하지 말 것
- 도메인(`domain/**`)에 Room/안드로이드 import 금지. Room 엔티티/DAO는 `data/local`에만.
- Mock 리포·`StoreRepository` 인터페이스 시그니처 변경 금지. 좌표 보강(2차 emit) 동작 깨지 마라.
- 금전/주문 생성 등 쓰기 액션에 낙관적 캐시 금지(이 step은 읽기 목록 캐시만).
- hex/새 토큰/매직넘버 금지. 예외 전파 금지(`AppResult`). 기존 테스트 무파손.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공
  (`StoreCacheMapperTest`/`RealStoreRepositoryTest` 통과, `StoreDaoTest`(androidTest) 컴파일, 기존 무파손).
- 매장 목록이 성공 시 캐시되고, 네트워크 실패 시 캐시가 오프라인 폴백으로 나오는 것을 단위테스트로 확인.
- 통과하면 step 1 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
