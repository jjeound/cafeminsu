# Step 1 — 결제 플로우 통합 (장바구니 → 결제 → 주문상태 + 스탬프 적립)

표준 주문 플로우(`PRD.md`)를 완성한다: **M-05 장바구니 → M-06 결제 → M-07 주문 상태**, 결제 승인 시 **스탬프 자동 적립**.
phase 4에서 장바구니가 임시로 M-07로 직접 가던 것을 **M-06 경유로 재배선**한다.

## 만들 것 / 바꿀 것
1. **네비게이션 재배선** (`ui/navigation/AppNavHost.kt`):
   - 장바구니(M-05) `onCheckout` 성공(주문 생성) → `navigate("m06/$orderId")` (결제로). phase 4의 직접 M-07 이동 제거.
   - 결제(M-06) 승인 성공 → `navigate("m07/$orderId")` (주문 상태로), 백스택에서 결제/장바구니는 적절히 정리
     (`popUpTo`로 결제 성공 후 뒤로가기가 결제 재진입이 되지 않게).
2. **스탬프 적립** — 결제 `Approved` 시 `RewardRepository.grantStampsForPaidOrder(orderId)` 호출.
   - `PaymentViewModel`(step 0)에 `RewardRepository` 주입을 추가하고, 승인 직후 적립을 수행한 뒤 주문 상태로 이동.
   - 적립은 결제 확정 **후**에만(낙관 금지). 적립 실패가 결제 성공을 되돌리지 않도록 처리(적립 실패는 비치명적 로깅/후속 재시도 대상).
3. M-07 주문 상태는 결제 후 진입 시 결제 완료 맥락(주문번호 등)을 보여준다(이미 phase 4 구현 — 필요한 미세 조정만).

## ⚠ TDD — 먼저 작성
- `PaymentViewModelTest`(step 0) 확장 또는 신규: 승인 시 `grantStampsForPaidOrder(orderId)`가 호출되고,
  그 **후** 주문상태 네비 이벤트가 발생한다. 적립 실패가 발생해도 결제 성공/이동은 유지된다(MockK 검증).
- 가능하면 `AppNavHostTest`(androidTest)에 장바구니→결제→주문상태 경로 스모크 추가(계측 불가 시 컴파일로 갈음).

## 하지 말 것
- 실제 PG 연동 금지(Mock 유지). 스탬프 화면(M-08) UI는 다음 phase. hex/새 토큰 금지. 카피 한국어.
- 결제 전 단계로의 뒤로가기로 중복 결제가 가능해지지 않게 백스택을 정리하라.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공. `./gradlew :app:assembleDebug` + `:app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 장바구니 주문하기 → 결제(Mock 승인) → 주문 상태, 그리고 승인 시 스탬프 적립이 호출되는 흐름이 그래프/코드상 연결된다.
- 통과하면 `phases/5-payment/index.json`의 step 1 status를 `completed` + `summary` 기록.
