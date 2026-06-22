# Step 0 — 우선순위 스케줄링 코어 엔진 + 사장님 주문 정렬 교체

피크 타임 주문 폭주 시 단순 선착순(FIFO) 대신 **우선순위 점수로 처리 순서를 결정**하는 스케줄링
엔진을 도메인에 만들고, 사장님 주문 화면(`OWNER_ORDERS`)의 정렬을 이 엔진으로 교체한다.
**비콘/AI는 이 step에서 다루지 않는다(다음 step).** proximity 입력은 `null` 허용 슬롯만 둔다.

엔진은 **순수 Kotlin(안드로이드 비종속)** 이어야 하며 결정론적이다 → 단위 테스트로 완전 검증한다.
`docs/ARCHITECTURE.md`(레이어/UDF/에러), `CLAUDE.md`(토큰·매직넘버·TDD)를 따른다.

## 현재 코드 (반드시 일관 유지)
- 정렬 교체 지점: `app/src/main/java/com/cafeminsu/ui/feature/owner/orders/OwnerOrdersViewModel.kt`
  (현재 `.filter { it.status == selectedFilter.status }.sortedByDescending { it.createdAtMillis }
  .map { it.toOwnerOrdersOrderUiModel(processingOrderIds) }`).
- 주문 모델 `domain/model/Order.kt`: `Order(id, orderNumber, items: List<CartItem>, totalAmount,
  status: OrderStatus, createdAtMillis: Long)`. `CartItem(id, menuItemId, name, unitPrice,
  selectedOptions: List<SelectedOption>, quantity)`(`domain/model/Cart.kt`).
- UiState: `OwnerOrdersUiState.Content(selectedFilter, counts, orders: List<OwnerOrdersOrderUiModel>)`,
  필터 enum `New(Accepted)/Preparing/Ready`. 매핑 확장 `Order.toOwnerOrdersOrderUiModel(...)`.
- 디스패처 `@IoDispatcher`(`di/DispatcherModule.kt`), 결과 `core/AppResult.kt`.

## ⚠ TDD — 테스트를 먼저 작성하라 (실패 테스트 우선)
순수 도메인이므로 JUnit 단위 테스트로:
- **정렬**: 대기시간이 길수록·수량이 많을수록 우선순위가 높아지는지(가중치 부호대로).
- **기아 방지(aging)**: 아주 오래 대기한 주문이 결국 최상단으로 올라오는지.
- **동점 처리**: 점수 동일 시 `createdAtMillis` 오름차순(먼저 들어온 주문 먼저)으로 안정 정렬.
- **ETA**: `estimatedReadyAtMillis = now + (앞 순번들의 prepSeconds 합 + 본인 prepSeconds)*1000` 계산.
- **혼잡도**: 활성 주문(Accepted+Preparing) 수 → `CongestionLevel`(Low/Mid/High) 경계값.
- **제조시간 규칙**: `RulePrepTimeEstimator`가 base + 항목수·수량 + 옵션수로 단조 증가.
- **proximity=null**: 근접 입력이 없을 때도 정상 동작(근접 기여 0).

## 만들 것
### 1) 도메인 모델·가중치 — `domain/scheduling/`
- `SchedulingSignals(orderId: String, waitingSeconds: Long, prepSeconds: Int, quantity: Int,
  congestion: CongestionLevel, proximity: ProximityInput? = null, expectedPickupAtMillis: Long? = null)`.
  - 이 step에서는 `ProximityInput`을 **최소 형태로** 도메인에 정의(`ProximityInput(estimatedArrivalSeconds:
    Int, rssi: Int)`) — 다음 step의 비콘 신호가 이걸 채운다. 지금은 항상 `null`로 전달돼도 된다.
- `CongestionLevel { Low, Mid, High }`.
- `SchedulingBadge { ArrivingSoon, Urgent, Normal }`.
- `SchedulingWeights(...)` — **모든 계수를 명명된 프로퍼티 기본값으로**(로직에 매직넘버 금지):
  `waitingWeight`, `quantityWeight`, `prepWeight`, `proximityWeight`, `pickupWeight`,
  `congestionWeight`, `agingThresholdSeconds`(Urgent 판정), `arrivingSoonSeconds`(ArrivingSoon 판정),
  `prepBaseSeconds`/`prepPerItemSeconds`/`prepPerOptionSeconds`(규칙 추정용).
- `ScheduledOrder(order: Order, priorityScore: Double, estimatedReadyAtMillis: Long,
  badge: SchedulingBadge)`.

### 2) 엔진·계산기 — `domain/scheduling/`
- `OrderScheduler`(`@Inject constructor(weights: SchedulingWeights = SchedulingWeights())`,
  순수 함수): `fun schedule(orders: List<Order>, signals: Map<String, SchedulingSignals>, nowMillis: Long):
  List<ScheduledOrder>`.
  - `priorityScore = waitingWeight*waitingSeconds + quantityWeight*quantity + prepWeight*prepSeconds
    + proximityWeight*proximityUrgency + pickupWeight*pickupUrgency + congestionWeight*congestion.ordinal`.
    - `proximityUrgency`: proximity==null → 0.0, else 도착이 임박할수록 큰 단조감소 함수
      (예: `arrivingSoonSeconds`초 이내면 (arrivingSoonSeconds - estimatedArrivalSeconds)/arrivingSoonSeconds 를
      0..1 로 clamp). **근거 주석 달 것.**
    - `pickupUrgency`: expectedPickupAtMillis==null → 0.0, else 마감이 가까울수록 증가.
  - **높은 점수 = 먼저 처리**(내림차순). 동점은 `createdAtMillis` 오름차순으로 안정 정렬.
  - **ETA**: 정렬된 순서대로 prepSeconds 누적 → `estimatedReadyAtMillis`.
  - **badge**: proximity가 `arrivingSoonSeconds` 이내 → `ArrivingSoon`; 아니고 waitingSeconds ≥
    `agingThresholdSeconds` → `Urgent`; 그 외 `Normal`.
  - signals에 해당 orderId가 없으면 안전한 기본 신호(prep는 추정기로, congestion은 전달값)로 보정.
- `CongestionCalculator`: `fun level(activeOrderCount: Int): CongestionLevel`(경계는 weights/상수로).
- `PrepTimeEstimator` 인터페이스: `fun estimateSeconds(order: Order): Int`.
  `RulePrepTimeEstimator(weights)` 구현: `prepBaseSeconds + Σ(prepPerItemSeconds +
  prepPerOptionSeconds*옵션수) * quantity`. **다음 step의 AI 추정기가 이 인터페이스를 구현/대체**한다.

### 3) 시간 추상화 — `domain/time/Clock.kt` + Hilt
- `interface Clock { fun nowMillis(): Long }`. 구현 `SystemClock`(`System.currentTimeMillis()`).
- `di/SchedulingModule.kt`(또는 적절한 모듈)에서 `Clock`·`PrepTimeEstimator`·`SchedulingWeights` 바인딩/제공.
  엔진은 Hilt로 주입 가능해야 한다(`@Inject`/`@Provides`). 테스트는 가짜 `Clock`/고정 `nowMillis` 사용.

### 4) ViewModel·UiState·화면 통합
- `OwnerOrdersViewModel`: `OrderScheduler`·`Clock`·`PrepTimeEstimator`·`CongestionCalculator` 주입.
  - 들어온 주문 리스트로 `SchedulingSignals`를 구성(waitingSeconds = (now - createdAtMillis)/1000,
    quantity = items 수량 합, prepSeconds = 추정기, congestion = 활성 주문 수 기반, proximity = null).
  - **`sortedByDescending { createdAtMillis }`를 `orderScheduler.schedule(...)` 결과 순서로 교체.**
    필터(New/Preparing/Ready) 동작은 그대로 유지. 처리중(processingOrderIds) 표시도 유지.
- `OwnerOrdersUiState.OwnerOrdersOrderUiModel`에 `priorityBadge: SchedulingBadge`(또는 표시용 enum)와
  `etaLabel: String?`(예 "약 4분") 추가. 매핑 확장 함수에서 ScheduledOrder로부터 채운다.
- `OwnerOrdersScreen`의 주문 카드에 **뱃지(Urgent/ArrivingSoon)와 ETA**를 노출. 색·치수·문구는
  **디자인 토큰(`CafeTheme.*`)만**, hex 금지, 카피 한국어("긴급", "약 N분" 등). 과한 장식 금지(`UI_GUIDE.md`).

## 하지 말 것
- 비콘/BLE/권한/AI/LLM 관련 코드 금지(다음 step). proximity는 `null` 슬롯만 둔다.
- `Order`/`OrderStatus`/`CartItem` 기존 필드 시그니처 변경 금지(필드 추가가 필요하면 기본값으로 하위호환).
- 도메인(`domain/scheduling`, `domain/time`)에 안드로이드 import 금지(순수 Kotlin).
- hex 리터럴·새 색/토큰 신설·매직넘버 금지(계수는 `SchedulingWeights`에). 새 결과 타입 금지(예외→`AppResult`).
- 기존 테스트·기존 사장님 주문 동작(상태 전이 버튼) 무파손.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공
  (`OrderSchedulerTest`/`CongestionCalculatorTest`/`RulePrepTimeEstimatorTest`/`OwnerOrdersViewModelTest`
  + 기존 무파손).
- 사장님 주문 목록이 FIFO가 아니라 우선순위 점수 순으로 정렬되고, 카드에 뱃지·ETA가 표시됨을 테스트로 확인.
- 통과하면 step 0 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
