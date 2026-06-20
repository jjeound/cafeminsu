# Step 3 — 장바구니 · 주문 실연동 (OrderApi + DTO + mapper)

주문 생성과 주문 상태/내역을 실서버에 붙인다. `OrderRepository`(필요 시 `CartRepository`)를 실서버
구현으로 교체한다(키 게이트 폴백). step 0/1 토대·인터셉터 재사용.

> **API 스펙**: `docs/openapi.json` 의 장바구니/주문 엔드포인트를 단일 진실로 한다. 서버가 카트를
> 보유하는지(서버 카트) 아니면 클라 카트+주문 생성만 받는지 스펙으로 판단해 구현 방식을 정한다.
> 도메인 모델은 `DATA_MODEL.md`(`Order`/`OrderStatus`/`OrderType`, `Cart`/`CartItem`) 계약을 따른다.

## ⚠ 사전 조건 (없으면 blocked)
- `docs/openapi.json` 에 주문 엔드포인트가 없으면 → **blocked** 후 중단.
- `BASE_URL` 부재 시 Mock 폴백 유지(코드/테스트는 MockWebServer로 작성·통과).

## ⚠ TDD — 테스트를 먼저 작성하라
MockWebServer 로 주문 생성(상태/금액/주문번호), 상태 조회, 내역 목록, 에러(`AppResult.Failure`)를
**실패 테스트 먼저** 작성. 금액은 서버 값으로 확정하되 클라 계산과 불일치 시 처리(스펙 기준).

## 만들 것
### 1) `OrderApi` + DTO + mapper — `data/remote/`
- 주문 생성/조회/내역 엔드포인트 + DTO(스펙 그대로) + DTO→domain 매퍼.

### 2) `RealOrderRepository` — `data/repository/`
- `createOrderFromCart(cart)` → `Order`(서버가 부여한 `id`/`orderNumber`/`status`/`totalAmount`).
- `observeOrder(orderId)`/`observeOrderHistory()`(`domain/repository/OrderRepository.kt` 계약).
- 상태 폴링/스트림 방식은 스펙을 따른다(소켓 미지원 시 폴링). 쓰기 작업은 오프라인 시 차단.

### 3) (스펙이 서버 카트를 요구할 때만) `RealCartRepository` — `data/repository/`
- 서버 카트면 `addItem`/`updateQuantity`/`removeItem`/`validateForCheckout`/`clear` 를 실서버로.
- 서버가 카트를 안 가지면 **CartRepository 는 그대로 Mock/로컬 유지**하고 주문 생성만 서버로 보낸다.

### 4) DI 교체 (키 게이트 폴백) — `di/RepositoryModule.kt`
- `OrderRepository`(+필요 시 `CartRepository`) 바인딩을 `BASE_URL` 유무로 Real/Mock 선택. 결제·기타 무변경.

## 하지 말 것
- 결제 Repository 교체 금지(다음 step). 화면/ViewModel·UI 변경 금지.
- 주문 생성에서 낙관적 성공 금지 — 서버 확정 응답으로만 `Order` 상태 확정. 새 결과 타입 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 가 성공한다(주문 생성/상태/내역·에러 MockWebServer 테스트 + 기존 무파손).
- `BASE_URL` 부재 시 Mock 폴백으로 정상 구동됨을 테스트로 확인.
- 통과하면 step 3 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
