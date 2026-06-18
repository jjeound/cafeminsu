# Step 0 — 장바구니 (M-05, MVVM + TDD)

`PRD.md` M-05(담긴 항목·수량·합계, 주문하기)을 구현한다. 플레이스홀더 `ui/feature/cart/CartScreen`을 채운다.
`CartRepository`(observe/update/remove/validate)와 `OrderRepository`(주문 생성)를 사용한다.
엣지 처리 원칙은 `ARCHITECTURE.md §엣지 케이스`, 상태 4종은 `UI_GUIDE.md`.

## 패턴
- `CartViewModel`(`@HiltViewModel`, `CartRepository`+`OrderRepository` 주입): `StateFlow<CartUiState>`.
- `CartScreen` stateless + `CartRoute` 래퍼(`hiltViewModel()` + 상태 수집 + 네비 콜백).

## 만들 것 — `ui/feature/cart/`
- `CartUiState.kt` — 항목 목록(`CartItem`), 합계(`subtotal`), 최소주문금액, `CartValidation`(Valid/Invalid 사유),
  체크아웃 진행 상태. Loading/Content/**Empty**/Error 포함.
- `CartViewModel.kt`:
  - `observeCart()`로 장바구니 구독 → Content/Empty/Error 매핑(`Failure`→Error, 빈 항목→Empty).
  - `onQuantityChange(cartItemId, qty)`, `onRemove(cartItemId)` → 리포지토리 호출 후 상태 반영.
  - `onCheckout()`: 먼저 `validateForCheckout()`. **Invalid면 사유를 상태로 노출하고 진행하지 않는다**
    (Empty / BelowMinimumAmount(부족액) / SoldOut(해당 항목) / StoreClosed). Valid면 `createOrderFromCart(cart)`
    호출 → 성공 시 생성된 `Order.id`로 **주문 상태(M-07) 라우트로 이동**하는 네비 이벤트 위임, 실패는 Error.
  - **금전 흐름이므로 낙관적 UI 금지**: 체크아웃 처리 중 버튼 비활성 + 중복 탭 가드(진행 플래그).
    (NOTE: 결제 화면 M-06은 phase 5에서 만들며, 그때 cart→결제→주문상태로 라우팅을 재배선한다.
     이 step에서는 임시로 주문 생성 후 M-07로 직접 이동한다.)
- `CartScreen.kt` — 항목 행(이름 `h3`, 옵션 요약 `caption`/`muted`, 단가/소계 `primary`), 수량 스텝퍼,
  삭제, 하단 합계 + `CafeButton`(primary, "주문하기"). BelowMinimum 등은 안내 배너 + 버튼 비활성.
  빈 장바구니는 `EmptyView`("담은 메뉴가 없어요" + "메뉴 보러가기" → 메뉴 탭). 토큰/컴포넌트만 사용.

## ⚠ TDD — ViewModel 테스트 먼저
`CartViewModelTest.kt`(실패 먼저 → 구현):
- 항목이 있으면 Content(합계 정확), 비면 Empty.
- `onQuantityChange`/`onRemove`가 리포지토리에 반영되고 합계가 갱신된다(Turbine).
- `onCheckout`: 빈/최소금액 미달/품절이면 각각의 Invalid 사유를 노출하고 주문 생성을 호출하지 않는다.
  Valid면 `createOrderFromCart` 호출 후 주문 id로 네비 이벤트가 발생한다(MockK 호출검증 또는 Mock 리포 상태).
- 체크아웃 진행 중 재호출(중복 탭)이 차단된다.

## 하지 말 것
- 결제(M-06)·스탬프 적립·음성 구현 금지. 낙관적 결제 표시 금지. hex/새 토큰 금지. 카피 한국어. 중앙 정렬 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(Cart 테스트 포함). `./gradlew :app:assembleDebug` 성공. 직접 실행해 확인하라.
- 통과하면 `phases/4-cart-order/index.json`의 step 0 status를 `completed` + `summary` 기록.
