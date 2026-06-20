# Step 4 — 결제 실연동 (PG prepare→verify · merchantUid 멱등 · 낙관 금지)

결제를 실서버에 붙인다. **서버 결제는 PG 2단계 흐름**이다(단순 `pay()` 아님):
`prepare → (클라 PG SDK로 impUid 발급) → verify → 상태 확정`. `PaymentRepository` 를 실서버 구현으로
교체한다(키 게이트 폴백). 금전 액션이므로 보안·정합성 최우선: `ARCHITECTURE.md`(결제 안전 처리) ·
`SECURITY.md §3` · `DATA_MODEL.md` · **`SERVER_INTEGRATION.md`(결제 섹션)**.

> **API 스펙**: `docs/openapi.json` + `SERVER_INTEGRATION.md`. 엔드포인트:
> `POST api/payments/prepare {orderId, useGifticonId?, gifticonAmount?, cardAmount?}` → `{merchantUid, amount}`,
> `POST api/payments/verify {impUid, merchantUid}` → `{paymentId, status:READY|PAID|FAILED|REFUNDED}`,
> `GET api/payments/{paymentId}` → 상태 확정. 모두 Bearer 필요. 카드 PAN/CVC/유효기간은 보유·로깅 금지.

## ⚠ 사전 조건 — 결정됨: PG는 Mock impUid 추상화 (이 step은 blocked 아님)
- 서버 verify 는 PG 제공자가 발급한 `impUid` 를 요구하지만, **이번 phase에서는 실 PG SDK를 붙이지 않는다.**
  PG 호출부를 `PgClient` 인터페이스로 추상화하고 **Mock 구현(성공/실패 impUid 분기)** 으로 둔다.
  prepare/verify/상태조회는 **실서버**로 호출한다. 실 PG SDK·상점키 연동은 **후속 phase**(여기서 만들지 마라).
- 따라서 이 step은 키 부재로 `blocked` 처리하지 마라. `BASE_URL` 부재 시에만 Mock 폴백(테스트는 MockWebServer).

## ⚠ TDD — 테스트를 먼저 작성하라
MockWebServer 로: prepare 성공(merchantUid/amount), verify PAID→Approved, FAILED→Failed,
**동일 주문 재시도가 같은 merchantUid 사용**(중복결제 방지), **READY/미확정을 성공으로 만들지 않음**,
`payment detail` 로 상태 확정을 **실패 테스트 먼저** 작성.

## 만들 것
### 1) `PaymentApi` + DTO + mapper — `data/remote/`
- prepare / verify / payment-detail 엔드포인트 + DTO(스펙 그대로) + 매퍼.
- 상태 매핑: 서버 `READY|PAID|FAILED|REFUNDED` → 도메인 `PaymentStatus`(PAID→Approved, FAILED→Failed,
  REFUNDED→Cancelled, READY→Pending). 금액은 `Int` 원 단위.

### 2) PG 호출 추상화 — `data/payment/`
- `interface PgClient { suspend fun authorize(merchantUid, amount): AppResult<String /*impUid*/> }` 형태로
  PG SDK 호출을 감싼다. 실제 PG SDK 미연동 시 Mock 구현(성공 impUid / 실패 분기)로 둔다(현재 앱의
  Mock PG 분기와 동일 철학). 카드 민감정보는 PgClient 밖으로 나오지 않게 한다.

### 3) `RealPaymentRepository` — `data/repository/`
- `pay(request)`: prepare → `PgClient.authorize` → verify → 결과. 도메인 매핑:
  `request.idempotencyKey ≈ merchantUid`(동일 주문 재시도 동일 키), `paymentMethodToken ≈ impUid`.
- `getPaymentStatus(orderId, idempotencyKey)`: `payment detail`/조회로 상태 확정.
- **타임아웃/미확정 → 낙관적 성공 금지.** 확정(`PAID`) 전 성공 반환 금지. 자동 재시도(비멱등) 금지.

### 4) DI 교체 (키 게이트 폴백) — `di/RepositoryModule.kt`
- `PaymentRepository` 바인딩을 `BASE_URL` 유무로 Real/Mock 선택. 다른 Repository 무변경.

## 하지 말 것
- 결제 성공을 서버 verify 확정 전에 화면/상태로 전환 금지(낙관 금지). 비멱등 자동 재시도 금지.
- 카드 PAN/CVC/유효기간 저장·로깅·모델 보유 금지(`SECURITY.md §3`). 새 결과 타입 금지(`AppResult`만).
- 화면/ViewModel·UI 변경 금지(결제 화면의 처리중/확정 흐름은 기존 계약 유지).

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 가 성공한다(prepare/verify/상태확정·중복 merchantUid·미확정 비성공
  MockWebServer 테스트 + 기존 무파손).
- 동일 주문 재시도가 같은 merchantUid 로 나감을 테스트로 확인.
- `BASE_URL` 부재 시 Mock 폴백으로 정상 구동됨을 테스트로 확인.
- 통과하면 step 4 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
