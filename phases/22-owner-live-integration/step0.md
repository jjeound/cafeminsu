# Step 0 — 점주 주문 실서버 연동 (RealOwnerOrderRepository)

## 배경
점주 주문(`OwnerOrderRepository`)은 현재 `MockOwnerOrderRepository` 만 있고 실서버에 연결돼 있지 않다.
서버에는 점주 주문 관리 엔드포인트가 **모두 존재**하므로 `Real*` 패턴(다른 `RealXxxRepository` 와 동일)으로
실연동하고 `BASE_URL` 키게이트(`selectXxxRepository`)로 Mock 폴백을 둔다. **도메인 인터페이스/모델/UI 는
변경하지 않는다**(UI 무변경 목표). 계약 단일 진실은 `docs/SERVER_INTEGRATION.md` + `docs/openapi.json`.

## 서버 계약 (openapi.json 확인됨)
- **점주 매장 목록**: `GET api/stores/my` → `List<MyStoreRes{ id:Long, name:String, imageUrl:String }>` (Bearer 필요).
  점주의 `storeId` 는 **여기 첫 매장의 id** 로 해석한다(owner-login 응답엔 storeId 가 없어
  `OwnerProfile.storeId` 는 placeholder=loginId 이므로 신뢰하지 말 것 — 반드시 `stores/my` 로 해석).
- **점주 주문 목록**: `GET api/stores/{storeId}/orders?status=&date=` →
  `List<StoreOrderItemRes{ orderId:Long, orderNumber:String, status:[PENDING|ACCEPTED|READY|DONE|CANCELLED],
  totalAmount:Int, items:List<MenuSummary{ menuId:Long, menuName:String, quantity:Int }>, createdAt:date-time }>`.
- **상태 전이**: `PATCH api/orders/{orderId}/accept` · `PATCH api/orders/{orderId}/ready` ·
  `PATCH api/orders/{orderId}/complete` → 각각 `OrderStatusRes{ status:[...] }` (no-body 가능성 대비 nullable).
  `POST api/orders/{orderId}/cancel` body `OrderCancelReq{ reason:String }` → 응답 본문 없음(2xx=성공).

## 작업 범위 (이 step에서만)
1. **API**: `app/src/main/java/com/cafeminsu/data/remote/OwnerOrderApi.kt` 신설(또는 `OrderApi`/`StoreApi` 에
   메서드 추가 중 일관성 있는 쪽 선택 — 신설 권장). 위 4개 호출 + `getMyStores()` 를 정의하고 DTO
   (`StoreOrderItemRes`, `MenuSummary`, `OrderStatusRes`, `OrderCancelReq`, `MyStoreRes`)를 `@JsonClass(generateAdapter=true)`
   로 둔다. **인증 필요 호출**이므로 인증된 Retrofit(`provideRetrofit`, `@Unauthenticated` 아님)으로 `NetworkModule` 에 provider 추가.
2. **매퍼**: `app/src/main/java/com/cafeminsu/data/mapper/OwnerOrderMapper.kt` (또는 기존 `OrderMapper.kt` 확장).
   - `StoreOrderItemRes` → 도메인 `Order`. `id = orderId.toString()`, `items` 는 `MenuSummary` →
     `CartItem(id="$orderId-$menuId", menuItemId=menuId.toString(), name=menuName, unitPrice=0, selectedOptions=emptyList(), quantity=quantity)`
     (단가는 요약 응답에 없음 → 0). `totalAmount` 는 서버 값. `createdAtMillis` 는 `createdAt`(ISO-8601) 파싱 —
     **기존 `OrderMapper` 의 날짜 파싱 헬퍼를 재사용**(없으면 동일 방식으로).
   - 서버 status 문자열 → 도메인 `OrderStatus` 매핑은 **기존 OrderMapper 의 매핑 헬퍼 재사용**
     (PENDING→Paid, ACCEPTED→Accepted, READY→Ready, DONE→Completed, CANCELLED→Cancelled). 점주 큐는 결제완료 주문이므로 PENDING→Paid.
3. **Repository**: `app/src/main/java/com/cafeminsu/data/repository/RealOwnerOrderRepository.kt`.
   - `@Inject` 생성자: `OwnerOrderApi`, `@IoDispatcher CoroutineDispatcher` (필요시 인증 게이트용 `SessionStateHolder`).
   - `observeIncomingOrders(filter)`: `flow { … }.flowOn(io)`. `getMyStores()` 로 storeId 해석 →
     `getStoreOrders(storeId, status=filter?.toServerStatus())` 호출 → `runCatchingToAppResult { … }` →
     성공 시 `List<Order>` 로 매핑해 emit. **stores/my 가 비어 있으면**(현재 테스트 계정 가능성) `AppResult.Success(emptyList())`
     로 안전 처리(예외/크래시 금지). 모든 외부 호출은 `runCatchingToAppResult` 로 감싼다(예외 전파 금지).
   - `advanceStatus(orderId, to)`: 도메인 target → 서버 호출 매핑:
     `Accepted→accept`, `Ready→ready`, `Completed→complete`, `Cancelled→cancel(reason="")`.
     서버에 대응 없는 `Preparing` 은 **로컬 전이로 간주**(서버 호출 없이 성공 반환) — 사유를 주석으로 명시.
     성공 시 `AppResult<Order>` 반환: 마지막으로 관측한 주문에 새 상태를 반영해 재구성하거나, 필요한 필드만 채운
     `Order`(id/orderNumber/status 등)로 반환. 비-2xx 는 `runCatchingToAppResult` 가 `DomainError` 로 변환.
4. **DI 키게이트**: `RepositoryModule.kt` 에 `provideOwnerOrderRepository(real, mock)` + `selectOwnerOrderRepository(baseUrl, …)`
   를 다른 도메인과 **동일 패턴**으로 추가하고, 기존 `@Binds bindOwnerOrderRepository(MockOwnerOrderRepository)` 는 제거.
5. **테스트(먼저 작성 — TDD)**: `app/src/test/java/com/cafeminsu/data/repository/RealOwnerOrderRepositoryTest.kt`.
   - **MockWebServer** 로 기존 `RealStoreRepositoryTest`/`RealOrderRepositoryTest` 스타일을 그대로 따른다(픽스처=실제 응답 모양).
   - 검증: (a) `stores/my` + `orders` 2회 enqueue 후 목록 매핑/필드 단언, (b) 빈 `stores/my` → emptyList,
     (c) `advanceStatus` 각 전이가 올바른 경로(`takeRequest().path`/`method`)로 가는지 + 매핑 결과, (d) 비-2xx → `AppResult.Failure`.

## 금지 / 불변 (반드시)
- 도메인 모델, `OwnerOrderRepository` **인터페이스 시그니처**, UI/ViewModel 은 변경하지 않는다.
- `MockOwnerOrderRepository` 는 폴백용으로 **유지**(삭제 금지). 다른 도메인의 Real/Mock·DI 는 건드리지 않는다.
- HTTPS 강제·민감값 미로깅 등 보안 규칙 유지(`SECURITY.md`). 모든 외부 호출은 `AppResult` 로 감싼다(예외 전파 금지).
- 카탈로그/포맷(ktlint/detekt) 통과. hex 리터럴·매직 의미값 금지(상수화).

## AC (직접 실행해 BUILD SUCCESSFUL 확인)
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과하면 `phases/22-owner-live-integration/index.json` 의 step 0 을 `completed` + `summary`(한 줄)로 갱신하고 커밋한다.
contract 가 openapi 와 명백히 달라 구현 불가하면 `blocked` + `blocked_reason` 으로 기록 후 즉시 중단한다.
