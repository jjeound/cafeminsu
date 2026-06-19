# Step 1 — 메뉴 선택 화면 재설계 (MENU, TDD)

> 첨부된 `주문 - 03 (메뉴 선택).png` + `docs/SCREENS.md`(MENU)를 그대로 따른다.
> **배경**: 기존 `ui/feature/menu/MenuScreen.kt`(phase 3)는 화면 PNG 확정 전 옛 디자인이다. 디자인에 맞게 재작업한다. ViewModel/Repository 데이터는 재사용.

## 바꿀 것 — `ui/feature/menu/`
`docs/SCREENS.md` MENU 레이아웃으로 재작업:
- 헤더: 매장명 "강남점" `h1` + "오늘의 추천 메뉴" `caption`/`muted`. 우상단 검색 아이콘.
- **카테고리 칩 row**(가로 스크롤, `CafeChip`): 추천(선택)·커피·논커피·디저트·티. 선택=`primary`. 선택 시 목록 필터.
- **메뉴 단일 컬럼 리스트**(2열 아님): 행마다 좌측 라운드 썸네일, 메뉴명 `h3`/`ink`(+품절 시 "품절" 태그 `accent-soft`),
  설명 `caption`/`muted`, 가격 `bodyL`. 행 사이 `hairline`. **품절 행은 비활성/딤 + 담기 불가.**
- **음성 주문 FAB**: 우하단 원형 `primary` + 마이크 아이콘 `on-primary` → `VOICE`.
- 탭 → `MENU_DETAIL`. 하단 탭바(주문 활성, 셸은 기존 유지).

## 데이터 (기존 재사용)
- 기존 `MenuRepository`/`MenuViewModel`/`MenuUiState` 재사용. 카테고리 필터가 없으면 ViewModel에 선택 카테고리(StateFlow) + 필터 로직 추가(TDD).
- 선택 매장: step 0의 `StoreRepository.observeSelectedStore()`로 헤더 매장명 표시(없으면 기본값).

## ⚠ TDD — ViewModel 테스트 먼저(추가/변경분)
- 카테고리 선택 시 목록 필터, 품절 메뉴 비활성 매핑, Content/Empty/Error(Turbine). 기존 메뉴 테스트가 구조 변경으로 깨지면 새 구조로 갱신.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 안티슬롭 금지.
- MENU_DETAIL 재작업은 step 2. 장바구니/결제 로직 변경 금지(메뉴→상세 진입만).

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 메뉴 화면이 `주문 - 03`.png 구조(카테고리 칩·단일 컬럼 리스트·음성 FAB·품절 딤)와 일치한다.
- 통과하면 `phases/12-redesign-store-menu/index.json`의 step 1 status를 `completed` + `summary` 기록.
