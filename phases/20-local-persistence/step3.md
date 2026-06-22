# Step 3 — 알림 + 주문내역 오프라인 캐시 (Room)

`CafeDatabase`를 마지막으로 확장해 **알림 목록**과 **고객 주문내역(HISTORY)**을 read-through 캐시한다.
둘 다 인증 필요(Bearer) 흐름이므로 **인증 게이트는 그대로 유지**하고, 네트워크 실패 시에만 캐시를 오프라인 폴백한다.
step1/2 패턴(매퍼 unit + 리포 캐시 로직 unit + DAO androidTest)을 그대로 따른다.

## 현재 코드 (반드시 일관 유지)
- `data/repository/RealNotificationRepository.kt`: `observeNotifications()`는 `ensureAuthenticated()` 후
  `notificationApi.getNotifications()` → `toAppNotifications()`를 emit. 미인증이면 `AppResult.Failure(Unauthorized)` emit하고 종료.
  `markAllRead()` 존재. `SessionStateHolder.authState`로 인증 확인. `@IoDispatcher`.
- `data/repository/RealOrderRepository.kt`: `observeOrderHistory()`는 `ensureAuthenticated()` 후
  `orderApi.getMyOrders(page, size)` → `toOrders()`를 emit. `observeOrder`/`createOrderFromCart`는 **건드리지 않는다**(주문 생성=금전 액션, 캐시 금지).
- `domain/model/AppNotification.kt`: `AppNotification(id, type: NotificationType, title, body, createdAtMillis, read)`,
  `NotificationType { OrderAccepted, OrderReady, OrderCompleted, StampEarned, GifticonReceived }`.
- `domain/model/Order.kt`: `Order(id, orderNumber, items: List<CartItem>, totalAmount, status: OrderStatus, createdAtMillis)`,
  `CartItem(id, menuItemId, name, unitPrice, selectedOptions: List<SelectedOption>, quantity)`(`domain/model/Cart.kt`). Moshi 직렬화 가능.
- `data/local/db/CafeDatabase`(step1·2), `*LocalDataSource` 패턴, `di/DatabaseModule.kt`. Moshi 사용 가능.

## ⚠ TDD — 테스트 먼저 (TDD 가드 훅)
- 작성할 테스트:
  - `NotificationCacheMapperTest`/`OrderCacheMapperTest`(unit): 엔티티↔도메인 왕복 보존
    (`Order.items`/`CartItem.selectedOptions`는 Moshi JSON으로 직렬화·복원).
  - `RealNotificationRepositoryTest`/`RealOrderRepositoryTest`(unit, 가짜 LocalDataSource):
    (a) **미인증이면 캐시 조회·노출 안 하고** 기존 `Unauthorized` Failure 유지(보안),
    (b) 인증+API 성공 시 write-through 후 emit, (c) 인증+API 실패+캐시 존재 시 캐시 오프라인 폴백,
    (d) 인증+API 실패+캐시 없음 시 기존 `Failure`. (주문은 `observeOrderHistory`만; `observeOrder`/`createOrderFromCart` 무변경 확인.)
  - `NotificationDaoTest`/`OrderDaoTest`(androidTest): in-memory Room upsert/query/replace.

## 만들 것
### 1) DB 확장
- `CafeDatabase`에 `NotificationEntity`·`OrderEntity` 추가, `version`을 3으로, `notificationDao()`/`orderHistoryDao()`.
  파괴적 마이그레이션 폴백 유지(주석).
### 2) 알림 캐시 — `data/local/notification/`
- `NotificationEntity`(@Entity "notifications", `@PrimaryKey id`, type(name 문자열)/title/body/createdAtMillis/read).
- `NotificationDao`: upsertAll/getAll(createdAt desc)/clear.
- `NotificationCacheMapper` + `NotificationLocalDataSource`(인터페이스)+`RoomNotificationLocalDataSource`:
  `cachedNotifications()`, `replaceNotifications(list)`.
### 3) 주문내역 캐시 — `data/local/order/`
- `OrderEntity`(@Entity "order_history", `@PrimaryKey id`, orderNumber/totalAmount/status(name)/createdAtMillis,
  `itemsJson: String`(Moshi로 `List<CartItem>` 직렬화)).
- `OrderHistoryDao`: upsertAll/getAll(createdAt desc)/clear.
- `OrderCacheMapper` + `OrderHistoryLocalDataSource`(인터페이스)+`RoomOrderHistoryLocalDataSource`:
  `cachedHistory()`, `replaceHistory(list)`.
### 4) 리포 통합
- `RealNotificationRepository.observeNotifications`: **인증 통과 후에만** — API 성공 시 write-through 후 emit,
  실패+캐시 존재 시 오프라인 폴백, 실패+캐시 없음 시 기존 Failure. 미인증이면 캐시 노출 금지.
- `RealOrderRepository.observeOrderHistory`: 동일 패턴(인증 게이트 유지). `observeOrder`/`createOrderFromCart`는 무변경.
- 각 LocalDataSource를 생성자에 주입.

## 하지 말 것
- **미인증 상태에서 캐시 노출 금지**(다른 사용자 데이터 누출 방지 — 보안). 인증 게이트를 우회하지 마라.
- `observeOrder`/`createOrderFromCart` 등 주문 생성·결제(금전) 경로에 캐시/낙관 적용 금지.
- 도메인에 Room/안드로이드 import 금지. Mock 리포·인터페이스 시그니처 변경 금지.
- hex/새 토큰/매직넘버 금지. 예외 전파 금지(`AppResult`). 기존 테스트(step1·2 포함) 무파손. PII/알림내용 로깅 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공
  (`NotificationCacheMapperTest`/`OrderCacheMapperTest`/`RealNotificationRepositoryTest`/`RealOrderRepositoryTest` 통과,
  `NotificationDaoTest`/`OrderDaoTest`(androidTest) 컴파일, 기존 무파손).
- 알림·주문내역이 인증 하에 캐시되고, 네트워크 실패 시 캐시가 오프라인 폴백으로 나오며, 미인증 시 캐시가 노출되지 않음을 단위테스트로 확인.
- 통과하면 step 3 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라. (마지막 step이면 `index.json`의 phase `completed_at`도 채워라.)
