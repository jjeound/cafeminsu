# Step 2 — 메뉴 상세 / 옵션 (M-03, MVVM + TDD)

`PRD.md` M-03(사이즈·온도·샷 등 옵션, 수량, 담기)을 구현한다. 플레이스홀더 `ui/feature/menu/MenuDetailScreen`을 채운다.
이 화면은 **장바구니에 담기**까지 책임진다(`CartRepository.addItem`).

## 네비게이션 (인자 라우트)
- `Routes.MENU_DETAIL`(`m03`)을 **`menuItemId` 인자**를 받는 라우트로 바꾼다(예: `"m03/{menuItemId}"`).
  `AppNavHost`에 `navArgument` 등록 + M-02/홈에서 `navigate("m03/$id")`로 이동하도록 step 1 배선과 연결.

## 패턴
- `MenuDetailViewModel`(`@HiltViewModel`): `SavedStateHandle`에서 `menuItemId`를 읽어 `getMenu(id)` 로드.
  `StateFlow<MenuDetailUiState>`. 옵션 선택/수량 변경/담기 액션을 함수로 수신.

## 만들 것 — `ui/feature/menu/`
- `MenuDetailUiState.kt` — 메뉴 상세, 옵션 그룹별 현재 선택, 수량, **현재 단가/합계**(= basePrice + 선택옵션
  extraPrice 합, × 수량), 담기 가능 여부(필수 그룹 충족). Loading/Content/Error 포함.
- `MenuDetailViewModel.kt`:
  - `getMenu` 결과를 Content/Error로. (없으면 `NotFound`→Error)
  - 옵션 선택 규칙: 그룹 `required`/`minSelect`/`maxSelect` 검증. `maxSelect=1`은 단일 선택(라디오),
    그 이상은 다중. 선택 변경 시 가격 재계산.
  - `onQuantityChange`(최소 1), `onAddToCart` → `CartRepository.addItem(menuItemId, selectedOptions, quantity)`.
    성공 시 완료 이벤트(스낵바/네비) 위임, `Failure`→에러 표시. **품절 아이템은 담기 비활성.**
- `MenuDetailScreen.kt` — 옵션 그룹은 `CafeChip`(선택)·라디오/체크 형태, 수량 스텝퍼, 하단 고정 `CafeButton`
  (primary, "담기 · {합계}원"). 가격 강조 `primary`. 상태는 컴포넌트로. 토큰만 사용.

## ⚠ TDD — ViewModel 테스트 먼저
`MenuDetailViewModelTest.kt`(실패 먼저 → 구현):
- 옵션 선택에 따라 합계가 정확히 재계산된다(basePrice + extras, × quantity).
- 필수 그룹 미충족이면 담기 불가, 충족이면 가능.
- `onAddToCart`가 올바른 인자로 `CartRepository.addItem`을 호출하고 성공/실패를 상태에 반영한다(MockK로 호출 검증 또는 Mock 리포 상태 확인).
- 없는 `menuItemId`면 Error.

## 하지 말 것
- 장바구니 화면(M-05)·결제·음성 구현 금지. 낙관적 UI로 결제하지 않음(여기선 담기까지만). hex/새 토큰 금지. 카피 한국어.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(MenuDetail 테스트 포함). `./gradlew :app:assembleDebug` + `:app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- M-02 → M-03(인자 전달) → 담기 흐름이 그래프상 연결된다.
- 통과하면 `phases/3-home-menu/index.json`의 step 2 status를 `completed` + `summary` 기록.
