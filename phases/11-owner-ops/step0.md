# Step 0 — 점주 메뉴 관리 (OWNER_MENU, MVVM + TDD)

> 첨부된 `점주 - 03 (메뉴 관리).png` + `docs/SCREENS.md`(OWNER_MENU)를 그대로 따른다. phase 10 step 0의 OWNER_MENU 플레이스홀더를 본구현으로 교체한다.
> 점주 메뉴 계약은 `docs/DATA_MODEL.md`(OwnerMenuRepository, `MenuItem.isVisible`)가 단일 진실.

## 만들 것 / 바꿀 것 — `ui/feature/owner/menu/`
`docs/SCREENS.md` OWNER_MENU 레이아웃을 정확히 구현:
- 상단 "메뉴 관리" `h1` + 우측 **"+ 메뉴 추가"** `primary` 텍스트버튼(MVP는 스텁 — 스낵바/토스트 "준비 중" 정도, 신규 화면 만들지 말 것).
- **카테고리 칩 row**(`CafeChip`): "전체"(선택=`primary`) · "커피" · "논커피" · "디저트". 선택 시 목록 필터.
- 메뉴 행(`surface-card`): 메뉴명 `h3`, 가격 `primary`, 상태 점+텍스트("● 판매중" `success` / "● 품절" `error`),
  우측 **토글 스위치**: ON=`primary`(판매중) / OFF=`hairline`(품절) → `setSoldOut`. **품절 행은 셀 디밍(`muted`)** + `error` "품절" 태그.

## 데이터 (도메인 계약)
- **`OwnerMenuRepository`**(`DATA_MODEL.md`) + `MockOwnerMenuRepository`(data, 인메모리 시드 + `@Binds`): `observeManagedMenus(categoryId)`, `setSoldOut(id, soldOut)`, `setVisible(id, visible)`.
- 필요 시 `MenuItem`에 `val isVisible: Boolean`(노출 on/off) 추가 — 기존 `isSoldOut`(품절)은 재사용. **기존 메뉴 사용처/테스트가 깨지지 않게** 기본값으로 안전하게 추가.
- `OwnerMenuViewModel`: 선택 카테고리(StateFlow) + 메뉴 Flow 결합 → `OwnerMenuUiState`(Loading/Content/Empty/Error). `Failure`→Error.
  `setSoldOut`는 확정 후 반영(낙관 금지), 중복 탭 가드.
- `OwnerMenuScreen` stateless + `OwnerMenuRoute`(hiltViewModel).

## ⚠ TDD — ViewModel 테스트 먼저
- 카테고리 필터, 품절 토글(`setSoldOut`) 후 상태/디밍 반영, Empty/Error 매핑(Turbine).

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 안티슬롭 금지.
- 매출·정산은 step 1 — 건드리지 마라. "메뉴 추가" 본기능(폼/저장)은 범위 밖(스텁만).

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 메뉴 관리가 `점주 - 03`.png 구조(카테고리 칩·메뉴 행·품절 토글)와 일치하고, 카테고리 필터·품절 토글이 동작한다.
- 통과하면 `phases/11-owner-ops/index.json`의 step 0 status를 `completed` + `summary` 기록.
