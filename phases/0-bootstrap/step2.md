# Step 2 — 디자인 시스템 테마 (CafeTheme + 토큰)

`DESIGN_SYSTEM.md`(스펙 단일 진실)를 Jetpack Compose 테마로 1:1 구현한다.
**hex 리터럴은 오직 `ui/theme/Color.kt` 한 곳에만** 존재하고, 그 외 모든 코드는 토큰명으로만 색을 참조한다.

## 만들 것 — `app/src/main/java/com/cafeminsu/ui/theme/`
1. `Color.kt` — `DESIGN_SYSTEM.md §2`의 **모든 토큰**을 `Color(0x..)`로 정의(이 파일이 hex의 유일한 거처):
   `canvas, ink, body, muted, mutedSoft, primary, surfaceCard, surfaceDark, hairline,
   primaryHover, accentSoft, onPrimary, onDark, success, warning, error`.
   값은 `DESIGN_SYSTEM.md §2` 표 그대로(예: `canvas = Color(0xFFFAF9F5)`, `primary = Color(0xFFCC785C)`).
2. `CafeColors.kt` — 위 토큰을 묶는 `data class CafeColors(...)`와 `val LocalCafeColors =
   staticCompositionLocalOf<CafeColors> { error("...") }`. 기본 인스턴스 `cafeLightColors()` 제공.
3. `Type.kt` — `DESIGN_SYSTEM.md §3`의 스케일(`display, h1, h2, h3, bodyL, body, caption, meta`)을
   `TextStyle`로. 폰트 패밀리는 Noto Sans KR 단일(리소스 폰트가 없으면 시스템 sans-serif로 두되 weight는 매핑).
   크기/라인하이트/weight는 §3 표 그대로(매직넘버 대신 의미 있는 스타일명).
4. `Shape.kt` — `radius-sm/md/lg/xl/pill`(8/12/16/24/999)을 `RoundedCornerShape`로.
5. `Spacing.kt` — `space-1..18`(4/8/12/16/20/24/32/40/56/72)을 `Dp` 상수로.
6. `Elevation.kt` — `elev-card`, `elev-overlay`(§6).
7. `CafeTheme.kt` — `@Composable fun CafeTheme(content)`:
   - `MaterialTheme`를 베이스로 쓰되 **`colorScheme`를 토큰으로 오버라이드**한다
     (`primary=primary`, `background=canvas`, `surface=surfaceCard`, `onPrimary=onPrimary` 등).
     **Material 기본 보라/인디고가 노출되면 안 된다**(`UI_GUIDE.md` 안티슬롭).
   - `CompositionLocalProvider(LocalCafeColors provides cafeLightColors())`로 토큰을 제공.
   - `MaterialTheme(colorScheme = ..., typography = ..., shapes = ..., content = content)`.
   - 편의 접근자 `object CafeTheme { val colors @Composable get() = LocalCafeColors.current }` 권장.

## 검증 (androidTest 또는 unit)
- `ui/theme/` 컴파일 + `MainActivity`가 임시로 `CafeTheme { Text(...) }`를 감싸도 빌드된다.
- 가능하면 Compose UI 테스트(`createComposeRule`)로 `CafeTheme` 안에서
  `MaterialTheme.colorScheme.primary`가 토큰 `primary`와 같고 기본 보라(`#6650a4` 계열)가 아님을 확인.
  (계측 테스트 환경 제약이 있으면 빌드 성공 + 코드 리뷰로 갈음하되, hex가 `Color.kt` 밖에 없음을 보장하라.)

## 하지 말 것
- `Color.kt` 외 파일에 hex 리터럴 작성 금지. 새 색/토큰 임의 추가 금지(`DESIGN_SYSTEM.md`에 있는 것만).
- 글래스모피즘/그라데이션 텍스트/네온 글로우 등 안티슬롭 요소(`UI_GUIDE.md`) 금지.
- 컴포넌트(Button/Card 등)나 화면은 만들지 마라 — 이 step은 **테마 토큰까지만**.

## Acceptance Criteria
- `./gradlew :app:assembleDebug` 가 성공한다. 직접 실행해 확인하라.
- `Color.kt` 외에는 hex 리터럴이 없다(직접 grep으로 확인: `grep -rn "0xFF" app/src/main` → `Color.kt`만).
- 통과하면 `phases/0-bootstrap/index.json`의 step 2 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
