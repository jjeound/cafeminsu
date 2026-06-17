# Step 1 — 화면 상태 컴포넌트 (Loading · Empty · Error · Offline · Snackbar)

데이터를 부르는 모든 화면이 공유할 **상태 표시 컴포넌트**를 만든다. `UI_GUIDE.md §화면 상태(필수 4종)`와
`DATA_MODEL.md §Shared UiState Contract(DataUiState)`가 기준이다. 색은 `CafeTheme` 토큰만 사용한다.

## 만들 것 — `ui/components/`
1. **`LoadingView`** — 콘텐츠 자리표시(스켈레톤 또는 절제된 인디케이터). 전체 블로킹 스피너 남용 금지.
2. **`EmptyView`** — 빈 상태 카피(텍스트 `muted`) + 다음 행동 버튼 슬롯(예: "메뉴 보러가기", `CafeButton`).
   `message: String`, `actionLabel: String?`, `onAction: (() -> Unit)?`.
3. **`ErrorView`** — 사용자 친화 **한국어** 메시지 + **재시도** 액션. 아이콘/보더 `error` 토큰.
   `message: String`, `retryable: Boolean`, `onRetry: () -> Unit`.
4. **`OfflineBanner`** — 오프라인 안내 배너(상단), 캐시 데이터는 읽기 전용이라는 맥락. 색 토큰 사용.
5. **`CafeSnackbarHost` / `cafeSnackbar`** (`DESIGN_SYSTEM §7.7`) — bg `surface-dark`, text `on-dark`,
   `radius-md`. 성공/에러/경고 아이콘 색 `success`/`error`/`warning`. Material3 `SnackbarHost`를 토큰으로
   래핑(기본 보라/기본색 노출 금지).
6. **`DataUiStateContent`** (편의 디스패처) — `DataUiState<T>`를 받아 `Loading→LoadingView`,
   `Empty→EmptyView`, `Error→ErrorView`, `Offline→OfflineBanner + content(cached)`, `Content→content(data)`
   로 분기하는 제네릭 컴포저블. 시그니처 예:
   `@Composable fun <T> DataUiStateContent(state: DataUiState<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit)`.

## 규칙 / 하지 말 것
- 텍스트는 한국어. 안티슬롭 요소 금지. 본문/리스트 중앙 정렬 금지(빈 상태 일러스트는 예외).
- 실제 화면/ViewModel/리포지토리 호출 금지 — 이 step은 상태 표시 컴포넌트까지만.
- 새 색/토큰 추가 금지. hex 리터럴 금지.

## 검증
- 가능하면 androidTest로 `DataUiStateContent`가 상태별로 올바른 뷰를 렌더하는지 스모크. 계측 불가 시 컴파일로 갈음.

## Acceptance Criteria
- `./gradlew :app:assembleDebug` 와 `./gradlew :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- `./gradlew :app:testDebugUnitTest` 여전히 green.
- hex 리터럴이 `ui/theme/Color.kt` 밖에 없다.
- 통과하면 `phases/2-components/index.json`의 step 1 status를 `completed` + `summary` 기록.
