# Step 1 — Mock Repository 구현 + 시드 데이터 (데이터, TDD)

step 0의 Repository 인터페이스를 **인메모리 Mock 구현**으로 채운다. 백엔드가 없으므로 MVP는 Mock 우선
(`ARCHITECTURE.md` 데이터 흐름·`DATA_MODEL.md` Local Persistence). 이것은 실제 도메인 로직(가격 합계,
장바구니 검증, 스탬프 적립 등)을 포함하므로 **TDD가 엄격히 적용된다.**

## ⚠ TDD — 테스트를 먼저 작성하라
각 Mock Repository의 동작을 검증하는 **실패 테스트를 `src/test`에 먼저 작성**하고 구현하라.
Flow 검증은 **Turbine**, 필요 시 **MockK**. (두 라이브러리는 bootstrap의 버전 카탈로그에 이미 선언됨 —
없으면 카탈로그/`app` testImplementation 의존성에 추가하라.)

## 만들 것 — `app/src/main/java/com/cafeminsu/data/`
인터페이스 6개의 Mock 구현(`data/repository/Mock*Repository.kt`)과 시드 데이터(`data/mock/MockData.kt` 등).
가변 상태는 `MutableStateFlow`로 보관하고 `observe*`는 그 Flow를 노출한다. 모든 외부 결과는 `AppResult`로 감싼다.

- **MockMenuRepository**: 시드로 카테고리 3개(예: 커피/티/디저트)와 메뉴 아이템 여러 개(옵션 그룹 포함:
  사이즈/온도/샷, 일부 품절 `isSoldOut=true`). `observeMenus(categoryId)`는 필터링, `getMenu`는 없으면
  `Failure(DomainError.NotFound)`.
- **MockCartRepository**: `addItem`은 `unitPrice = basePrice + 선택옵션 extraPrice 합`, `subtotal =
  Σ(unitPrice × quantity)` 으로 재계산. `updateQuantity`(0 이하는 제거 또는 거부 — 명확히), `removeItem`,
  `clear`. `validateForCheckout`/`Cart.validation`:
  - 빈 장바구니 → `Invalid([Empty])`
  - `subtotal < minimumOrderAmount` → `Invalid([BelowMinimumAmount(shortage)])`
  - 품절 항목 포함 → `Invalid([SoldOut(menuItemId)])`
  - 그 외 → `Valid`
- **MockOrderRepository**: `createOrderFromCart` → `Order`(상태 `PendingPayment`, `orderNumber` 생성,
  `totalAmount = cart.subtotal`). `observeOrder`/`observeOrderHistory`.
- **MockPaymentRepository**: `pay`는 유효 토큰이면 `PaymentResult(status=Approved, approvedAtMillis 설정)`.
  `getPaymentStatus(orderId, idempotencyKey)`로 상태 조회. **타임아웃/불명 상태를 성공으로 만들지 마라**
  (Mock은 Approved를 명시적으로 반환). 같은 `idempotencyKey` 재호출은 같은 결과(중복결제 방지).
- **MockRewardRepository**: `grantStampsForPaidOrder`는 `currentCount` 증가 + `StampEvent` 추가(목표 도달 처리).
  `observeStampCard`, `observeGifticons`, `getGifticon`, `markGifticonUsed`(→ `status=Used`).
- **MockSessionRepository**: `observeAuthState`는 기본 `AuthState.Guest`(또는 `Unknown`→`Guest`). `refreshOnce`,
  `clearSession`(→ Guest로 리셋). **토큰 값을 모델/로그에 노출하지 마라.**

## 테스트 (src/test, JUnit4 + Turbine) — 먼저 작성
최소 검증:
- Menu: `observeCategories`가 `sortOrder` 순 비어있지 않은 `Success` 방출, `observeMenus(id)` 필터, `getMenu` NotFound.
- Cart: 옵션 포함 `addItem` 후 `subtotal` 정확, `updateQuantity`/`removeItem` 반영, `validateForCheckout`의
  Empty/BelowMinimumAmount/SoldOut/Valid 각 케이스.
- Reward: `grantStampsForPaidOrder` 후 `currentCount` 증가, `markGifticonUsed` 후 `Used`.
- Order: `createOrderFromCart` 결과 상태/금액/주문번호.
- Payment: `pay` Approved, 같은 idempotencyKey 재호출 일관성.

## 하지 말 것
- Room/네트워크(Retrofit) 실제 연동 금지(이 step은 인메모리 Mock만). DI 모듈/화면 코드 금지(다음 step).
- 예외 throw 금지 — 에러는 `Failure(DomainError)`.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 가 성공한다(새 Mock 테스트 포함 전부 green). 직접 실행해 확인하라.
- 통과하면 `phases/1-data/index.json`의 step 1 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
