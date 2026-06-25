# Step 1 — 점주 메뉴 실서버 연동 (RealOwnerMenuRepository)

## 배경
점주 메뉴(`OwnerMenuRepository`)는 현재 `MockOwnerMenuRepository` 만 있다. 서버에 목록/생성/판매토글
엔드포인트가 존재하므로 `Real*` 패턴으로 실연동하고 `BASE_URL` 키게이트로 Mock 폴백을 둔다. step 0
(점주 주문)과 일관된 구조를 유지한다(특히 `stores/my` 로 storeId 해석). 계약 단일 진실은
`docs/SERVER_INTEGRATION.md` + `docs/openapi.json`.

## 서버 계약 (openapi.json 확인됨 — 일부 DTO 는 기존 재사용)
- **점주 매장 id**: step 0 와 동일하게 `GET api/stores/my` → 첫 매장 id. step 0 에서 만든 `getMyStores()`/`MyStoreRes`
  를 **재사용**한다(중복 정의 금지).
- **메뉴 목록**: `GET api/stores/{storeId}/menus?category=` → `List<MenuListItemRes>`.
  ⚠ `MenuListItemRes{ id, name, price, category, imageUrl, isAvailable }` 와 `MenuApi.listByStore` 는
  **이미 `MenuApi.kt` 에 존재** → 재사용(고객용과 동일 응답). 점주 전용 Api 에서 같은 DTO 를 재정의하지 말 것.
- **메뉴 생성**: `POST api/stores/{storeId}/menus` body
  `MenuCreateReq{ name, description, price:Int, category, imageUrl, isAvailable:Boolean }` → `MenuCreateRes{ menuId:Long }`.
- **판매 토글**: `PATCH api/menus/{menuId}/availability` body `MenuAvailabilityReq{ isAvailable:Boolean }` → 응답 본문 없음(2xx=성공).
- **가시성(setVisible) 엔드포인트는 없음** → 로컬 전용으로 처리(서버 호출 없이 성공 반환, 사유 주석). `SERVER_INTEGRATION.md` "범위 밖" 참조.

## 작업 범위 (이 step에서만)
1. **API**: `OwnerMenuApi.kt` 신설 — `createMenu(storeId, MenuCreateReq): MenuCreateRes`,
   `setAvailability(menuId, MenuAvailabilityReq)`(반환 Unit/no-body). 목록은 **기존 `MenuApi.listByStore`** 사용
   (필요하면 `RealOwnerMenuRepository` 에 `MenuApi` 주입). 인증 필요 호출이므로 인증 Retrofit provider 로
   `NetworkModule` 에 추가.
2. **매퍼**: `MenuListItemRes` → 도메인 `MenuItem` 매핑(`categoryId=category`, `basePrice=price`,
   `isSoldOut = isAvailable == false`, `isVisible = true`, `options=emptyList()`, `description=""`).
   기존 `MenuMapper.kt` 에 고객용 매핑이 있으면 재사용/확장(점주 화면이 기대하는 `MenuItem` 필드와 일치시킬 것).
3. **Repository**: `RealOwnerMenuRepository.kt`.
   - `observeManagedMenus(categoryId)`: `stores/my` → storeId → `listByStore(storeId, categoryId)` → `List<MenuItem>` emit.
     빈 stores/my → emptyList 안전 처리.
   - `setSoldOut(menuItemId, soldOut)`: `setAvailability(menuId, MenuAvailabilityReq(isAvailable = !soldOut))` 호출 →
     성공 시 갱신된 `MenuItem`(마지막 관측분에 soldOut 반영) 반환.
   - `setVisible(menuItemId, visible)`: 서버 엔드포인트 없음 → 로컬 전용 성공 반환(주석으로 사유 명시).
   - `addMenu(draft)`: `MenuCreateReq` 로 변환 후 `createMenu` → 반환된 `menuId` 로 `MenuItem` 구성(draft + 서버 id) 반환.
   - 모든 외부 호출 `runCatchingToAppResult` 로 감싸고 예외 전파 금지.
4. **DI 키게이트**: `RepositoryModule.kt` 에 `provideOwnerMenuRepository` + `selectOwnerMenuRepository` 추가,
   기존 `@Binds bindOwnerMenuRepository(MockOwnerMenuRepository)` 제거.
5. **테스트(먼저 작성)**: `RealOwnerMenuRepositoryTest.kt` (MockWebServer, 기존 스타일).
   목록 매핑/빈 처리, `setSoldOut` 의 PATCH 경로·바디(`isAvailable=false`), `addMenu` 의 POST 경로·바디·`menuId` 반영,
   `setVisible` 로컬 성공, 비-2xx → Failure 단언.

## 금지 / 불변
- 도메인 모델, `OwnerMenuRepository` 인터페이스, UI/ViewModel 변경 금지. `MockOwnerMenuRepository` 유지.
- 다른 도메인의 Real/Mock·DI·step 0 산출물은 건드리지 않는다(단, step 0 의 `getMyStores`/`MyStoreRes` 는 재사용).
- 보안/포맷 규칙 유지. 외부 호출은 `AppResult` 로 감싼다.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 1 을 `completed` + `summary` 로 갱신·커밋. 구현 불가 시 `blocked` + `blocked_reason` 후 중단.
