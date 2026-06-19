# Step 1 — 로그인(회원가입) + 카카오 로그인 (실연동 + Mock 폴백, TDD)

> 첨부된 `회원가입.png` + `docs/SCREENS.md`(LOGIN)를 그대로 따른다.

카카오 로그인 화면과 로그인 추상화를 구현한다. **실연동(Kakao SDK)** 하되, 키가 없으면 빌드/자율 루프가 막히지
않도록 **Mock 폴백**을 둔다(`docs/PRD.md` 결정). 키는 하드코딩 금지 — `local.properties`→BuildConfig 주입(`SECURITY.md §7`).

## 만들 것
1. **`LoginProvider` 추상화**(`domain`, `DATA_MODEL.md` 참조): `suspend fun login(): AppResult<AuthState>`,
   `suspend fun logout(): AppResult<Unit>`.
   - `RealKakaoLoginProvider`(data/platform): Kakao 로그인 SDK 사용. 성공 시 `AuthState.Authenticated(UserProfile)`.
   - `MockLoginProvider`: 버튼 탭 → 즉시 `Authenticated(데모 UserProfile)`(키 없을 때 사용).
   - **DI 선택**: `BuildConfig.KAKAO_NATIVE_APP_KEY`가 비어있지 않으면 Real, 아니면 Mock 바인딩.
2. **Gradle/키 주입**: `app/build.gradle.kts`에 Kakao SDK 의존성(버전 카탈로그에 추가) + `local.properties`의
   `KAKAO_NATIVE_APP_KEY`를 `BuildConfig` 필드와 manifestPlaceholder(`kakao{key}` redirect scheme)로 주입.
   키가 없으면 빈 문자열 placeholder로 **빌드는 성공**해야 한다. AndroidManifest에 Kakao AuthCodeHandlerActivity 등록(키 scheme).
3. **`SessionRepository` 로그인 연결**: 기존 `SessionRepository`(Mock)가 `LoginProvider`를 사용해 `login()`/`logout()`로
   `AuthState`를 갱신하도록 확장(또는 별도 AuthRepository). 로그아웃 시 토큰/민감데이터 와이프 의미 유지.
4. **로그인 화면** — `ui/feature/login/`: `LoginViewModel`(StateFlow<LoginUiState>) + `LoginScreen`(`docs/SCREENS.md` LOGIN):
   배경 `canvas`, 코랄 애스터리스크 + "카페민수" `display`, 하단 **카카오 로그인 버튼**(카카오 옐로우 — 이 브랜드 색만
   `Color.kt`에 상수로 예외 허용, 말풍선+"카카오 로그인"). 탭 → `login()` → 성공 시 `HOME`, 실패 스낵바.
   step 0의 인증 게이트가 이 화면을 LOGIN 라우트로 사용하게 연결.

## ⚠ TDD — 먼저 작성
- `MockLoginProvider`/`SessionRepository`: `login()`→`Authenticated`, `logout()`→`Guest`(와이프). (Turbine/MockK)
- `LoginViewModel`: 로그인 성공 시 성공 이벤트/상태, 실패 시 에러 상태.

## 규칙 / 하지 말 것
- 키 하드코딩·리포 커밋 금지(local.properties만). 토큰 값 로그/화면 노출 금지. 카카오 옐로우 외 hex 금지.
- 실제 카카오 런타임 동작은 키 필요 — 코드/빌드는 폴백으로 완성하고, 런타임 활성화 키 안내는 step 완료 요약에 남겨라.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` 성공(키 없이도 빌드 green — Mock 폴백). 직접 실행해 확인하라.
- 미인증 진입 시 LOGIN 화면이 보이고, (Mock) 로그인 → HOME 으로 전환된다.
- 통과하면 `phases/9-redesign-shell/index.json`의 step 1 status를 `completed` + `summary`(필요한 키 명시) 기록.
