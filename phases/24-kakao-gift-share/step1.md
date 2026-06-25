# Step 1: kakao-share-launch (카카오톡 공유 런처)

선물하기 카카오톡 연동의 2단계. 선물 채널이 **카카오톡**일 때, 서버 구매/공유로 받은 링크
(step0 에서 `GiftSendResult.shareLink`/`deepLink` 로 전달됨)를 **카카오톡 공유 SDK(`v2-share`)로 띄워**
사용자가 친구에게 기프티콘을 전달하게 한다. 카카오톡 미설치/키 공백 시 웹 공유 URL 폴백, 그마저 불가하면
기존 토스트만(무해 폴백). Activity 의존(공유 런처)은 **UI 레이어**에만 둔다.

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md` (레이어 의존: UI 가 Activity/SDK 의존을 담당, 도메인/데이터 비종속)
- `/docs/SECURITY.md` (§4: 링크/수신자 로깅 최소화)
- `/docs/UI_GUIDE.md`, `/docs/DESIGN_SYSTEM.md` (안티-AI슬롭, 토큰만 사용, 한국어 카피)
- `phases/24-kakao-gift-share/step0.md` 및 step0 산출물(`GiftSendResult` 확장, 매퍼)
- `app/src/main/java/com/cafeminsu/ui/feature/gift/GiftViewModel.kt` (`GiftEvent`, `sendGift`)
- `app/src/main/java/com/cafeminsu/ui/feature/gift/GiftScreen.kt`
- `app/src/main/java/com/cafeminsu/ui/feature/gift/GiftUiState.kt` (이벤트 정의 위치)
- `app/src/main/java/com/cafeminsu/CafeApplication.kt` (`KakaoSdk.init`, `BuildConfig.KAKAO_NATIVE_APP_KEY`)
- `app/build.gradle.kts`, `gradle/libs.versions.toml` (의존성 버전 카탈로그, `kakao=2.23.2`)
- `app/src/test/java/com/cafeminsu/ui/feature/gift/GiftViewModelTest.kt`

step0 에서 만들어진 링크 전달 코드를 읽고 의도를 이해한 뒤 작업하라.

## 작업

1. **의존성 추가**:
   - `gradle/libs.versions.toml` `[libraries]` 에:
     ```toml
     kakao-share = { group = "com.kakao.sdk", name = "v2-share", version.ref = "kakao" }
     ```
     (기존 `kakao = "2.23.2"` 버전 재사용. 새 버전 변수 만들지 마라.)
   - `app/build.gradle.kts` 의 dependencies 에 `implementation(libs.kakao.share)` 추가.

2. **ViewModel 이벤트** — `GiftViewModel`/`GiftUiState`:
   - `GiftEvent` 에 `LaunchKakaoShare(target)` 를 추가한다. `target` 은 step0 의 `GiftSendResult` 링크
     (`shareLink`/`deepLink`)를 담는 간단한 값(예: `data class KakaoShareTarget(val shareLink: String?, val deepLink: String?)`).
   - `sendGift` 성공 시, 채널이 `GiftChannel.KakaoTalk` 이고 링크가 있으면 기존 `SendSucceeded` 와 함께(또는 대신)
     `LaunchKakaoShare(target)` 를 emit 한다. SMS 채널은 기존 동작 유지.

3. **공유 런처(UI)** — `GiftScreen` 또는 신규 `ui/feature/gift/KakaoShareLauncher.kt`:
   - `LocalContext` 로 Activity/Context 를 얻어 카카오 `ShareClient` 로 공유를 실행한다.
   - `ShareClient.instance.isKakaoTalkSharingAvailable(context)` 로 카카오톡 사용 가능 여부 확인 →
     가능하면 피드/텍스트 템플릿(기프티콘 안내 + `shareLink`)으로 카카오톡 공유, 불가하면 `shareLink` 를
     웹 공유(Custom Tab/브라우저 Intent)로 폴백.
   - `BuildConfig.KAKAO_NATIVE_APP_KEY` 가 공백이거나 링크가 모두 `null` 이면 런처를 호출하지 말고 기존
     "선물을 보냈어요" 토스트만 노출(무해 폴백).
   - 카피는 한국어, 색/치수는 디자인 토큰만(hex 리터럴 금지).

### 핵심 규칙 (반드시 준수)

- **카카오 공유 실패 ≠ 선물 실패**: 서버 구매/공유는 이미 성공했으므로, 카카오톡 런처 실패/취소가 선물 전송을
  실패로 만들면 안 된다. 런처 오류는 조용히 흡수하거나 안내 토스트로만 처리한다. 이유: 이중 차감/혼란 방지.
- **로깅 금지**: `shareLink`/`deepLink`/수신자를 로그에 남기지 마라.
- **레이어 분리**: 카카오 `ShareClient`/Context 의존은 UI 에만. `GiftViewModel`/도메인/데이터에 Android Context
  를 들이지 마라. 이유: 도메인 안드로이드 비종속 규칙.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**: 구현 전에 `GiftViewModelTest` 에 "카카오톡 채널 + 링크 있는 전송 성공 시
   `LaunchKakaoShare` 이벤트가 emit 된다"는 실패 테스트를 Turbine 으로 추가한 뒤 구현한다. 공유 런처 자체는
   Activity 의존이라 단위 테스트는 **이벤트 계약** 위주로 한다.
2. 위 AC 통과 확인. 기존 `GiftViewModelTest`·`RealGiftRepositoryTest` **무회귀**.
3. 아키텍처 체크리스트:
   - UI 레이어에만 Context/SDK 의존이 있는가? ViewModel/도메인은 비종속인가?
   - hex 리터럴 0(디자인 토큰만), 카피 한국어인가?
   - SECURITY 로깅 최소화를 위반하지 않았는가?
4. 결과에 따라 `phases/24-kakao-gift-share/index.json` 의 step 1 을 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "v2-share 의존성 + GiftEvent.LaunchKakaoShare + 카카오톡 공유 런처(미설치 시 웹 폴백, 키 공백 무해 폴백). 카카오 공유 실패는 선물 실패 아님"`
   - 3회 실패 → `"status": "error"`, `"error_message": "<에러>"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "<사유>"` 후 중단

## 금지사항

- `GiftViewModel`/도메인/데이터에 Android `Context` 를 주입하지 마라. 이유: 레이어 비종속 규칙 위반.
- 카카오톡 런처 실패를 선물 전송 실패로 처리하지 마라. 이유: 서버 구매/공유는 이미 성공.
- 링크/수신자를 로깅하지 마라.
- hex 색 리터럴을 쓰지 마라(토큰만). 기존 테스트를 깨뜨리지 마라.
