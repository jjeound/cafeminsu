# Step 2 — 메뉴 오프라인 캐시 (Room)

step1의 `CafeDatabase`를 확장해 **매장별 메뉴를 read-through 캐시**한다. 네트워크 성공 시 write-through,
실패 시 캐시가 있으면 오프라인 폴백. step1의 패턴(매퍼 unit + 리포 캐시 로직 unit + DAO androidTest)을 그대로 따른다.

## 현재 코드 (반드시 일관 유지)
- `data/repository/RealMenuRepository.kt`:
  - `observeCategories()`: `flow { emit(fetchMenuList(null) → toMenuCategories()) }.flowOn(io)`.
  - `observeMenus(categoryId)`: `flow { emit(fetchMenuList(categoryId) → toMenuItems()) }.flowOn(io)`.
  - `fetchMenuList`는 **선택 매장이 있어야** `menuApi.listByStore(storeId, category)` 호출. 선택 매장 없으면
    `AppResult.Success(emptyList())` 폴백(홈 에러 방지) — 이 동작 유지.
  - `getMenu(id)`/`refreshMenus()` 존재. `@Unauthenticated MenuApi`, `SelectedStoreHolder`, `@IoDispatcher`.
- `domain/model/Menu.kt`: `MenuItem(id, categoryId, name, description, basePrice, imageUrl?, isSoldOut,
  options: List<MenuOptionGroup>, isVisible)`, `MenuCategory(id, name, sortOrder)`,
  중첩 `MenuOptionGroup(id, name, required, minSelect, maxSelect, options: List<MenuOption>)`, `MenuOption(id, name, extraPrice, isAvailable)`.
- `data/local/db/CafeDatabase`(step1), `StoreLocalDataSource` 패턴, `di/DatabaseModule.kt`. Moshi 사용 가능.

## ⚠ TDD — 테스트 먼저 (TDD 가드 훅)
- 작성할 테스트:
  - `MenuCacheMapperTest`(unit): `MenuItem ↔ MenuEntity` 왕복 보존. **중첩 옵션(`options`)은 Moshi JSON으로
    직렬화**해 한 컬럼에 저장하고 복원 시 동일해야 함. `MenuCategory`도 캐시한다면 왕복 보존.
  - `RealMenuRepositoryTest`(unit, 가짜 `MenuLocalDataSource`): (a) API 성공 시 선택매장 키로 write-through 후 emit,
    (b) API 실패 + 캐시 존재 시 캐시 오프라인 폴백, (c) 선택 매장 없을 때 기존 `emptyList()` 폴백 유지(캐시 조회 안 함),
    (d) 카테고리 파생/필터 동작 무파손.
  - `MenuDaoTest`(androidTest): in-memory Room upsert/query(storeId 기준), replace.

## 만들 것
### 1) DB 확장 — `data/local/db/`
- `CafeDatabase`에 `MenuEntity`(+필요 시 `MenuCategoryEntity`) 추가, `version`을 2로 올리고 `abstract fun menuDao()`.
  파괴적 마이그레이션 폴백 유지(캐시이므로 OK — 주석).
### 2) 메뉴 캐시 — `data/local/menu/`
- `MenuEntity`(@Entity "menus", `@PrimaryKey id`, `storeId`(인덱스), categoryId/name/description/basePrice/
  imageUrl?/isSoldOut/isVisible, `optionsJson: String`(Moshi로 `List<MenuOptionGroup>` 직렬화)).
- `MenuDao`(@Dao): `@Upsert suspend upsertAll(List<MenuEntity>)`,
  `@Query("SELECT * FROM menus WHERE storeId = :storeId") suspend byStore(storeId): List<MenuEntity>`,
  `@Query("DELETE FROM menus WHERE storeId = :storeId") suspend clearStore(storeId)`.
- `MenuCacheMapper.kt`: 순수 매퍼(`MenuEntity.toMenuItem(moshi)`/`MenuItem.toMenuEntity(storeId, moshi)`). 옵션 JSON 직렬화.
- `MenuLocalDataSource`(인터페이스) + `RoomMenuLocalDataSource`(@Inject, `MenuDao`+`Moshi` 위임):
  `suspend cachedMenus(storeId): List<MenuItem>`, `suspend replaceMenus(storeId, menus: List<MenuItem>)`.

### 3) RealMenuRepository 통합
- `MenuLocalDataSource` 주입. 선택 매장 id가 있을 때만 캐시 사용(없으면 기존 `emptyList()` 폴백 그대로).
- `observeMenus`: API 성공 시 `replaceMenus(storeId, items)` write-through 후 emit. 실패 시 `cachedMenus(storeId)`가
  비어있지 않으면 `AppResult.Success(cached)`(categoryId 필터 적용) 오프라인 폴백, 비었으면 기존 `Failure`.
- `observeCategories`: 캐시된 메뉴에서 카테고리를 파생하거나(폴백 시) 기존 로직 유지 — **카테고리 도출 결과가
  기존과 동일**해야 한다.

## 하지 말 것
- 도메인에 Room/안드로이드 import 금지. Mock 메뉴 리포·`MenuRepository` 시그니처 변경 금지.
- 선택 매장 없을 때 캐시를 강제로 조회하거나 에러 내지 마라(기존 `emptyList()` 폴백 유지).
- hex/새 토큰/매직넘버 금지. 예외 전파 금지. 기존 테스트(step1 포함) 무파손.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공
  (`MenuCacheMapperTest`/`RealMenuRepositoryTest` 통과, `MenuDaoTest`(androidTest) 컴파일, 기존 무파손).
- 메뉴가 성공 시 매장별로 캐시되고, 네트워크 실패 시 캐시가 오프라인 폴백으로 나오는 것을 단위테스트로 확인.
- 통과하면 step 2 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
