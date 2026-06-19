# Step 2 — 메뉴 상세 화면 재설계 (MENU_DETAIL, TDD)

> 첨부된 `주문 - 04 (메뉴 상세).png` + `docs/SCREENS.md`(MENU_DETAIL)를 그대로 따른다.
> **배경**: 기존 `ui/feature/menu/MenuDetailScreen.kt`(phase 3)는 옛 디자인이다. 디자인에 맞게 재작업한다. 담기/옵션/합계 도메인 로직은 재사용.

## 바꿀 것 — `ui/feature/menu/`
`docs/SCREENS.md` MENU_DETAIL 레이아웃으로 재작업:
- `CafeTopBar`: 좌 `‹`, 중앙 "메뉴 상세", 우측 **찜 하트** 토글(`ink`/선택 `primary`).
- 상단 큰 이미지 영역(`surface-card` 배경 + 원형 메뉴 이미지 플레이스홀더).
- 메뉴명 `h1`, 설명 `body`/`muted`, 가격 `primary`(`h2`급). `hairline` 구분.
- **옵션 그룹**(라벨 `body` + "*필수" `caption`):
  - 온도 *필수: [HOT] [ICE] 2분할 토글(선택=`surface-dark`/`on-dark`, 비선택=`surface-card`/`ink`).
  - 사이즈 *필수: [Regular] [Large (+500)].
  - 샷 추가: [없음] [+1샷 (+500)] [+2샷 (+1,000)] (3분할).
- **수량 스텝퍼**: "수량" + [− 1 +] (우측 정렬, `surface-card`).
- 하단 고정 **"장바구니 담기 · {합계}원"** `CafeButton`(primary, 폭 꽉 참). 합계=base+옵션, 실시간 갱신. 품절 시 비활성.

## 데이터 (기존 재사용)
- 기존 `MenuDetailViewModel`/UiState/옵션·합계 계산·장바구니 담기 로직 재사용. 옵션 모델(온도/사이즈/샷)이 부족하면 디자인에 맞게 도메인 보강(TDD).
- 금액/옵션 입력 검증 유지(`SECURITY.md`). 담기는 확정 후 반영.

## ⚠ TDD — ViewModel 테스트 먼저(추가/변경분)
- 옵션 선택 조합 → 합계 실시간 계산, 필수 옵션 미선택 시 담기 불가, 수량 스텝퍼 경계, 품절 비활성(Turbine). 기존 테스트는 새 구조로 갱신.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 매직넘버 금지(옵션가/치수 상수화·토큰화).
- 장바구니 화면 자체 재작업은 phase 13 — 담기 동작만 유지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 메뉴 상세가 `주문 - 04`.png 구조(이미지·옵션 세그먼트·수량 스텝퍼·하단 담기 버튼+합계)와 일치하고, 옵션→합계 갱신이 동작한다.
- 통과하면 `phases/12-redesign-store-menu/index.json`의 step 2 status를 `completed` + `summary` 기록.
