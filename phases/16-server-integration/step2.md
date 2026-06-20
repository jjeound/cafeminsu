# Step 2 — 매장 · 메뉴 실연동 (StoreApi/MenuApi + DTO + mapper)

매장 목록/상세와 메뉴 카테고리/목록/상세를 실서버에서 가져온다. `StoreRepository`/`MenuRepository`
를 실서버 구현으로 교체한다(키 게이트 폴백). step 0 의 `NetworkModule`, step 1 의 Auth 인터셉터를 재사용.

> **API 스펙**: `docs/openapi.json` 의 매장/메뉴 엔드포인트를 단일 진실로 한다. DTO 필드·페이징·정렬은
> 스펙 그대로. 도메인 모델은 `DATA_MODEL.md`(`Store`/`StoreStatus`/`StoreAmenity`, `MenuCategory`/
> `MenuItem`/`MenuOptionGroup`/`MenuOption`) 계약을 따른다.

## ⚠ 사전 조건 (없으면 blocked)
- `docs/openapi.json` 에 매장/메뉴 엔드포인트가 없으면 → **blocked** 후 중단.
- `BASE_URL` 부재 시 Mock 폴백 유지(코드/테스트는 MockWebServer로 작성·통과).

## ⚠ TDD — 테스트를 먼저 작성하라
MockWebServer 로 DTO→도메인 매핑, 카테고리 정렬(`sortOrder`), `getMenu` NotFound, 빈 목록 케이스를
**실패 테스트 먼저** 작성.

## 만들 것
### 1) `StoreApi`/`MenuApi` + DTO + mapper — `data/remote/`
- 엔드포인트별 Retrofit 인터페이스 + DTO(스펙 그대로) + DTO→domain 매퍼.
- 매퍼는 `data/mapper/`(또는 remote 하위)에 두고 단위 테스트로 검증.

### 2) `RealStoreRepository` — `data/repository/`
- `observeNearbyStores(query)`/`getStore(id)`/`selectStore(id)`/`observeSelectedStore()`
  (`domain/repository/StoreRepository.kt` 계약). 선택 매장은 메모리/로컬 보관(민감정보 아님).

### 3) `RealMenuRepository` — `data/repository/`
- `observeCategories()`/`observeMenus(categoryId)`/`getMenu(id)`/`refreshMenus()`
  (`domain/repository/MenuRepository.kt` 계약). `getMenu` 미존재 → `Failure(DomainError.NotFound)`.
- (선택) **메뉴 Room 캐시**: `DATA_MODEL.md` Local Persistence 는 메뉴 캐시를 허용한다. 도입 시
  로컬 우선 → 원격 갱신, 오프라인 시 캐시 읽기 전용 노출(`ARCHITECTURE.md`). 토큰/PII 캐시 금지.

### 4) DI 교체 (키 게이트 폴백) — `di/RepositoryModule.kt`
- `StoreRepository`/`MenuRepository` 바인딩을 `BASE_URL` 유무로 Real/Mock 선택. 다른 Repository는 무변경.

## 하지 말 것
- 장바구니/주문/결제 Repository 교체 금지(다음 step). 화면/ViewModel·UI 변경 금지.
- 스펙에 없는 필드·엔드포인트 임의 추가 금지. 새 결과 타입 금지(예외는 step 0 매퍼로 `AppResult`).

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 가 성공한다(매장/메뉴 매핑·정렬·NotFound MockWebServer 테스트 + 기존 무파손).
- `BASE_URL` 부재 시 Mock 폴백으로 정상 구동됨을 테스트로 확인.
- 통과하면 step 2 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
