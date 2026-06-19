# Step 0 — 장바구니 화면 재설계 (CART, TDD)

> 첨부된 `주문 - 06 (장바구니).png` + `docs/SCREENS.md`(CART)를 그대로 따른다.
> **배경**: 기존 `ui/feature/cart/CartScreen.kt`(phase 4)는 화면 PNG 확정 전 옛 디자인이다. 디자인에 맞게 재작업한다. 주문 생성/수량/합계 도메인 로직은 재사용.

## 바꿀 것 — `ui/feature/cart/`
`docs/SCREENS.md` CART 레이아웃으로 재작업:
- `CafeTopBar`: 좌 `‹`, 중앙 "장바구니". 배경 `canvas`.
- **주문 방식 토글**: "주문 방식" 라벨 + 2분할 [매장에서 먹기] [포장 (픽업)] (선택 강조 within `surface-card` track).
- "주문 항목 (N)" 라벨. 항목 카드(`surface-card`): 좌 썸네일, 메뉴명 `h3` + 옵션 "ICE · Regular" `caption`/`muted`,
  우측 가격 `ink`, 아래 수량 스텝퍼 [− N +].
- **요청사항**: 라벨 + `CafeTextField` placeholder "예) 얼음 적게 부탁드려요".
- 하단: "총 결제 금액" `body` + 큰 금액 `display`/`ink`(우측). 폭 꽉 찬 **"결제하기"** `CafeButton`(primary).
- 빈 장바구니: `EmptyView`("담은 메뉴가 없어요" + "메뉴 보러가기"). 체크아웃 중 버튼 비활성(중복 가드).

## 데이터 (기존 재사용)
- 기존 `CartRepository`/`CartViewModel`/UiState/주문 생성·수량·합계 로직 재사용. 주문 방식(매장/포장)·요청사항 필드가 없으면 도메인 보강(TDD).
- 금액 낙관적 표시 금지(확정 후). 수량/금액 검증 유지(`SECURITY.md`).

## ⚠ TDD — ViewModel 테스트 먼저(추가/변경분)
- 수량 변경→합계 갱신, 주문 방식 토글 상태, 빈 장바구니 Empty, 체크아웃 중복 가드(Turbine). 기존 테스트는 새 구조로 갱신.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 금전 낙관 표시 금지.
- 결제 화면 재작업은 step 1 — 장바구니→결제 진입만 유지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 장바구니가 `주문 - 06`.png 구조(주문 방식 토글·항목 카드·요청사항·하단 총액+결제하기)와 일치한다.
- 통과하면 `phases/13-redesign-cart-pay/index.json`의 step 0 status를 `completed` + `summary` 기록.
