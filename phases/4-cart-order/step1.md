# Step 1 — 주문 완료 / 상태 (M-07, MVVM + TDD)

`PRD.md` M-07(결제 완료, 주문번호, 준비 상태)을 구현한다. 플레이스홀더 `ui/feature/order/OrderStatusScreen`을 채운다.
`OrderRepository.observeOrder(orderId)`를 구독해 주문 진행 상태를 보여준다.

## 네비게이션 (인자 라우트)
- `Routes.ORDER_STATUS`(`m07`)를 **`orderId` 인자** 라우트로 바꾼다(예: `"m07/{orderId}"`),
  `AppNavHost`에 `navArgument` 등록. step 0(cart)의 주문 생성 → `navigate("m07/$orderId")` 와 연결한다.

## 패턴
- `OrderStatusViewModel`(`@HiltViewModel`): `SavedStateHandle`에서 `orderId` → `observeOrder(orderId)`.
  `StateFlow<OrderStatusUiState>`.

## 만들 것 — `ui/feature/order/`
- `OrderStatusUiState.kt` — 주문번호(`orderNumber`), 상태(`OrderStatus`), 항목/합계, 준비 단계 표현.
  Loading/Content/Error 포함.
- `OrderStatusViewModel.kt` — `observeOrder` 결과를 매핑. `Failure`→Error(재시도 가능). 예외 전파 금지.
- `OrderStatusScreen.kt`:
  - 상단 완료/진행 헤더: 결제 완료/주문 접수 시 `display`(예: "주문이 들어갔어요") + 주문번호 강조.
  - `OrderStatus` 단계별 안내(PendingPayment/Paid/Accepted/Preparing/Ready/Completed/Cancelled/Failed)를
    사람이 읽는 한국어 라벨 + 진행 표시(현재 단계 `primary`, 완료 `success`). 항목 요약 `CafeCard`.
  - 토큰/컴포넌트만. 상태 4종은 컴포넌트 사용.

## ⚠ TDD — ViewModel 테스트 먼저
`OrderStatusViewModelTest.kt`(실패 먼저 → 구현):
- 주어진 `orderId`의 주문을 Content로 노출하고 상태 변화가 반영된다(Turbine; Mock 리포의 상태 변경 후 emit 검증).
- 없는 주문/`Failure`면 Error.
- `OrderStatus` → 화면 라벨/단계 매핑이 정확하다(상태 매핑 함수에 대한 순수 단위 테스트 권장).

## 하지 말 것
- 결제(M-06) 실제 처리·스탬프 적립·음성 구현 금지(스탬프 적립 표시는 stamp phase). hex/새 토큰 금지. 카피 한국어.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(OrderStatus 테스트 포함). `./gradlew :app:assembleDebug` + `:app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- M-05(주문하기) → 주문 생성 → M-07(인자 전달) 흐름이 그래프상 연결된다.
- 통과하면 `phases/4-cart-order/index.json`의 step 1 status를 `completed` + `summary` 기록.
