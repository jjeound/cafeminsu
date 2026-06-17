# Step 0 — 핵심 디자인 시스템 컴포넌트 (Button · Card · Chip · TopBar)

화면이 raw `Box`/`Button`에 색을 직접 칠하지 않도록, `DESIGN_SYSTEM.md §7` 스펙대로 **재사용 컴포넌트**를
`app/src/main/java/com/cafeminsu/ui/components/`에 만든다. 색·치수·타이포는 **반드시 `CafeTheme`/
`LocalCafeColors` 토큰**으로만 참조한다(hex 리터럴 금지 — hex는 `ui/theme/Color.kt`에만).

## 만들 것 — `ui/components/`
1. **`CafeButton`** (`DESIGN_SYSTEM §7.1`):
   - `enum class CafeButtonVariant { Primary, Secondary, Ghost, Dark }`.
   - height **52**, shape `radius-lg`(16), 가로 패딩 `space-5`(20), 아이콘+텍스트 gap `space-2`(8).
   - variant별 배경/텍스트/보더/pressed 색은 §7.1 표 그대로(예: Primary = bg `primary`/text `on-primary`/
     pressed `primary-hover`; Secondary = bg `canvas`/text `ink`/1px `hairline`; Ghost = transparent/text
     `primary`/pressed `accent-soft`; Dark = bg `surface-dark`/text `on-dark`).
   - `enabled=false` 비활성 시각 처리(토큰 기반), 터치 타깃 ≥48dp.
2. **`CafeCard`** (`DESIGN_SYSTEM §7.2`):
   - `enum class CafeCardType { Default, Product, Info }`.
   - Default: bg `surface-card`, `radius-lg`, padding `space-5`. Product: bg `surface-dark`/text `on-dark`,
     `radius-xl`. Info: bg `canvas`, 1px `hairline`, `radius-lg`. 슬롯(`content: @Composable () -> Unit`) 형태.
3. **`CafeChip`** (`DESIGN_SYSTEM §7.6`):
   - 미선택: bg `accent-soft`/text `primary`. 선택: bg `primary`/text `on-primary`. shape `radius-pill`,
     height 32, 가로 패딩 `space-3`. `selected: Boolean`, `onClick`.
4. **`CafeTopBar`** (`DESIGN_SYSTEM §7.4`):
   - height 56, bg `canvas`, 타이틀 `ink`/Bold(`h2` 스케일). 옵션 좌측 back/우측 action 슬롯(아이콘 24, `ink`).

## 규칙 / 하지 말 것
- 안티슬롭(`UI_GUIDE.md`): 글래스모피즘·그라데이션 텍스트·네온 글로우·아이콘 둥근 배경박스 금지.
- 카드 타입별 radius를 일괄 24로 만들지 마라(타입별 `radius-lg`/`radius-xl` 구분).
- 화면(M-01~)·ViewModel은 만들지 마라. 이 step은 컴포넌트까지만.
- 새 색/토큰 임의 추가 금지. hex 리터럴 금지.

## 검증
- 가능하면 각 컴포넌트의 androidTest(`createComposeRule`) 스모크(렌더 + 클릭 콜백) 작성. 계측 실행이 불가하면
  **컴파일**로 갈음한다.

## Acceptance Criteria
- `./gradlew :app:assembleDebug` 와 `./gradlew :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- `./gradlew :app:testDebugUnitTest` 여전히 green.
- hex 리터럴이 `ui/theme/Color.kt` 밖에 없다(`grep -rn "0xFF" app/src/main` → `Color.kt`만).
- 통과하면 `phases/2-components/index.json`의 step 0 status를 `completed` + `summary` 기록.
