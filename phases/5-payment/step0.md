# Step 0 — 결제 (M-06, MVVM + TDD) — Mock PG, 보안 우선

`PRD.md` M-06(결제수단 선택, 금액 확인, 결제 실행)을 구현한다. 플레이스홀더 `ui/feature/payment/PaymentScreen`을 채운다.
**실제 PG 키 없이** 기존 `MockPaymentRepository`(data phase)로 동작시킨다(ADR-006의 Mock 결제 경로 → 이 step은 blocked 아님).
`SECURITY.md §3`·`ARCHITECTURE.md §결제 안전 처리`가 최우선 규칙이다.

## 보안/안전 규칙 (엄수)
- **PAN/CVC/유효기간 입력·저장·로깅 금지.** 결제수단은 PG/Mock이 발급한 **토큰만** 사용
  (Mock 결제수단 = 라벨 + 토큰, 예: "민수페이"→`tok_minsupay`, "카드(등록됨)"→`tok_card_demo`). 카드번호 입력 UI 만들지 마라.
- **멱등키**: 결제 시도마다 클라이언트 UUID `idempotencyKey` 생성. **동일 주문 재시도는 같은 키 재사용**(중복결제 방지).
- **낙관적 성공 금지**: `pay()` 결과가 `Approved`일 때만 성공 처리. `Failed`/`PaymentStatus.Unknown`/타임아웃은
  성공으로 취급하지 않는다. 불명 상태는 `getPaymentStatus(orderId, idempotencyKey)`로 확정 후 화면 갱신.
- **중복 제출 가드**: 처리 중 결제 버튼 비활성 + 진행 플래그로 더블탭 차단.
- 결제 민감정보·토큰을 로그/화면/상태에 노출하지 않는다.

## 네비게이션 (인자 라우트)
- `Routes.PAYMENT`(`m06`)를 **`orderId` 인자** 라우트로 바꾼다(예: `"m06/{orderId}"`), `AppNavHost`에 `navArgument` 등록.
  (cart→payment, payment→order-status 배선은 step 1에서.)

## 패턴 / 만들 것 — `ui/feature/payment/`
- `PaymentViewModel`(`@HiltViewModel`, `SavedStateHandle`의 `orderId` + `PaymentRepository`+`OrderRepository` 주입):
  `StateFlow<PaymentUiState>`.
- `PaymentUiState.kt` — 주문 금액/항목 요약, 선택 가능한 Mock 결제수단 목록, 선택된 수단, **결제 진행 상태**
  (Idle/Processing/Approved/Failed/NeedsConfirmation 등 명시적). Loading/Content/Error 포함.
- `PaymentViewModel.kt`:
  - 주문(`observeOrder`/조회)으로 금액 확정 후 결제수단 선택 노출.
  - `onSelectMethod(token)`, `onPay()`: `Processing`로 전환(버튼 비활성) → `PaymentRequest(orderId, amount,
    paymentMethodToken, idempotencyKey)`로 `pay()`. `Approved`만 성공 이벤트(주문 id) 위임. `Failed`→사유 안내+재시도.
    `Unknown`/예외→`getPaymentStatus`로 재확인, 확정 안 되면 사용자 재시도 유도(자동 재시도 금지).
  - 재시도는 **같은 idempotencyKey**.
- `PaymentScreen.kt` — 금액/항목 요약 `CafeCard`, 결제수단 선택(라디오/`CafeChip`), 하단 `CafeButton`(primary,
  "{금액}원 결제"). 처리 중 인디케이터+버튼 비활성. 실패 시 명확한 한국어 안내+재시도. 토큰/컴포넌트만.

## ⚠ TDD — ViewModel 테스트 먼저
`PaymentViewModelTest.kt`(실패 먼저 → 구현):
- `Approved` 응답 → 성공 이벤트(주문 id) 발생.
- `Failed`/`Unknown` 응답 → 성공으로 전환되지 않고 에러/재확인 상태가 된다(낙관 금지 회귀 방지).
- 동일 주문 재시도가 **같은 idempotencyKey**를 사용한다(MockK 인자 검증 또는 캡처).
- 처리 중 `onPay()` 재호출(더블탭)이 무시된다.

## 하지 말 것
- 실제 PG SDK/키 연동·카드번호 입력 UI 금지. 스탬프 적립·cart 재배선은 step 1. hex/새 토큰 금지. 카피 한국어.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(Payment 테스트 포함). `./gradlew :app:assembleDebug` 성공. 직접 실행해 확인하라.
- 통과하면 `phases/5-payment/index.json`의 step 0 status를 `completed` + `summary` 기록.
