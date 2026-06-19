# Step 1 — 공통 상태 화면 디자인 정렬 (Loading / Network Error / Logout, TDD)

> 첨부된 `Loading.png`(ST_LOADING) + `Network Error.png`(ST_NETERR) + `logout confrim dialog.png`(ST_LOGOUT) + `docs/SCREENS.md`(ST_LOADING, ST_NETERR, ST_LOGOUT)를 그대로 따른다.
> **배경**: `ui/components`의 `LoadingView`/`ErrorView`와 로그아웃 다이얼로그를 디자인에 맞게 정렬한다. 공통 컴포넌트라 **모든 화면에 반영**된다.

## 바꿀 것 — `ui/components/`
1. **LoadingView**(`docs/SCREENS.md` ST_LOADING): 배경 `canvas`, 중앙 **코랄 아크 스피너**(`primary`, 절제된 회전) + "메뉴 정보를 불러오고 있어요" `h3` + "잠시만 기다려주세요" `muted`, 하단 **스켈레톤 카드 2개**(`surface-card`/`hairline`). 전체 블로킹 남용 금지. (메시지는 파라미터화해 화면별 문구 허용, 기본값 제공.)
2. **ErrorView**(ST_NETERR): `CafeTopBar`(‹ 오류) 옵션, 중앙 **연한 원(`accent-soft`/`error`)+Wi-Fi off 아이콘(`error`)**, "연결에 실패했어요" `h2` + 안내 `muted`, **에러코드 칩**(`surface-card`/`muted`), 폭 꽉 찬 "다시 시도"(primary) + "고객센터 문의"(ghost/`muted`).
3. **로그아웃 확인 다이얼로그**(ST_LOGOUT): 스크림 + 다이얼로그(`canvas`): "로그아웃 하시겠어요?" `h2` + 안내 `muted`, [취소(secondary)] [로그아웃(`primary`)] → `clearSession()` 와이프 → `LOGIN`. (MY step의 로그아웃과 공유.)

## 데이터 / 호환
- `LoadingView`/`ErrorView` 시그니처는 **기존 호출부와 호환 유지**(파라미터 추가 시 기본값). 모든 화면이 계속 컴파일·통과해야 한다.
- 에러코드/메시지는 `DomainError`→표시 매핑 재사용. 고객센터 동작은 스텁 허용.

## ⚠ TDD
- 컴포넌트 변경이 기존 androidTest/단위 테스트를 깨지 않게 유지. 변경 시그니처는 테스트도 갱신. (Compose UI 테스트: 메시지/버튼 표시 단언.)

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 안티슬롭(네온/글래스) 금지. (상태 화면 중앙 정렬 예외 허용.)
- 기존 화면의 상태 처리(`DataUiStateContent`) 계약을 깨지 마라.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- Loading/Error/Logout이 각 PNG 구조(아크+스켈레톤 / Wi-Fi off+에러코드+재시도 / 로그아웃 다이얼로그)와 일치하고, 기존 화면들이 정상 동작한다.
- 통과하면 `phases/15-redesign-voice-states/index.json`의 step 1 status를 `completed` + `summary` 기록.
