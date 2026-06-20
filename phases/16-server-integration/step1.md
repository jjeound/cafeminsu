# Step 1 — 인증/세션 실연동 (카카오 토큰 서버 교환 + EncryptedDataStore)

기존 카카오 로그인으로 얻은 토큰을 **서버에 교환**해 앱 세션 토큰을 발급받고, 보안 저장소에
보관한다. `SessionRepository` 를 실서버 구현으로 교체한다(키 게이트 폴백). `SECURITY.md §1`(토큰
저장·와이프) · `ARCHITECTURE.md`(인증/세션·401 처리)를 따른다.

> **API 스펙**: `docs/openapi.json` 의 인증 관련 엔드포인트(카카오 토큰 교환 / 세션 갱신 / 내 프로필)를
> 단일 진실로 한다. 요청·응답 DTO 필드는 스펙 그대로. base 설정은 step 0 의 `NetworkModule` 재사용.

## ⚠ 사전 조건 (없으면 blocked)
- `docs/openapi.json` 에 카카오 토큰 교환 엔드포인트가 없으면 → **blocked**(`blocked_reason`에 명시) 후 중단.
- `BuildConfig.BASE_URL` 가 비어 있으면 Real 을 활성화하지 말고 **Mock 폴백**으로 두되(아래 DI),
  이 step 의 코드/테스트 자체는 작성·통과시킨다(테스트는 MockWebServer 사용).

## ⚠ TDD — 테스트를 먼저 작성하라
MockWebServer 로 교환 성공/실패, 401→갱신 1회→실패 시 만료, 로그아웃 와이프를 **실패 테스트 먼저**.

## 만들 것
### 1) 토큰 저장소 — `data/auth/` (EncryptedDataStore)
- 액세스/리프레시 토큰을 `androidx.security`로 래핑한 **EncryptedDataStore** 또는 Keystore 저장소에 보관.
  **평문(SharedPreferences/Room/파일) 금지**(`SECURITY.md §1`). 읽기/쓰기/와이프 API 제공.
- 토큰 값을 도메인 모델·로그·예외 메시지에 노출하지 마라.

### 2) `AuthApi` + DTO + mapper — `data/remote/`
- 카카오 토큰 교환(요청: 카카오 access token → 응답: 앱 세션 토큰 + 사용자 프로필), 세션 갱신, me 조회.
- DTO→`UserProfile`/`AuthState` 매퍼. 매핑은 `domain/model` 계약(`AuthState`, `UserProfile`) 준수.

### 3) 카카오 토큰 노출 — `data/platform/RealKakaoLoginProvider.kt`(+ `domain/auth/LoginProvider`)
- 현재 `LoginProvider.login()` 은 `AuthState` 를 직접 반환한다. 서버 교환을 위해 **카카오 OAuth 토큰**을
  얻는 좁은 경로를 추가하라(예: `LoginProvider` 에 토큰 반환 메서드 추가 또는 결과에 토큰 동봉).
  기존 Mock/Owner 인증 경로와 시그니처 호환을 깨지 마라.

### 4) `RealSessionRepository` — `data/repository/`
- `login()`: 카카오 로그인 → 카카오 토큰 → `AuthApi` 교환 → 토큰 저장 → `AuthState.Authenticated(UserProfile)`.
- `observeAuthState()`/`refreshOnce()`/`clearSession()`(`domain/repository/SessionRepository.kt` 계약 그대로).
- `clearSession()`/`logout()` 은 토큰·로컬 민감 데이터를 **즉시 와이프**(`SECURITY.md §1`).

### 5) OkHttp Auth 인터셉터 + 401 처리 — `data/remote/`
- 저장된 액세스 토큰을 `Authorization` 헤더로 부착. **401 → 갱신 1회 시도 → 실패 시 `AuthState.Expired`**
  로 전이(`ARCHITECTURE.md`). 토큰을 URL 쿼리/로그에 넣지 마라.

### 6) DI 교체 (키 게이트 폴백) — `di/RepositoryModule.kt` / `di/AuthModule.kt`
- `SessionRepository` 바인딩을 `BuildConfig.BASE_URL` 가 있으면 `RealSessionRepository`, 없으면
  `MockSessionRepository` 로 선택. 기존 `selectLoginProvider(...)` 의 키 게이트 패턴을 재사용/모사한다.
- 이 phase 범위 밖 Repository 바인딩(점주·쿠폰·선물·알림·리워드 등)은 **건드리지 마라**.

## 하지 말 것
- 토큰 평문 저장·로깅 노출 금지. 결제/주문 등 다른 도메인 Repository 교체 금지(다음 step).
- 화면/ViewModel 수정 금지(Repository 계약 동일하므로 UI 무변경 목표). 새 결과 타입 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 가 성공한다(교환/갱신/만료/와이프 MockWebServer 테스트 + 기존 무파손).
- `grep` 으로 토큰이 로그/모델에 노출되지 않음을 확인(릴리스 로깅 없음).
- `BASE_URL` 부재 시 앱이 Mock 세션으로 정상 구동(폴백)됨을 테스트로 확인.
- 통과하면 step 1 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
