# Step 3 — 네비게이션 골격 (AppNavHost + 하단 탭 + 플레이스홀더)

Navigation Compose 기반의 단일 `AppNavHost`와 하단 탭 골격을 만든다. 각 화면은 아직 **플레이스홀더**다
(제목만 표시). 실제 기능은 이후 feature phase에서 채운다. `ARCHITECTURE.md §네비게이션`과 `PRD.md`의
M-코드/하단 탭 정의를 따른다.

## 만들 것 — `app/src/main/java/com/cafeminsu/ui/navigation/`
1. `Routes.kt` — PRD M-코드와 1:1 라우트 상수:
   `HOME="m01", MENU="m02", MENU_DETAIL="m03", VOICE="m04", CART="m05", PAYMENT="m06",
    ORDER_STATUS="m07", STAMP="m08", GIFTICON="m09", MY="m10"`.
2. `AppNavHost.kt`:
   - `Scaffold`의 `bottomBar`에 하단 탭(**홈 M-01 · 메뉴 M-02 · 스탬프 M-08 · 마이 M-10**) — 4개만.
     탭 바 스펙은 `DESIGN_SYSTEM.md §7.5`(높이 72, 활성 `primary`/비활성 `muted`, 상단 1px `hairline`),
     색은 **토큰(`CafeTheme`/`LocalCafeColors`)으로만** 참조.
   - `NavHost`의 `startDestination = Routes.HOME`.
   - 4개 탭 라우트 + `VOICE(m04)`는 **풀스크린 모달 성격의 라우트**로 등록(탭에는 없음).
     나머지 M-03/05/06/07/09 라우트도 placeholder 컴포저블로 등록만 해 둔다(네비게이션 그래프 완비).
   - 탭 재선택/백스택은 `launchSingleTop = true` + state 복원 기본값 사용.
3. 플레이스홀더 화면 — `ui/feature/<feature>/<Feature>Screen.kt` 형태로 stateless 컴포저블.
   각 화면은 `canvas` 배경 + 화면명을 `h1` 타이포(`ink`)로 좌측 정렬 표시(중앙 정렬 금지, `UI_GUIDE.md`).
   ViewModel/UiState는 이 step에서 만들지 않는다(플레이스홀더는 무상태).
4. `MainActivity` 연결 — `setContent { CafeTheme { AppNavHost() } }`로 교체(step 0의 임시 `Text` 제거).

## 검증 (androidTest 스모크)
- `app/src/androidTest/.../AppNavHostTest.kt` (`createComposeRule` 또는 `createAndroidComposeRule`):
  - 시작 시 홈(M-01) 화면이 보인다.
  - 하단 "메뉴" 탭을 클릭하면 메뉴(M-02) 화면으로 전환된다.
  (계측 테스트가 이 환경에서 실행 불가하면, 최소한 컴파일되는 androidTest를 작성하고 AC는
   `assembleDebug` + `compileDebugAndroidTestKotlin` 성공으로 갈음하라.)

## 하지 말 것
- 실제 메뉴/주문/결제/적립 로직, Repository, ViewModel 구현 금지(플레이스홀더만).
- 하단 탭에 4개 외 항목 추가 금지. hex 리터럴/안티슬롭 요소 금지. 새 색 토큰 추가 금지.

## Acceptance Criteria
- `./gradlew :app:assembleDebug` 와 `./gradlew :app:compileDebugAndroidTestKotlin` 가 성공한다. 직접 실행해 확인하라.
- 앱 진입점이 `CafeTheme { AppNavHost() }`이고, 하단 탭 4개와 M-코드 라우트 그래프가 존재한다.
- 통과하면 `phases/0-bootstrap/index.json`의 step 3 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
