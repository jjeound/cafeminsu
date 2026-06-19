# Step 0 — 스탬프 / 적립 (M-08, MVVM + TDD, 보호 화면)

`PRD.md` M-08(스탬프 진행도, 적립 내역)을 구현한다. 플레이스홀더 `ui/feature/stamp/StampScreen`을 채운다.
`RewardRepository.observeStampCard()`와 세션(`SessionRepository`)을 사용한다. 하단 탭의 "스탬프" 화면이다.

## 인증/보호 화면 처리 (ARCHITECTURE §인증)
- M-08은 보호 화면 → `AuthState`가 `Guest`/`Expired`면 **"로그인이 필요해요" + 재로그인 CTA** UiState로 전환한다(contract 준수).
- 단, **MVP 데모에는 로그인 화면이 아직 없으므로**(PRD 오픈 이슈) `MockSessionRepository`의 기본 상태를
  `AuthState.Authenticated(demo UserProfile)`로 둔다(현재 `Guest` → 데모 유저로 조정). 이렇게 해서 기본 진입 시
  콘텐츠가 보이고, Guest/Expired 분기는 구현은 하되 기본 트리거되지 않는다. (세션 토큰 값은 모델/로그에 노출 금지.)
- `MockSessionRepository` 조정으로 깨지는 기존 세션 단위 테스트가 있으면 함께 갱신한다.

## 패턴 / 만들 것 — `ui/feature/stamp/`
- `StampViewModel`(`@HiltViewModel`, `RewardRepository`+`SessionRepository` 주입): `StateFlow<StampUiState>`.
- `StampUiState.kt` — 진행도(`currentCount`/`goalCount`), 적립 내역(`List<StampEvent>`), 목표 도달 여부.
  Loading/Content/**Empty**(아직 적립 없음)/Error + NeedsLogin(보호) 포함.
- `StampViewModel.kt` — `observeAuthState`와 `observeStampCard`를 결합: 비인증→NeedsLogin, 인증→스탬프 카드 매핑.
  `Failure`→Error. 예외 전파 금지.
- `StampScreen.kt` — 상단 진행도 시각화(채워진 스탬프 `primary`, 빈 칸 `hairline`; 목표 도달 `success`),
  "{currentCount}/{goalCount}" 텍스트. 적립 내역 리스트(`CafeCard`, 날짜/주문/개수, 금액·메타 `caption`/`muted`).
  빈 내역은 `EmptyView`("아직 적립 내역이 없어요" + "메뉴 보러가기"). 토큰/컴포넌트만. 좌측 정렬.

## ⚠ TDD — ViewModel 테스트 먼저
`StampViewModelTest.kt`(실패 먼저 → 구현):
- 인증 상태에서 스탬프 카드가 Content(진행도/내역)로 노출된다(Turbine).
- 적립 내역이 비면 Empty.
- `Failure`면 Error.
- `AuthState.Guest`/`Expired`면 NeedsLogin 상태가 된다.
- (적립 발생 시 `currentCount` 증가가 화면에 반영됨 — Mock 리포 상태 변경 후 emit 검증.)

## 하지 말 것
- 기프티콘(M-09)·마이(M-10)·음성 구현 금지. 실제 로그인 화면/제공자 구현 금지(상태 분기만). hex/새 토큰 금지. 카피 한국어.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(Stamp 테스트 + 조정된 세션 테스트 포함). `./gradlew :app:assembleDebug` 성공. 직접 실행해 확인하라.
- 통과하면 `phases/6-stamp-gifticon/index.json`의 step 0 status를 `completed` + `summary` 기록.
