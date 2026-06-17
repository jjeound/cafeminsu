# Step 0 — 홈 화면 (M-01, MVVM + TDD)

`PRD.md` M-01(홈/그리팅: 인사, 추천 메뉴, 진행 중 주문, 스탬프 요약)을 MVVM+UDF로 구현한다.
플레이스홀더였던 `ui/feature/home`을 실제 화면으로 채운다. data phase에서 주입만 해 둔 `HomeViewModel`을 확장한다.

## 패턴 (ARCHITECTURE.md 준수)
- `HomeViewModel`(`@HiltViewModel`): `StateFlow<HomeUiState>` 노출, 사용자 액션은 함수로 수신.
- `HomeScreen`은 stateless — 상태를 받고 이벤트를 위로 올린다(state hoisting). `hiltViewModel()`로 VM 획득하는
  `HomeRoute`(또는 동등) 래퍼에서 상태 수집 후 `HomeScreen(state, onXxx)` 호출.
- `HomeUiState`: Loading/Content/Empty/Error 의미를 포함(`DataUiState` 재사용 또는 화면 전용 sealed).

## 만들 것 — `ui/feature/home/`
- `HomeUiState.kt` — 그리팅 텍스트(세션 `AuthState` 기반: Guest면 일반 인사), 추천/최근 메뉴 리스트
  (`MenuRepository`), 스탬프 요약(`RewardRepository`의 `StampCard` currentCount/goalCount), 진행 중 주문 요약(옵션, 없으면 생략).
- `HomeViewModel.kt` — 주입된 `MenuRepository`/`RewardRepository`/`SessionRepository`의 Flow를 결합해
  `HomeUiState`로 매핑. `AppResult.Failure`는 `Error`로, 빈 데이터는 `Empty` 의미로 변환(예외 전파 금지).
  추천 메뉴 클릭 시 상세로 가는 네비게이션 이벤트(`onMenuClick(menuItemId)`)는 콜백으로 위임.
- `HomeScreen.kt` — `canvas` 배경, 그리팅 `display`/`ink`, 섹션 헤더 `h2`. 추천 메뉴는 `CafeCard`(product),
  스탬프 요약은 `CafeCard`. 로딩/빈/에러는 `DataUiStateContent`/`LoadingView`/`EmptyView`/`ErrorView` 사용.
  좌측 정렬, 사이드 패딩 `space-5`, 섹션 간격 `space-8`. 하단 탭 스캐폴드 안에서 동작.

## ⚠ TDD — ViewModel 테스트 먼저
`app/src/test/.../home/HomeViewModelTest.kt`를 **먼저** 작성(실패) 후 구현:
- 리포지토리가 데이터를 주면 `HomeUiState`가 추천 메뉴/스탬프 요약을 담은 Content가 된다(Turbine으로 StateFlow 검증).
- 리포지토리가 `Failure`를 주면 상태가 Error가 된다.
- 빈 메뉴면 Empty 의미로 표현된다.
- (Mock 리포지토리 또는 MockK 페이크 사용. `kotlinx-coroutines-test`가 없으면 카탈로그/testImplementation에 추가.)

## 하지 말 것
- 다른 화면(M-02/03 등)·결제·음성 구현 금지. 네비게이션 그래프의 인자/라우트 변경은 다음 step(M-02/03)에서.
- raw `Box`에 색 직접 칠하기 금지(컴포넌트/토큰 사용). hex 리터럴·새 토큰 금지. 카피는 한국어.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(HomeViewModel 테스트 포함 green). 직접 실행해 확인하라.
- `./gradlew :app:assembleDebug` 성공.
- 통과하면 `phases/3-home-menu/index.json`의 step 0 status를 `completed` + `summary` 기록.
