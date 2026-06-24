# Step 2 — 홈 재주문 누르면 바로 결제 화면으로

## 배경 / 요구
홈(`ui/feature/home/`)의 최근주문 카드에는 **재주문** 버튼이 있다. 현재 `AppNavHost.kt` 의 `Routes.HOME`
`onReorderClick = { menuItemId -> navController.navigate(Routes.menuDetail(menuItemId)) }` 로 **메뉴 상세**로
이동한 뒤 메뉴 화면으로 이어진다. 요구: **홈에서 재주문을 누르면 곧장 결제 화면(`Routes.pay(orderId)`)** 으로
이동한다.

관련:
- 홈 재주문 데이터: `HomeUiState.kt` 의 `HomeRecentOrderSummary(menuItemId, name, totalPrice, ...)`.
- 주문 생성: `domain/repository/OrderRepository.createOrderFromCart(cart): AppResult<Order>`.
- 장바구니: `domain/repository/CartRepository.addItem(menuItemId, options, quantity)` / `observeCart()`.
- 결제 라우트: `Routes.pay(orderId)` (PAY 화면은 orderId 인자로 주문을 관찰).
- 참고: 장바구니→결제 전환은 `ui/feature/cart/CartViewModel` 의 체크아웃 경로를 참고한다.

## 작업 범위 (이 step에서만)
1. **재주문 → 주문 생성 → 결제 이동** 경로를 만든다. 홈 재주문(단일 `menuItemId`)으로 **그 메뉴가 담긴 주문을
   생성**하고 `Routes.pay(orderId)` 로 이동한다.
   - 권장: 재주문 항목(기본 옵션, 수량 1)으로 주문을 만들고 결제로 보낸다. 사용자의 기존 장바구니를 말없이
     비우지 않도록 주의한다(별도 일회성 주문 생성 선호). 구현 방식은 코드베이스 패턴을 조사해 선택한다.
2. **배선**: `HomeViewModel` 에 재주문 액션을 추가해 주문 생성 결과(orderId)를 1회성 이벤트로 노출하고,
   `AppNavHost` 의 HOME `onReorderClick` 이 그 orderId 로 `Routes.pay(orderId)` 로 이동하게 한다.
   (주문 생성은 suspend 이므로 콜백 즉시 네비게이션이 아니라 VM 이벤트 → 화면에서 네비게이션 collect 패턴 사용.)
3. **실패 처리**: 주문 생성 실패는 `UiState.Error`/스낵바 등으로 사용자에게 알리고 결제로 넘어가지 않는다(낙관 금지).
4. 주문내역(`HISTORY`/`HISTORY_DETAIL`)의 재주문은 **기존 동작 유지**(이 step 범위 밖). `menuDetailAddedDestination`
   불변.

## 테스트 (먼저 작성)
- `HomeViewModelTest` 에 재주문 액션 검증: 성공 시 orderId 를 담은 네비게이션 이벤트 emit, 실패 시 에러 처리.
  기존 홈 테스트/페이크 패턴을 따른다.

## 금지 / 불변
- 결제/주문 도메인 계약 불변(읽기/생성만 사용). 금전 액션 낙관적 UI 금지.
- 디자인 토큰/한국어 카피 가드레일 준수.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 2 를 `completed` + `summary` 로 갱신·커밋.
