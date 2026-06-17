# Step 1 — 메뉴 목록 (M-02, MVVM + TDD)

`PRD.md` M-02(카테고리 탭 + 메뉴 카드 그리드)를 구현한다. 플레이스홀더 `ui/feature/menu/MenuScreen`을 채운다.

## 패턴
- `MenuViewModel`(`@HiltViewModel`, `MenuRepository` 주입): `StateFlow<MenuUiState>`.
- 카테고리 탭 선택 상태 보유 → 선택된 카테고리로 `observeMenus(categoryId)` 결합.
- `MenuScreen` stateless + `MenuRoute` 래퍼(`hiltViewModel()` + 상태 수집).

## 만들 것 — `ui/feature/menu/`
- `MenuUiState.kt` — 카테고리 목록 + 선택된 카테고리 id + 메뉴 아이템 목록. Loading/Content/Empty/Error 포함.
- `MenuViewModel.kt` — `observeCategories` + 선택 카테고리의 `observeMenus`를 결합. 카테고리 선택 액션
  `onCategorySelect(id)`. `Failure`→Error, 빈 목록→Empty. 예외 전파 금지.
- `MenuScreen.kt` — 상단 카테고리 탭은 가로 스크롤 `CafeChip`(선택 시 `primary`). 메뉴는 **2열 그리드**
  (`CafeCard` product, 메뉴명 `h3`, 가격 `primary`/`caption`). **품절(`isSoldOut`)**: 품절 배지 + 담기/클릭 비활성.
  메뉴 클릭 → `onMenuClick(menuItemId)` 콜백. 상태 4종은 컴포넌트로. 사이드 패딩 `space-5`.

## 네비게이션 배선
- `MenuScreen`의 `onMenuClick(id)`가 메뉴 상세(M-03) 라우트로 이동하도록 `AppNavHost`에서 연결한다.
  (실제 M-03 화면 구현은 step 2. 이 step에서는 라우트 인자 전달 콜백까지만 배선하고, 도착지는 기존 플레이스홀더여도 됨.)
- 홈(step 0)의 추천 메뉴 클릭도 동일 상세 라우트로 연결(가능하면).

## ⚠ TDD — ViewModel 테스트 먼저
`MenuViewModelTest.kt`(실패 먼저 → 구현):
- 초기 로드시 카테고리와 (기본/첫) 카테고리의 메뉴가 Content로 노출.
- `onCategorySelect` 호출 시 해당 카테고리 메뉴로 갱신(Turbine).
- `Failure`→Error, 빈 카테고리→Empty.
- 품절 아이템이 목록에 포함되되 플래그가 유지됨(담기 비활성은 UI 책임).

## 하지 말 것
- M-03 상세 로직/장바구니 담기 구현 금지(다음 step). 결제·음성 무관. hex/새 토큰 금지. 카피 한국어.
- 그리드 3열 이상 금지(모바일 2열까지). 중앙 정렬 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(Menu 테스트 포함). `./gradlew :app:assembleDebug` 성공. 직접 실행해 확인하라.
- 통과하면 `phases/3-home-menu/index.json`의 step 1 status를 `completed` + `summary` 기록.
