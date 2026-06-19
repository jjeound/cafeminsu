# 데이터 모델 계약 (CafeMinsu)

본 문서는 phase 분해와 구현 step이 공유하는 엔티티/Repository 계약이다. 제품 범위는 `PRD.md`,
레이어/에러 원칙은 `ARCHITECTURE.md`, 보안 제약은 `SECURITY.md`를 따른다.

## 원칙
- Domain 모델은 Android 프레임워크에 의존하지 않는 Kotlin data class / value class로 둔다.
- Repository와 UseCase의 외부 호출 결과는 `AppResult<T>`를 반환한다.
- 금액은 `Int` 원 단위 또는 `Money(amount: Int, currency: "KRW")`로 표현하고 부동소수점은 금지한다.
- 카드 PAN/CVC/유효기간은 모델에 두지 않는다. 결제수단은 PG/Mock이 발급한 토큰만 참조한다.
- Session/Auth 모델은 토큰 값을 UI 상태나 로그에 노출하지 않는다.

## Domain Models

### Menu
```kotlin
data class MenuCategory(
    val id: String,
    val name: String,
    val sortOrder: Int,
)

data class MenuItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val basePrice: Int,
    val imageUrl: String?,
    val isSoldOut: Boolean,
    val options: List<MenuOptionGroup>,
)

data class MenuOptionGroup(
    val id: String,
    val name: String,
    val required: Boolean,
    val minSelect: Int,
    val maxSelect: Int,
    val options: List<MenuOption>,
)

data class MenuOption(
    val id: String,
    val name: String,
    val extraPrice: Int,
    val isAvailable: Boolean,
)
```

### Cart
```kotlin
data class Cart(
    val items: List<CartItem>,
    val subtotal: Int,
    val minimumOrderAmount: Int,
    val validation: CartValidation,
)

data class CartItem(
    val id: String,
    val menuItemId: String,
    val name: String,
    val unitPrice: Int,
    val selectedOptions: List<SelectedOption>,
    val quantity: Int,
)

data class SelectedOption(
    val groupId: String,
    val optionId: String,
    val name: String,
    val extraPrice: Int,
)

sealed interface CartValidation {
    data object Valid : CartValidation
    data class Invalid(val reasons: List<CartInvalidReason>) : CartValidation
}

sealed interface CartInvalidReason {
    data object Empty : CartInvalidReason
    data class BelowMinimumAmount(val shortage: Int) : CartInvalidReason
    data class SoldOut(val menuItemId: String) : CartInvalidReason
    data class PriceChanged(val menuItemId: String, val latestPrice: Int) : CartInvalidReason
    data class OptionUnavailable(val optionId: String) : CartInvalidReason
    data object StoreClosed : CartInvalidReason
}
```

### Order / Payment
```kotlin
data class Order(
    val id: String,
    val orderNumber: String,
    val items: List<CartItem>,
    val totalAmount: Int,
    val status: OrderStatus,
    val createdAtMillis: Long,
)

enum class OrderStatus {
    PendingPayment,
    Paid,
    Accepted,
    Preparing,
    Ready,
    Completed,
    Cancelled,
    Failed,
}

data class PaymentRequest(
    val orderId: String,
    val amount: Int,
    val paymentMethodToken: String,
    val idempotencyKey: String,
)

data class PaymentResult(
    val orderId: String,
    val paymentId: String,
    val status: PaymentStatus,
    val approvedAtMillis: Long?,
)

enum class PaymentStatus {
    Pending,
    Approved,
    Failed,
    Cancelled,
    Unknown,
}
```

결제 타임아웃/네트워크 끊김 시 `PaymentStatus.Unknown`을 성공으로 취급하지 않는다.
동일 주문의 재시도는 같은 `idempotencyKey`로 상태 조회 후 확정한다.

### Rewards / Gifticon
```kotlin
data class StampCard(
    val userId: String,
    val currentCount: Int,
    val goalCount: Int,
    val history: List<StampEvent>,
)

data class StampEvent(
    val id: String,
    val orderId: String,
    val count: Int,
    val createdAtMillis: Long,
)

data class Gifticon(
    val id: String,
    val title: String,
    val barcodeValue: String,
    val qrValue: String,
    val expiresAtMillis: Long,
    val status: GifticonStatus,
)

enum class GifticonStatus {
    Available,
    Used,
    Expired,
}
```

기프티콘 바코드/QR 값은 클립보드에 자동 복사하지 않고 로그에 남기지 않는다.

### Session / Profile
```kotlin
sealed interface AuthState {
    data object Unknown : AuthState
    data object Guest : AuthState
    data class Authenticated(val user: UserProfile) : AuthState
    data object Expired : AuthState
}

data class UserProfile(
    val id: String,
    val displayName: String,
    val phoneLast4: String?,
)
```

로그인 제공자와 UX는 미확정이지만, 보호 화면은 `Guest`/`Expired` 상태를 구분하고 재로그인 경로로 유도한다.

## Repository Contracts

```kotlin
interface MenuRepository {
    fun observeCategories(): Flow<AppResult<List<MenuCategory>>>
    fun observeMenus(categoryId: String? = null): Flow<AppResult<List<MenuItem>>>
    suspend fun getMenu(menuItemId: String): AppResult<MenuItem>
    suspend fun refreshMenus(): AppResult<Unit>
}

interface CartRepository {
    fun observeCart(): Flow<AppResult<Cart>>
    suspend fun addItem(menuItemId: String, options: List<SelectedOption>, quantity: Int): AppResult<Cart>
    suspend fun updateQuantity(cartItemId: String, quantity: Int): AppResult<Cart>
    suspend fun removeItem(cartItemId: String): AppResult<Cart>
    suspend fun validateForCheckout(): AppResult<CartValidation>
    suspend fun clear(): AppResult<Unit>
}

interface OrderRepository {
    suspend fun createOrderFromCart(cart: Cart): AppResult<Order>
    fun observeOrder(orderId: String): Flow<AppResult<Order>>
    fun observeOrderHistory(): Flow<AppResult<List<Order>>>
}

interface PaymentRepository {
    suspend fun pay(request: PaymentRequest): AppResult<PaymentResult>
    suspend fun getPaymentStatus(orderId: String, idempotencyKey: String): AppResult<PaymentResult>
}

interface RewardRepository {
    fun observeStampCard(): Flow<AppResult<StampCard>>
    suspend fun grantStampsForPaidOrder(orderId: String): AppResult<StampCard>
    fun observeGifticons(): Flow<AppResult<List<Gifticon>>>
    suspend fun getGifticon(id: String): AppResult<Gifticon>
    suspend fun markGifticonUsed(id: String): AppResult<Gifticon>
}

interface SessionRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun refreshOnce(): AppResult<AuthState>
    suspend fun clearSession(): AppResult<Unit>
}
```

## Local Persistence
- Room 캐시는 메뉴, 주문 내역, 스탬프 내역, 기프티콘 목록만 저장한다.
- 토큰/세션은 Room, SharedPreferences, 파일에 저장하지 않고 EncryptedDataStore 또는 Keystore 래핑 저장소만 사용한다.
- Mock Repository는 위 인터페이스를 그대로 구현하고, Real Repository 전환은 Hilt `@Binds` 모듈에서만 바꾼다.

권장 Entity:
- `MenuCategoryEntity`, `MenuItemEntity`, `MenuOptionGroupEntity`, `MenuOptionEntity`
- `CartItemEntity`, `SelectedOptionEntity`
- `OrderEntity`, `OrderItemEntity`
- `StampEventEntity`, `GifticonEntity`

## Shared UiState Contract
데이터를 부르는 화면은 최소 상태 4종을 제공한다.

```kotlin
sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Content<T>(val data: T) : DataUiState<T>
    data class Empty(val message: String) : DataUiState<Nothing>
    data class Error(val message: String, val retryable: Boolean) : DataUiState<Nothing>
    data class Offline<T>(val cached: T?) : DataUiState<T>
}
```

화면별 sealed `UiState`를 쓰는 경우에도 위 4상태 의미를 빠뜨리지 않는다. 결제 화면은 처리 중 상태를 별도로 두되,
서버/Mock 확정 전에는 성공 화면으로 전환하지 않는다.

---

## 재설계 추가 모델 (`docs/screens` 반영)
아래는 화면 디자인 정렬을 위해 추가되는 계약이다. 기존 모델/원칙(금액 Int·AppResult·토큰화 등)을 그대로 따른다.

### Store / 매장
```kotlin
data class Store(
    val id: String,
    val name: String,            // "카페민수 강남점"
    val address: String,
    val phone: String,
    val distanceMeters: Int,     // 현재 위치 기준
    val latitude: Double,
    val longitude: Double,
    val status: StoreStatus,
    val closingTimeLabel: String?, // "22:00 마감"
    val amenities: List<StoreAmenity>, // 콘센트/Wi-Fi/드라이브스루/테라스 등
)

enum class StoreStatus { Open, ClosingSoon, Closed }
enum class StoreAmenity { Outlet, Wifi, DriveThru, Terrace, Parking }
```

### Order 확장 (주문 방식·요청사항·매장)
```kotlin
enum class OrderType { DineIn, Takeout }   // 매장에서 먹기 / 포장(픽업)
// Order 에 추가: val storeId: String, val orderType: OrderType, val requestNote: String?,
//               val estimatedReadyMinutes: Int?
```
`Cart`/체크아웃은 `orderType`과 `requestNote`(예: "얼음 적게")를 보유한다.

### Coupon / Stamp (쿠폰 화면 통합)
```kotlin
data class Coupon(
    val id: String,
    val type: CouponType,
    val title: String,           // "무료 음료 1잔 쿠폰" / "₩10,000"
    val amount: Int?,            // 금액형이면 원, 무료음료면 null
    val expiresAtMillis: Long,
    val status: CouponStatus,
)
enum class CouponType { FreeDrink, Amount }
enum class CouponStatus { Available, Used, Expired }
// 스탬프는 기존 StampCard 재사용(매장별 currentCount/goalCount). 쿠폰 화면은 StampCard + List<Coupon>.
```

### Membership / Profile 확장
```kotlin
enum class MembershipTier { Basic, Silver, Gold }
// UserProfile 에 추가: val tier: MembershipTier, val orderCount: Int,
//                      val stampCount: Int, val couponCount: Int
```

### Notification / 알림
```kotlin
data class AppNotification(
    val id: String,
    val type: NotificationType,  // OrderReady/OrderAccepted/StampEarned/GifticonReceived/OrderCompleted
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val read: Boolean,
)
enum class NotificationType { OrderAccepted, OrderReady, OrderCompleted, StampEarned, GifticonReceived }
```

### Gift / 선물하기
```kotlin
data class GiftSendRequest(
    val amount: Int,             // 금액형 기프티콘 (5000/10000/20000/직접입력)
    val channel: GiftChannel,    // KakaoTalk / Sms
    val recipientRef: String,    // 카카오 친구 식별 또는 연락처(저장·로깅 최소화)
    val message: String?,
)
enum class GiftChannel { KakaoTalk, Sms }
data class GiftSendResult(val giftId: String, val sentAtMillis: Long)
```

### Auth / 로그인 (카카오)
```kotlin
interface LoginProvider {                      // 실(Kakao)/Mock 교체 가능
    suspend fun login(): AppResult<AuthState>  // 성공 시 Authenticated(UserProfile)
    suspend fun logout(): AppResult<Unit>
}
```

### 점주(Owner) 모델 — 역할·인증·운영
고객(카카오)과 분리된 **아이디/비밀번호** 인증. 비밀번호는 모델/로그/디스크에 두지 않고(메모리 전용·전송 후 폐기),
세션 토큰만 EncryptedDataStore에 저장한다(`SECURITY.md`). 주문 상태는 기존 `OrderStatus`(Accepted/Preparing/Ready/Completed)를 재사용한다.
```kotlin
enum class UserRole { Customer, Owner }        // AuthState.Authenticated 가 보유(기본 Customer)

data class OwnerProfile(
    val id: String,
    val storeId: String,
    val storeName: String,        // "민수 강남점"
    val loginId: String,          // 아이디만 보유 — 비밀번호/해시 미보유
    val isStoreOpen: Boolean,     // 대시보드 "영업중" 토글
)

// 매출·정산 (OWNER_SALES)
enum class SalesPeriod { Today, Week, Month }
data class TopMenu(val rank: Int, val name: String, val soldCount: Int, val sales: Int)
data class SalesSummary(
    val period: SalesPeriod,
    val totalSales: Int,          // 원
    val orderCount: Int,
    val deltaPercent: Int?,       // 직전 동기간 대비(±)
    val dailySales: List<Int>,    // 막대 차트용(요일/일자별 원)
    val topMenus: List<TopMenu>,
    val payoutAmount: Int,        // 정산 예정 금액(원)
    val payoutDateLabel: String?, // "6월 24일 입금 예정"
)
// MenuItem 에 추가: val isVisible: Boolean   // 메뉴 노출 on/off (품절은 기존 isSoldOut 재사용)
```

## 추가 Repository Contracts
```kotlin
interface StoreRepository {
    fun observeNearbyStores(query: String? = null): Flow<AppResult<List<Store>>>
    suspend fun getStore(storeId: String): AppResult<Store>
    suspend fun selectStore(storeId: String): AppResult<Unit>   // 현재 주문 매장 설정
    fun observeSelectedStore(): Flow<Store?>
}

interface CouponRepository {                 // 또는 RewardRepository 확장
    fun observeCoupons(): Flow<AppResult<List<Coupon>>>
    suspend fun useCoupon(id: String): AppResult<Coupon>
}

interface NotificationRepository {
    fun observeNotifications(): Flow<AppResult<List<AppNotification>>>
    suspend fun markAllRead(): AppResult<Unit>
}

interface GiftRepository {
    suspend fun sendGift(request: GiftSendRequest): AppResult<GiftSendResult>  // 실Kakao/Mock
}

// ---- 점주(Owner) 계약 ----
interface OwnerAuthProvider {                  // 실/Mock 교체. 비밀번호는 저장·로깅 금지
    suspend fun login(loginId: String, password: String): AppResult<OwnerProfile>
    suspend fun logout(): AppResult<Unit>
    suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile>   // 영업중 토글
}

interface OwnerOrderRepository {               // OWNER_ORDERS 실시간 주문 관리
    fun observeIncomingOrders(filter: OrderStatus? = null): Flow<AppResult<List<Order>>>
    suspend fun advanceStatus(orderId: String, to: OrderStatus): AppResult<Order> // 접수→준비완료→픽업완료
}

interface OwnerMenuRepository {                // OWNER_MENU 메뉴 관리
    fun observeManagedMenus(categoryId: String? = null): Flow<AppResult<List<MenuItem>>>
    suspend fun setSoldOut(menuItemId: String, soldOut: Boolean): AppResult<MenuItem>
    suspend fun setVisible(menuItemId: String, visible: Boolean): AppResult<MenuItem>
}

interface SalesRepository {                    // OWNER_SALES 매출·정산
    fun observeSales(period: SalesPeriod): Flow<AppResult<SalesSummary>>
}
```
- 신규 Mock 리포지토리(StoreRepository/CouponRepository/NotificationRepository/GiftRepository/OwnerAuthProvider/
  OwnerOrderRepository/OwnerMenuRepository/SalesRepository)는 기존 패턴대로 인메모리 시드 + `@Binds`로 DI 연결한다.
  실연동(카카오/지도/선물/점주 인증·주문 푸시)은 키 게이트 + 폴백(키 부재 시 Mock)으로 둔다.
- 선물 수신자(연락처·카카오 친구)·토큰은 저장·로깅 최소화(`SECURITY.md §4`). **점주 비밀번호는 모델/로그/디스크 미보유**, 세션 토큰만 EncryptedDataStore.
