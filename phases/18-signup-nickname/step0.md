# Step 0 — 회원가입 도메인·데이터 (신규유저 신호 + 닉네임 중복확인/가입)

카카오 로그인이 돌려주는 `isNewUser` 신호를 도메인까지 전달하고, 닉네임 중복 확인·회원가입을
실서버에 붙인다. **UI 는 건드리지 않는다(다음 step).** step 16 의 `AuthApi`/`NetworkModule`/토큰 저장소를
재사용한다. `SECURITY.md`(토큰·PII) · `ARCHITECTURE.md`(인증/세션)를 따른다.

> **API 스펙**: `docs/openapi.json` 의 회원가입/닉네임 엔드포인트가 단일 진실이다.
> - `GET api/user/nickname/check?nickname=<>` → `NicknameCheckRes{available}`
> - `POST api/user/signup` `SignupReq{nickname, profileImageUrl?}` → `SignupRes{userId, nickname}`
> 둘 다 **Bearer 필요**(카카오 로그인 직후 토큰 저장돼 있음 — 인터셉터가 부착). 매핑 규칙은
> `docs/SERVER_INTEGRATION.md`(인증 절)를 따른다.

## ⚠ 사전 조건 (없으면 blocked)
- `docs/openapi.json` 에 `api/user/signup`·`api/user/nickname/check` 가 없으면 → **blocked**(`blocked_reason` 명시) 후 중단.
- `BuildConfig.BASE_URL` 부재 시 Real 비활성·**Mock 폴백** 유지(코드/테스트는 MockWebServer 로 작성·통과).

## ⚠ TDD — 테스트를 먼저 작성하라
MockWebServer(Real) + 단위(Mock)로 다음을 **실패 테스트 먼저** 작성:
- 카카오 로그인 응답 `isNewUser=true/false` 가 `AuthState.Authenticated.isNewUser` 로 전달되는지,
- `checkNickname` 가용/중복(`available` true/false) 매핑, `completeSignup` 성공 시 세션 상태가
  `isNewUser=false` + 표시명=닉네임 으로 갱신되는지, 입력 검증/서버 에러 → `AppResult.Failure`,
- 비로그인(토큰 없음) 시 가입 호출 전 차단(`Unauthorized`).

## 만들 것
### 1) 신규유저 신호 — `domain/model/Session.kt`, `data/remote/AuthApi.kt`
- `AuthState.Authenticated` 에 **`isNewUser: Boolean = false`** 필드를 추가한다(기본값으로 **하위호환** —
  기존 `Authenticated(...)` 생성부·`when` 분기 무파손). 새 AuthState variant 는 만들지 마라.
- `KakaoLoginRes.toLoginExchange()` 가 `isNewUser` 를 `Authenticated.isNewUser` 로 전달하도록 수정
  (현재는 버려지고 있음). refresh/profile 경로의 기본값은 `false`.

### 2) `AuthApi` 확장 + DTO + mapper — `data/remote/`
- `GET api/user/nickname/check`(@Query `nickname`) → `NicknameCheckRes{available}`,
  `POST api/user/signup`(@Body `SignupReq`) → `SignupRes{userId, nickname}`. DTO 는 스펙 그대로.
- `SignupRes` → `AuthState.Authenticated`(id=userId.toString(), displayName=nickname, isNewUser=false) 매퍼.

### 3) `SessionRepository` 계약 확장 — `domain/repository/SessionRepository.kt`
- `suspend fun checkNickname(nickname: String): AppResult<Boolean>`(사용 가능 여부),
  `suspend fun completeSignup(nickname: String): AppResult<AuthState>`(가입 완료 후 갱신된 세션).
- 닉네임 검증(2~10자·한글/영문/숫자)은 **UI step 의 책임**이지만, 빈/공백 닉네임은 여기서도 `Validation` 으로 방어.

### 4) `RealSessionRepository` + `MockSessionRepository` 구현 — `data/repository/`
- Real: `checkNickname` → check API, `completeSignup` → `signup` API → 성공 시 토큰은 그대로 두고
  **세션 상태를 갱신**(`Authenticated(isNewUser=false, displayName=닉네임)`)하고 그 상태를 반환.
  로그인 안 된 상태면 `Unauthorized`(가입은 카카오 로그인 후에만).
- Mock: 데모 동작 유지 — `checkNickname` 는 기본 가용(특정 예약어만 중복 처리해도 됨),
  `completeSignup` 은 닉네임 반영한 `Authenticated(isNewUser=false)` 반환. 기존 Mock 시나리오 무파손.

## 하지 말 것
- 화면/ViewModel/네비 수정 금지(다음 step). `AuthState` 에 새 variant 추가 금지(필드 추가만).
- 토큰·PII 로그 노출 금지. 스펙에 없는 필드·엔드포인트 임의 추가 금지. 새 결과 타입 금지(예외→`AppResult`).
- PATCH `api/user/nickname`(기존 유저 닉네임 변경)은 이번 범위 밖 — 구현하지 않는다.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 가 성공한다(isNewUser 전달·checkNickname·completeSignup·검증·비로그인
  차단 MockWebServer/단위 테스트 + 기존 무파손).
- `BASE_URL` 부재 시 Mock 폴백으로 정상 구동됨을 테스트로 확인.
- 통과하면 step 0 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
