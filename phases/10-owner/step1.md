# Step 1 — 점주 대시보드 (OWNER_HOME, MVVM + TDD)

> 첨부된 `점주 - 01 (대시보드).png` + `docs/SCREENS.md`(OWNER_HOME)를 그대로 따른다. step 0의 OWNER_HOME 플레이스홀더를 본구현으로 교체한다.

## 만들 것 / 바꿀 것 — `ui/feature/owner/home/`
`docs/SCREENS.md` OWNER_HOME 레이아웃을 정확히 구현(위→아래):
- 상단: 매장명 **"강남점 ▾"** `h1`(매장 선택은 표시만) + 우측 **"● 영업중" 토글 pill** → `OwnerAuthProvider.setStoreOpen(open)`로 토글, `isStoreOpen` 반영.
- "6월 19일 (금)" `caption`/`muted` + **"오늘의 매장 현황"** `h2`.
- **3 stat 카드**(`surface-card`): 오늘 매출 "₩482,000" · 주문 "37건" · 신규 대기 "3건"(신규 대기 값 `primary`).
- **"지금 처리할 주문"** `h2` + "전체 보기 →"(`primary`) → `OWNER_ORDERS`. 처리 대기 주문 카드 리스트:
  주문번호·시각·상태 점("● 신규" `warning` / "● 준비중" `primary`)·품목요약·금액 `primary`·우측 액션 버튼("접수하기"/"준비완료") → `advanceStatus`.

## 데이터 (도메인 계약)
- **`OwnerOrderRepository`**(`DATA_MODEL.md`) + `MockOwnerOrderRepository`(data, 인메모리 시드 + `@Binds`): `observeIncomingOrders(filter)`, `advanceStatus(orderId, to)`(접수→준비중→준비완료→픽업완료). 주문은 기존 `Order`/`OrderStatus` 재사용.
- 대시보드 3 stat(오늘 매출/주문수/신규 대기)은 **오늘 주문 데이터에서 파생**(매출=합계, 주문=건수, 신규 대기=신규 상태 건수) — 별도 SalesRepository 의존 금지(매출·정산은 phase 11).
- `OwnerHomeViewModel`: 주문 Flow + 영업 상태 결합 → `OwnerHomeUiState`(Loading/Content/Empty/Error). `Failure`→Error, 예외 전파 금지. `advanceStatus`/`setStoreOpen`는 낙관적 UI 금지(확정 후 반영).
- `OwnerHomeScreen` stateless + `OwnerHomeRoute`(hiltViewModel + 네비 콜백: 전체보기→OWNER_ORDERS).

## ⚠ TDD — ViewModel 테스트 먼저
- 주문 데이터 → 3 stat/처리 대기 목록 매핑. `advanceStatus` 후 목록·카운트 갱신. `setStoreOpen` 토글 반영. `Failure`→Error. 빈 주문 처리.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 중앙 정렬 금지. 안티슬롭 금지. 금전 수치 낙관 표시 금지.
- 주문 관리 본구현(필터/전체 리스트)은 step 2 — 여기선 "지금 처리할 주문" 요약 + 전체보기 라우트 연결.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` 성공. 직접 실행해 확인하라.
- 대시보드가 `점주 - 01`.png 구조(매장명/영업중 토글/3 stat/지금 처리할 주문)와 일치하고, 접수/준비완료·영업 토글이 동작한다.
- 통과하면 `phases/10-owner/index.json`의 step 1 status를 `completed` + `summary` 기록.
