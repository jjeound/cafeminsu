# Step 0 — 네비게이션 3탭 재편 + 스플래시 + 인증 게이트

> 첨부된 디자인 이미지(`Splash.png`, `홈.png`)와 `docs/SCREENS.md`(SPLASH·HOME 탭바)를 그대로 따른다.

기존 4탭(홈/메뉴/스탬프/마이) 구조를 **디자인의 3탭(홈/주문/MY)**으로 재편하고, 스플래시 → 인증 게이트를 도입한다.
앱이 끝까지 빌드되도록 기존 화면/ViewModel은 유지하되 네비게이션 그래프만 재구성한다.

## 만들 것 / 바꿀 것
1. **`ui/navigation/Routes.kt`** — 라우트 상수를 `docs/PRD.md`/`docs/SCREENS.md` 코드 체계로 재정의:
   `SPLASH, LOGIN, HOME, NOTI, STORE, STORE_DETAIL, MENU, MENU_DETAIL(인자), VOICE, CART, PAY, ORDER_OK,
   ORDER_FAIL, MY, COUPON, GIFT, HISTORY`. (인자 라우트는 기존 방식 유지.)
2. **`ui/navigation/AppNavHost.kt`** 재구성:
   - 최상위 `NavHost` `startDestination = SPLASH`.
   - **인증 게이트**: SPLASH에서 `SessionRepository.observeAuthState()` 확인 → `Authenticated`면 메인(HOME),
     아니면 `LOGIN`으로. (LOGIN 화면 본구현은 step 1 — 여기서는 라우트와 게이트 분기만, LOGIN은 임시 플레이스홀더 허용.)
   - **메인 Scaffold + 하단 탭 3개**(`docs/SCREENS.md` 탭바): **홈(HOME) · 주문(STORE) · MY(MY)**.
     활성 `primary`/비활성 `muted`, 상단 1px `hairline`, height 72. 기존 "메뉴"·"스탬프" 탭 제거.
   - 주문 탭 시작지 = `STORE`(매장 선택). **STORE/STORE_DETAIL 등 주문 플로우 화면 본구현은 phase 10** —
     여기서는 STORE를 플레이스홀더 화면으로 등록(제목만). MENU/MENU_DETAIL/CART/PAY/VOICE 등 기존 화면은 그래프에 유지.
   - MY 탭 = 기존 MyScreen(재작업은 phase 12).
3. **Splash 화면** — `ui/feature/splash/SplashScreen.kt` (`docs/SCREENS.md` SPLASH): 배경 `primary`(코랄),
   중앙 "카페민수" `display`/`on-primary` + "Warm cream coffee". 짧은 fade 후 게이트 분기.
4. **`MainActivity`** — `setContent { CafeTheme { AppNavHost() } }` 유지(이미 그러함). 진입은 SPLASH.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만 사용. hex/새 토큰 금지(카카오 옐로우는 step 1에서). 카피 한국어.
- 기존 ViewModel/화면 로직을 깨지 마라(라우트 연결만 갱신). 로그인 본구현·홈 재작업·알림은 다음 step.
- 기존 네비 테스트가 라우트 변경으로 깨지면 새 구조에 맞게 갱신한다(탭 3개·시작지 SPLASH).

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 모두 성공. 직접 실행해 확인하라.
- 하단 탭이 홈/주문/MY 3개이고, 앱 진입이 SPLASH → (미인증)LOGIN/(인증)HOME 로 분기한다.
- 통과하면 `phases/9-redesign-shell/index.json`의 step 0 status를 `completed` + `summary` 기록.
