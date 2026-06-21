# Step 1 — 회원가입 화면 + 네비 배선 (SIGNUP 닉네임 설정)

신규 카카오 유저가 닉네임을 설정하는 SIGNUP 화면을 만들고 `LOGIN → (신규유저) → SIGNUP → HOME`
흐름을 배선한다. step 0 의 `SessionRepository.checkNickname/completeSignup` 와 `AuthState.isNewUser` 를 사용한다.

> **디자인 단일 진실**: `docs/SCREENS.md` 의 **SIGNUP** 절(`docs/screens/회원가입 - 닉네임 설정.png`,
> 에러: `회원가입 - 닉네임 설정 (에러).png`). 색·치수·타이포는 **토큰만**(`docs/DESIGN_SYSTEM.md`), hex 리터럴 금지.
> 안티-AI슬롭(`docs/UI_GUIDE.md`) 준수. 카피는 한국어.

## ⚠ TDD — 테스트를 먼저 작성하라
- ViewModel 단위 테스트(JUnit/MockK/Turbine): 닉네임 검증(2~10자·한글/영문/숫자), 중복(`available=false`)→에러,
  규칙 위반→에러, 유효+미중복일 때만 `시작하기` 활성화, `completeSignup` 성공→완료 이벤트, 실패→에러 메시지.
- Compose UI 테스트(`createComposeRule`): 입력/클리어(✕)/글자수(n/10)/버튼 비활성·활성/에러 헬퍼 노출.

## 만들 것
### 1) 라우트 — `ui/navigation/Routes.kt`
- `const val SIGNUP = "signup"` 추가.

### 2) `SignupScreen` + `SignupViewModel`(+UiState) — `ui/feature/signup/`
SCREENS.md SIGNUP 스펙 그대로:
- 좌상단 `‹` 뒤로(→ LOGIN). 헤더 "닉네임을 설정해주세요" `h1` + "카페민수에서 사용할 이름이에요" `body`/`muted`.
- 라벨 "닉네임" + `CafeTextField`(placeholder "닉네임을 입력해주세요", 우측 클리어 ✕, 포커스 1.5px `primary`).
- 헬퍼 행: 좌 "한글·영문·숫자 2~10자" `caption`/`muted`, 우 글자수 "n/10".
- 하단 폭 꽉 찬 `시작하기` `CafeButton`(primary) — **유효(2~10자 규칙)·미중복 전까지 비활성**.
- 에러 상태: 입력 1.5px `error` 보더 + 헬퍼를 에러로 교체 — 중복 "이미 사용 중인 닉네임이에요",
  규칙 위반 "한글·영문·숫자 2~10자로 입력해주세요" `error`.
- ViewModel: 입력 검증(클라) → `시작하기` 시 `completeSignup(nickname)`(서버가 중복도 최종 검증).
  중복 응답/검증 실패는 위 에러 메시지로 매핑. (필요 시 입력 변경 시 `checkNickname` 으로 사전 확인 가능 —
  단, 매 키 입력 호출 남발 금지. 최종 판정은 `completeSignup`.) 모든 외부 호출은 `AppResult`/`UiState.Error` 처리.

### 3) 네비 배선 — `ui/navigation/AppNavHost.kt`, `ui/feature/login/*`
- `LoginViewModel.handleLoginSuccess`: `authState.isNewUser` 면 신규 이벤트(예: `LoginEvent.NavigateSignup`),
  아니면 기존 `NavigateHome`. `LoginRoute` 에 `onNewUser: () -> Unit` 콜백 추가(기존 `onLoginSuccess` 유지).
- LOGIN composable: `onNewUser` → `navigate(SIGNUP)`(popUpTo LOGIN inclusive). SIGNUP composable 추가:
  `onSignupComplete` → `navigate(HOME)`(popUpTo SIGNUP/LOGIN inclusive), `onBack` → LOGIN 복귀.
- 스플래시/리프레시 경로는 그대로 둔다(이번 범위는 **로그인 직후 신규유저 라우팅**; 재실행 시 닉네임 미설정
  엣지 처리는 후속). 기존 로그인 성공 플로우(기존 유저→HOME) 무파손.

## 하지 말 것
- step 0 의 Repository/DTO/세션 로직 변경 금지(이 step 은 화면·배선만). 점주 로그인 플로우 변경 금지.
- hex 리터럴·임의 색/토큰 신설 금지. 보라/인디고·글래스모피즘·네온 글로우 금지(`UI_GUIDE.md`).
- 닉네임을 로그에 남기지 마라(PII). 새 결과 타입 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 가 성공한다
  (SignupViewModel 단위 + SIGNUP Compose UI 테스트 + 기존 무파손).
- 신규유저(`isNewUser=true`) 로그인 → SIGNUP, 기존 유저 → HOME 으로 분기됨을 테스트로 확인.
- 통과하면 step 1 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
