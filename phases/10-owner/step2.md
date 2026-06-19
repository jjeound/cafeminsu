# Step 2 — 점주 실시간 주문 관리 (OWNER_ORDERS, MVVM + TDD)

> 첨부된 `점주 - 02 (주문 관리).png` + `docs/SCREENS.md`(OWNER_ORDERS)를 그대로 따른다. step 0의 OWNER_ORDERS 플레이스홀더를 본구현으로 교체한다.

## 만들 것 / 바꿀 것 — `ui/feature/owner/orders/`
`docs/SCREENS.md` OWNER_ORDERS 레이아웃을 정확히 구현:
- 상단 "주문 관리" `h1` + 우측 "● 실시간" `caption`(`success` 점).
- **필터 칩 row**(`CafeChip`): "신규 {n}"(기본 선택=`primary`) · "준비중 {n}" · "준비완료 {n}". 개수는 상태별 카운트. 선택 시 목록 필터.
- 주문 카드(`surface-card`): 주문번호 "#1042" + "오후 2:14" + 우측 상태 점, 품목 멀티라인("아메리카노 (L) · ICE · 1 / …"),
  요청 "포장 · 요청: 얼음 적게" `caption`/`muted`, 금액 `primary` + 우측 액션 버튼.
- **상태 전이**: 신규="접수하기"(→준비중), 준비중="준비완료"(→준비완료), 준비완료="픽업완료"(→완료) → `advanceStatus`. 전이 후 해당 필터 카운트/목록 갱신.
- 선택 필터에 항목이 없으면 `EmptyView`("새 주문이 없어요").

## 데이터 (step 1 계약 재사용)
- step 1의 **`OwnerOrderRepository`/`MockOwnerOrderRepository`** 재사용(시드 확장 가능): `observeIncomingOrders(filter)`, `advanceStatus`.
- `OwnerOrdersViewModel`: 선택 필터(StateFlow) + 주문 Flow 결합 → `OwnerOrdersUiState`(Loading/Content(상태별 카운트+필터 목록)/Empty/Error). `Failure`→Error.
  `advanceStatus`는 낙관적 UI 금지(확정 후 반영). 중복 탭 가드.
- `OwnerOrdersScreen` stateless + `OwnerOrdersRoute`(hiltViewModel).

## ⚠ TDD — ViewModel 테스트 먼저
- 상태별 카운트 계산, 필터 선택 시 목록 필터, `advanceStatus`(신규→준비중→준비완료→완료) 후 목록·카운트 전이, Empty/Error 매핑(Turbine).

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 금전·주문 상태 낙관 표시 금지(확정 후).
- 메뉴/매출 화면은 phase 11 — 건드리지 마라.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 주문 관리가 `점주 - 02`.png 구조(필터 칩·주문 카드·상태 액션)와 일치하고, 필터·상태 전이가 동작한다.
- 통과하면 `phases/10-owner/index.json`의 step 2 status를 `completed` + `summary` 기록.
