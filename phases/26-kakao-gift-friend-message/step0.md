# Step 0: friend-picker-deps-and-scope (카카오 친구/메시지 SDK + 스코프)

카카오톡 친구 선물의 1단계. 친구 피커(`v2-friend`)와 친구 메시지 전송(`v2-talk`) SDK 의존성을 추가하고,
`friends`/`talk_message` 추가 스코프를 **증분 동의**로 요청하는 헬퍼를 만든다. 이 step 은 **의존성 + 스코프
헬퍼(플랫폼 레이어)** 만 다룬다. 친구 피커 UI·메시지 전송·구매 변경은 step1.

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md` (레이어 의존: Activity/SDK 의존은 UI·플랫폼 레이어, 도메인/데이터 비종속)
- `/docs/SECURITY.md` (§4: 친구 식별자/링크 로깅 최소화; 권한 최소화 + rationale)
- `/docs/KAKAO_GIFT_BACKEND.md` (전체 친구 선물 흐름·게이트 맥락)
- `app/src/main/java/com/cafeminsu/data/platform/RealKakaoLoginProvider.kt`
  (`UserApiClient` 로그인/콜백 패턴, `suspendCancellableCoroutine` 사용법)
- `app/src/main/java/com/cafeminsu/CafeApplication.kt` (`KakaoSdk.init`, `BuildConfig.KAKAO_NATIVE_APP_KEY`)
- `gradle/libs.versions.toml`, `app/build.gradle.kts` (버전 카탈로그, `kakao = "2.23.2"`)

## 작업

1. **의존성 추가** (기존 `kakao = "2.23.2"` 버전 재사용, 새 버전 변수 만들지 마라):
   - `gradle/libs.versions.toml` `[libraries]`:
     ```toml
     kakao-friend = { group = "com.kakao.sdk", name = "v2-friend", version.ref = "kakao" }
     kakao-talk = { group = "com.kakao.sdk", name = "v2-talk", version.ref = "kakao" }
     ```
   - `app/build.gradle.kts` dependencies: `implementation(libs.kakao.friend)`, `implementation(libs.kakao.talk)`.

2. **스코프 증분 동의 헬퍼** — `ui/feature/gift/` 또는 `data/platform/` (Activity/Context 의존이므로 UI·플랫폼):
   - 인터페이스 + 실구현으로, 친구 선택/메시지 전송 직전에 `friends`·`talk_message` 동의를 보장한다:
     ```kotlin
     interface KakaoTalkScopeProvider {
         suspend fun ensureFriendMessageScopes(activity: Activity): AppResult<Unit>
     }
     ```
   - 실구현: `UserApiClient.instance.loginWithNewScopes(activity, listOf("friends", "talk_message")) { token, error -> ... }`
     를 `suspendCancellableCoroutine` 으로 감싸 `AppResult` 반환. 취소/거부 → `DomainError.Unauthorized` 등 매핑.
   - 예외 비전파(`AppResult` 래핑). 토큰/스코프 값 로깅 금지.

### 핵심 규칙 (반드시 준수)

- 권한은 **필요 시점에 최소**로 요청한다(증분 동의). 앱 시작/로그인 시 강제 요청하지 마라. 이유: 권한 최소화(SECURITY).
- Activity/SDK 의존은 UI·플랫폼 레이어에만. 도메인/데이터(`GiftRepository`)에 Context 를 들이지 마라.
- 모든 콜백 경로를 `AppResult` 로 감싸고 예외를 던지지 마라.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**: `KakaoTalkScopeProvider` 계약(성공/거부/취소 매핑)을 Fake 또는 계약 테스트로 먼저
   작성한 뒤 구현한다. 실 `loginWithNewScopes` 는 Activity 의존이라 단위 테스트는 계약 위주.
2. 위 AC 통과 확인. 기존 테스트 무회귀(로그인/선물 등).
3. 아키텍처 체크리스트:
   - SDK/Context 의존이 UI·플랫폼 레이어에만 있는가?
   - 권한을 필요 시점에만 최소 요청하는가? 토큰/스코프 미로깅인가?
4. `phases/26-kakao-gift-friend-message/index.json` step 0 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "v2-friend·v2-talk 의존성 + KakaoTalkScopeProvider(friends/talk_message 증분 동의 헬퍼, loginWithNewScopes)"`
   - 3회 실패 → `"status": "error"` + `"error_message"`
   - 사용자 개입 필요 → `"status": "blocked"` + `"blocked_reason"` 후 중단

## 금지사항

- 친구 피커 UI·메시지 전송·구매(수신자 미지정) 로직을 건드리지 마라. 이유: step1 범위.
- 앱 시작/로그인 시 friends/talk_message 를 강제 요청하지 마라. 이유: 권한 최소화.
- 토큰/스코프/친구 식별자를 로깅하지 마라. 기존 테스트를 깨뜨리지 마라.
