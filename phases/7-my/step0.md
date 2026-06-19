# Step 0 — 마이페이지 (M-10, MVVM + TDD, 보호 화면)

`PRD.md` M-10(프로필, 주문 내역, 설정)을 구현한다. 플레이스홀더 `ui/feature/my/MyScreen`을 채운다.
하단 탭의 "마이" 화면. `SessionRepository`(프로필/로그아웃)와 `OrderRepository.observeOrderHistory()`를 사용한다.

## 인증/보호 화면 (ARCHITECTURE §인증, SECURITY §1)
- 보호 화면 → `Guest`/`Expired`면 NeedsLogin 상태(stamp/gifticon과 동일 패턴 재사용). 기본은 인증된 데모 유저(이전 phase에서 조정됨).
- **로그아웃**: `SessionRepository.clearSession()` 호출 → 세션/로컬 민감 데이터 와이프 의미(Mock에서는 상태를
  `Guest`로 리셋). 로그아웃 후 화면은 NeedsLogin 또는 홈으로. **프로필의 민감값(전화번호)은 끝 4자리만**(`phoneLast4`),
  토큰은 화면/로그에 노출하지 않는다.

## 패턴 / 만들 것 — `ui/feature/my/`
- `MyViewModel`(`@HiltViewModel`, `SessionRepository`+`OrderRepository` 주입): `StateFlow<MyUiState>`.
- `MyUiState.kt` — 프로필(`displayName`, `phoneLast4`), 최근 주문 내역(`List<Order>` 요약), 설정 항목(로그아웃 등),
  앱 버전/메타. Loading/Content/Empty(주문 없음)/Error + NeedsLogin 포함.
- `MyViewModel.kt` — `observeAuthState` + `observeOrderHistory` 결합 → 매핑. `onLogout()` → `clearSession()`.
  `Failure`→Error. 예외 전파 금지.
- `MyScreen.kt`:
  - 프로필 헤더(`displayName` `h2`/`ink`, 전화 끝 4자리 `caption`/`muted` — 예: "010-****-1234" 형태로 마스킹).
  - 주문 내역 리스트(`CafeCard`, 주문번호/날짜/금액/상태). 비면 `EmptyView`("주문 내역이 없어요" + "메뉴 보러가기").
    내역 항목 클릭 → 주문 상태(M-07) 라우트로(가능하면).
  - 설정 섹션: 로그아웃(`CafeButton` secondary/ghost), 앱 버전 `meta`/`muted-soft`. 토큰/컴포넌트만. 좌측 정렬.

## ⚠ TDD — ViewModel 테스트 먼저
`MyViewModelTest.kt`(실패 먼저 → 구현):
- 인증 상태에서 프로필 + 주문 내역이 Content로 노출.
- 주문 내역이 비면 Empty.
- `onLogout()`이 `clearSession()`을 호출하고 상태가 비인증(NeedsLogin)으로 전환된다.
- `Failure`면 Error. `Guest`/`Expired`면 NeedsLogin.

## 하지 말 것
- 음성(M-04) 구현 금지. 실제 로그인 화면/제공자 구현 금지(상태 분기만). 전화번호 전체·토큰 노출 금지. hex/새 토큰 금지. 카피 한국어.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(My 테스트 포함). `./gradlew :app:assembleDebug` + `:app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 통과하면 `phases/7-my/index.json`의 step 0 status를 `completed` + `summary` 기록.
