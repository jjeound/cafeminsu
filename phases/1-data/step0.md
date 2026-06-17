# Step 0 — 도메인 계약 (모델 · Repository 인터페이스 · DataUiState)

`DATA_MODEL.md`(데이터 계약의 단일 진실)를 코드로 옮긴다. 이 step은 **순수 Kotlin 선언**만 만든다
(구현 없음). `ARCHITECTURE.md`의 레이어 규칙(`domain`은 Android 비종속)을 지킨다.

## 만들 것

### 1) 도메인 모델 — `app/src/main/java/com/cafeminsu/domain/model/`
`DATA_MODEL.md §Domain Models`의 모든 타입을 **그대로** 선언한다(필드명·타입 동일):
- Menu: `MenuCategory`, `MenuItem`, `MenuOptionGroup`, `MenuOption`
- Cart: `Cart`, `CartItem`, `SelectedOption`, `CartValidation`(sealed), `CartInvalidReason`(sealed)
- Order/Payment: `Order`, `OrderStatus`(enum), `PaymentRequest`, `PaymentResult`, `PaymentStatus`(enum)
- Rewards: `StampCard`, `StampEvent`, `Gifticon`, `GifticonStatus`(enum)
- Session: `AuthState`(sealed), `UserProfile`

규칙:
- 금액은 `Int`(원 단위). 부동소수점 금지.
- 카드 PAN/CVC/유효기간 필드 금지. 결제수단은 토큰(`paymentMethodToken`)만.
- 파일은 도메인 영역별로 분리(예: `Menu.kt`, `Cart.kt`, `Order.kt`, `Reward.kt`, `Session.kt`).
- Android/`androidx` import 금지.

### 2) Repository 인터페이스 — `app/src/main/java/com/cafeminsu/domain/repository/`
`DATA_MODEL.md §Repository Contracts`의 인터페이스 6개를 **시그니처 그대로** 선언한다:
`MenuRepository`, `CartRepository`, `OrderRepository`, `PaymentRepository`, `RewardRepository`, `SessionRepository`.
- 반환 타입의 `AppResult`는 step 0(bootstrap)에서 만든 `com.cafeminsu.core.AppResult` 를 재사용한다.
- `Flow` 는 `kotlinx.coroutines.flow.Flow`.

### 3) 공통 화면 상태 계약 — `DataUiState`
`com.cafeminsu.core`(AppResult 옆)에 `DATA_MODEL.md §Shared UiState Contract`의 `DataUiState<out T>` sealed
인터페이스를 선언한다(`Loading/Content/Empty/Error/Offline`).

## 하지 말 것
- 구현(Mock/Real Repository), Room, DI, 화면 코드 생성 금지(다음 step 소관).
- 모델/인터페이스에 없는 필드·메서드 임의 추가 금지. `DATA_MODEL.md`에 있는 것만.

## Acceptance Criteria
- `./gradlew :app:compileDebugKotlin` 가 성공한다(새 도메인 코드가 컴파일된다). 직접 실행해 확인하라.
- `./gradlew :app:testDebugUnitTest` 가 여전히 성공한다(기존 테스트 무파손).
- 모든 도메인 코드에 `android.`/`androidx.` import 가 없다(`grep -rn "import android" app/src/main/java/com/cafeminsu/domain` → 없음).
- 통과하면 `phases/1-data/index.json`의 step 0 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
