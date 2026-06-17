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
