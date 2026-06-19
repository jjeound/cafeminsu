# Step 2 — 음성 AI 주문 화면 (M-04, MVVM + TDD)

`PRD.md` M-04(코랄 펄스 마이크, 실시간 transcript, 결과 확인)을 구현한다. 플레이스홀더 `ui/feature/voice/VoiceScreen`을 채운다.
step 0의 파서와 step 1의 `VoiceRecognizer`를 결합해 **발화 → transcript → 파싱 → 장바구니 반영 → 확인**까지 잇는다.
파이프라인은 `ARCHITECTURE.md §음성 AI 주문`, 비주얼은 `DESIGN_SYSTEM.md §9`, 접근성은 `UI_GUIDE.md`.

## 패턴 / 만들 것 — `ui/feature/voice/`
- `VoiceViewModel`(`@HiltViewModel`, `VoiceRecognizer` + `ParseVoiceOrderUseCase` + `MenuRepository` +
  `CartRepository` 주입): `StateFlow<VoiceUiState>`.
- `VoiceUiState.kt` — 상태: `PermissionRequired` / `Idle` / `Listening(partialText)` / `Parsed(items, unmatched)` /
  `AddedToCart` / `Error(message)`. transcript(interim/final)와 파싱 결과를 담는다.
- `VoiceViewModel.kt`:
  - 마이크 권한 없으면 `PermissionRequired`. 권한 허용 후 `VoiceRecognizer.start()` → `events` 수집:
    `Partial`→`Listening(partial)`, `Final(text)`→ 메뉴(`observeMenus`) 로드 후 `ParseVoiceOrderUseCase`로 파싱 →
    `Parsed`. `Error`→`Error` 상태(크래시 금지).
  - `onConfirm()`: `Parsed.items`를 `CartRepository.addItem(...)`로 반영 → 성공 시 `AddedToCart` + 장바구니(M-05)
    이동 이벤트 위임. (음성→확인→담기. 결제는 표준 플로우로.)
  - `unmatched`가 있으면 사용자에게 안내(무엇을 못 알아들었는지). 재시도(`onRetry`) 가능.
- `VoiceScreen.kt` (`DESIGN_SYSTEM §9`):
  - 배경 `canvas`, 중앙 원형 **코랄 펄스**(`primary`→`accent-soft` 그라데이션, 듣는 중 애니메이션 — 절제된 펄스),
    중앙 마이크 아이콘 `on-primary`.
  - **하단 transcript 항상 노출**(확정 `ink`, interim `muted`) — 접근성상 시각 병행 필수.
  - 파싱 결과 리스트 + "장바구니에 담기" `CafeButton`(primary). 권한 거부 시 **대체 경로**(메뉴로 이동 + 설정 유도),
    크래시 금지. 토큰/컴포넌트만. 네온 글로우/과한 애니메이션 금지(`UI_GUIDE.md`).
- 네비게이션: M-04는 홈/메뉴에서 진입하는 **풀스크린 모달 라우트**(이미 `Routes.VOICE` `m04` 존재). 진입 버튼을
  홈 또는 메뉴 화면에 연결(가능하면). 결과 확인 후 장바구니(M-05)로.

## ⚠ TDD — ViewModel 테스트 먼저
`VoiceViewModelTest.kt`(실패 먼저 → 구현; fake `VoiceRecognizer`로 이벤트 주입):
- `Partial` 이벤트 → `Listening`에 partial 텍스트 반영.
- `Final("아메리카노 두 잔")` → 파서 결과가 `Parsed`로 노출(메뉴 매칭).
- `onConfirm()` → `CartRepository.addItem`이 파싱 항목으로 호출되고 `AddedToCart` + 네비 이벤트.
- 권한 없음 → `PermissionRequired`. `Error` 이벤트 → `Error` 상태(크래시 없음).

## 하지 말 것
- 클라우드 STT/LLM 금지(온디바이스+규칙기반 유지). 결제/적립 화면 변경 금지. hex/새 토큰 금지. 카피 한국어.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(Voice VM 테스트 포함). `./gradlew :app:assembleDebug` + `:app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 발화→transcript→파싱→장바구니 반영 흐름이 코드/그래프상 연결되고, 마이크 권한 거부 시 대체 경로가 있다.
- 통과하면 `phases/8-voice/index.json`의 step 2 status를 `completed` + `summary` 기록.
