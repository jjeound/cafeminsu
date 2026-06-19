# Step 0 — 매장 선택 + 매장 상세 (STORE / STORE_DETAIL, MVVM + TDD)

> 첨부된 `주문 - 01.png`(STORE) + `주문 - 02 (매장 선택).png`(STORE_DETAIL) + `docs/SCREENS.md`(STORE, STORE_DETAIL)를 그대로 따른다.
> **배경**: 현재 STORE는 phase 9에서 등록한 제목만 있는 플레이스홀더다. 이번 step에서 본구현으로 교체한다. 주문 탭(STORE) 시작지.
> 도메인 계약은 `docs/DATA_MODEL.md`(StoreRepository, Store)가 단일 진실.

## 만들 것
1. **도메인**(`domain`, `DATA_MODEL.md`): `Store`(id, name, distanceLabel, address, 영업상태/마감시각, 편의시설, 전화 등) + `StoreRepository`
   (`observeNearbyStores(query)`, `getStore(id)`, `selectStore(id)`, `observeSelectedStore()`). `MockStoreRepository`(data, 인메모리 시드 + `@Binds`).
2. **STORE 화면** — `ui/feature/store/`: `StoreViewModel`(StateFlow<StoreUiState>) + `StoreScreen`(`docs/SCREENS.md` STORE):
   헤더 "매장 선택"+"오늘 어디서 한 잔 하실까요?", 검색 입력(`CafeTextField`, 위치핀), **지도 영역 플레이스홀더**(`StoreMap` 라운드 카드 + "내 주변 지도" pill + 코랄 마커 — 실지도 미연동, Box 플레이스홀더),
   "가까운 매장"+"전체 보기", 매장 카드 목록(첫 선택 항목=`surface-dark`, 나머지 `surface-card`; 썸네일·매장명·거리 pill·주소·영업상태 점). 탭 → STORE_DETAIL.
3. **STORE_DETAIL**(바텀시트) — `ui/feature/store/`: 스크림 + 하단 바텀시트(핸들바, `canvas`), 매장명·영업상태, 정보 행(주소/전화/거리/주차),
   편의시설 칩(`CafeChip` 비선택), 하단 "이 매장에서 주문하기" `CafeButton`(primary) → `selectStore` 후 `MENU`(해당 매장).
4. **네비**: phase 9의 STORE 플레이스홀더를 StoreRoute로 교체. STORE_DETAIL은 라우트 또는 동일 화면 내 바텀시트 상태로 구현(기존 네비 그래프와 일관).

## ⚠ TDD — ViewModel/Repository 테스트 먼저
- `MockStoreRepository`: 목록 방출, 검색 필터, `selectStore`→`observeSelectedStore` 반영(Turbine/MockK).
- `StoreViewModel`: 목록 Content/Empty/Error 매핑, 매장 선택 흐름. `Failure`→Error.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 안티슬롭 금지. 실지도 SDK 추가 금지(플레이스홀더).
- 기존 MENU/CART/결제 플로우 로직을 깨지 마라(STORE→MENU 진입만 연결). 메뉴 화면 재작업은 step 1.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 모두 성공. 직접 실행해 확인하라.
- 주문 탭 진입 시 STORE가 `주문 - 01`.png 구조와 일치하고, 매장 탭 → STORE_DETAIL 바텀시트 → "이 매장에서 주문하기" → MENU 로 이어진다.
- 통과하면 `phases/12-redesign-store-menu/index.json`의 step 0 status를 `completed` + `summary` 기록.
