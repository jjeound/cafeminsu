# Step 1 — 장바구니 담긴 수량 뱃지 표시 (메뉴 화면 카트 버튼)

## 배경
주문(메뉴) 화면 `ui/feature/menu/MenuScreen.kt` 의 우하단 `CartFloatingButton` 은 장바구니 아이콘만 보여줄 뿐
**담긴 수량을 표시하지 않는다.** 사용자가 장바구니에 몇 개 담겨 있는지 한눈에 알 수 있도록 카트 버튼에 수량 뱃지를 추가한다.

장바구니 수량 원본: `domain/repository/CartRepository.observeCart(): Flow<AppResult<Cart>>`,
`Cart.items: List<CartItem>`, `CartItem.quantity: Int`.

## 작업 범위 (이 step에서만)
1. **ViewModel**: `MenuViewModel` 에 `CartRepository` 를 **추가 주입**하고 `cartItemCount: StateFlow<Int>` 를 노출한다.
   - `cartRepository.observeCart()` 구독 → `AppResult.Success` 면 `cart.items.sumOf { it.quantity }`(담긴 **총 수량**),
     `AppResult.Failure` 또는 예외면 `0`. `.catch { emit(0) }` 로 예외를 흡수한다.
   - `stateIn`(WhileSubscribed) 로 노출하고 초기값 `0`. **기존 `uiState` 흐름은 변경하지 않는다**(StateFlow 추가만).
2. **UI 배선**: `MenuRoute` → `MenuScreen` → `CartFloatingButton` 으로 `cartItemCount: Int` 를 전달한다.
   - `cartItemCount > 0` 일 때만 카트 버튼 **우상단에 작은 원형 뱃지**(숫자)를 겹쳐 표시한다. `99` 초과는 `"99+"`.
   - 색·치수·타이포는 **디자인 토큰만** 사용한다(예: 뱃지 배경 `colors.primary` 또는 `colors.surfaceDark`,
     텍스트 `colors.onPrimary`/`colors.onDark`, 타이포 `caption`/`meta`, 모양 `shapes.radiusPill`). **hex 금지.**
   - 뱃지에 접근성 `contentDescription`(예: `"장바구니 ${count}개"`) 를 부여하거나 카트 버튼 description 에 수량을 반영한다.
   - 뱃지가 버튼 밖으로 약간 튀어나와도 클립되지 않도록 배치(필요 시 카트 버튼을 감싸는 `Box` 사용).
3. **테스트(먼저 작성)**: `MenuViewModelTest` 에 `cartItemCount` 검증을 추가한다 —
   빈 카트 → `0`, 항목 수량 합산(예: 수량 2 + 3 → `5`), 카트 실패 → `0`. 테스트용 `FakeCartRepository`(=`CartRepository`
   가짜 구현, `observeCart` 를 제어 가능한 Flow 로) 를 추가한다. 기존 `MenuViewModelTest` 의 패턴/페이크를 따른다.
   가능하면 `MenuScreenTest`(androidTest) 에 뱃지 표시 케이스를 추가하되, 최소한 컴파일은 유지한다.

## 금지 / 불변
- 장바구니 도메인/리포지토리/주문/결제 로직을 변경하지 않는다(**읽기 전용** 구독만 추가).
- `MenuViewModel`/`MenuScreen` 의 기존 공개 동작·`MenuUiState` 형태를 유지한다(카운트는 별도 StateFlow/파라미터).
- 디자인 토큰·hex·한국어 카피 가드레일 준수.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 1 을 `completed` + `summary` 로 갱신·커밋.
