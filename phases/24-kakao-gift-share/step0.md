# Step 0: share-link-plumbing (선물 공유 링크 도메인 전달)

선물하기 카카오톡 연동의 1단계. 서버 `구매 → /share` 가 돌려주는 `shareLink`/`deepLink` 를 도메인
모델(`GiftSendResult`)까지 끌어올려, 다음 step(UI 카카오톡 공유 런처)이 이 링크를 사용할 수 있게 한다.
이 step 은 **data/domain 레이어만** 다룬다. UI/Activity/카카오 SDK 는 step1 에서 다룬다.

## 읽어야 할 파일

먼저 아래를 읽고 아키텍처와 설계 의도를 파악하라. 특히 `AppResult` 래핑, 낙관 금지, 로깅 최소화 규칙:

- `/docs/ARCHITECTURE.md` (레이어 의존 방향, `AppResult`/`DomainError`, 예외 비전파)
- `/docs/SERVER_INTEGRATION.md` (선물 절: `POST api/gifticons`(구매) → `POST api/gifticons/{id}/share`(선물 링크) 2단계)
- `/docs/SECURITY.md` (§4: 선물 수신자·토큰·링크 저장·로깅 최소화)
- `/docs/DATA_MODEL.md` (Gift 모델 계약)
- `app/src/main/java/com/cafeminsu/data/repository/RealGiftRepository.kt`
- `app/src/main/java/com/cafeminsu/data/remote/GifticonApi.kt` (`GifticonShareRes{shareLink, deepLink}` 확인)
- `app/src/main/java/com/cafeminsu/domain/model/Reward.kt` (`GiftSendResult`, `GiftChannel`)
- `app/src/main/java/com/cafeminsu/data/mapper/` 내 `toGiftSendResult` 매퍼
- `app/src/main/java/com/cafeminsu/data/repository/MockGiftRepository.kt`
- `app/src/test/java/com/cafeminsu/data/repository/RealGiftRepositoryTest.kt`

이전 코드를 꼼꼼히 읽고 설계 의도를 이해한 뒤 작업하라.

## 작업

서버 `/share` 응답(`GifticonShareRes{shareLink, deepLink}`)의 링크를 도메인 결과까지 전달한다.

1. **도메인 모델 확장** — `domain/model/Reward.kt`(또는 `GiftSendResult` 정의 파일):
   - `GiftSendResult` 에 nullable 링크 필드를 추가한다:
     ```kotlin
     data class GiftSendResult(
         val giftId: String,
         val sentAtMillis: Long,
         val shareLink: String? = null,
         val deepLink: String? = null,
     )
     ```
   - 기본값 `null` 로 두어 기존 호출부(SMS 채널, Mock)가 깨지지 않게 한다.

2. **매퍼 수정** — `toGiftSendResult`:
   - `GifticonShareRes` 의 `shareLink`/`deepLink` 를 `GiftSendResult` 에 채워 매핑한다.

3. **RealGiftRepository.sendGift** — 기존 2단계(구매 → /share) 흐름은 **그대로 유지**하고, `/share` 응답을
   매퍼로 변환할 때 링크가 채워지도록만 보장한다. 중간(구매) 실패 시 `/share` 진행 금지(낙관 금지) — 기존 유지.

4. **MockGiftRepository** — 시그니처 정합을 위해 `GiftSendResult` 반환부를 맞춘다. Mock 은 카카오 채널일 때
   더미 `shareLink`(예: 자사 더미 URL)나 `null` 중 택해도 무방하나, 컴파일/테스트가 통과해야 한다.

### 핵심 규칙 (반드시 준수)

- **로깅 금지**: `shareLink`/`deepLink`/수신자 식별자를 로그·분석 이벤트에 남기지 마라. 이유: PII/민감 링크 노출.
- **낙관 금지 유지**: 구매 단계 실패 시 `/share` 호출로 진행하지 마라. 이유: 미결제 선물 링크 생성 방지.
- **`AppResult` 래핑**: 모든 외부 호출은 `runCatchingToAppResult` 등으로 감싸고 예외를 던지지 마라.
- SMS 채널은 링크가 `null` 이어도 정상이다.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**: 구현 전에 `RealGiftRepositoryTest` 에 "카카오 채널 선물 성공 시 결과에 `shareLink`/
   `deepLink` 가 매핑된다"는 실패하는 테스트를 추가한 뒤 구현한다. (`scripts/hooks/tdd-guard.sh` 가 강제)
2. 위 AC 커맨드가 모두 통과하는지 확인한다. 기존 `RealGiftRepositoryTest`·`GiftViewModelTest` 등 **무회귀**.
3. 아키텍처 체크리스트:
   - 도메인 모델이 안드로이드 비종속인가? (링크는 순수 `String?`)
   - `AppResult`/`DomainError` 규약을 지켰는가? 예외를 던지지 않는가?
   - CLAUDE.md/SECURITY.md 로깅 최소화 규칙을 위반하지 않았는가?
4. 결과에 따라 `phases/24-kakao-gift-share/index.json` 의 step 0 을 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "GiftSendResult 에 shareLink/deepLink 추가 + toGiftSendResult/RealGiftRepository 매핑 (UI 공유 런처용 링크 전달)"`
   - 3회 시도 후 실패 → `"status": "error"`, `"error_message": "<구체적 에러>"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "<사유>"` 후 즉시 중단

## 금지사항

- UI/Compose/Activity/카카오 SDK 코드를 건드리지 마라. 이유: 이 step 은 data/domain 한정. 공유 런처는 step1.
- `GiftSendResult` 의 기존 필드(`giftId`, `sentAtMillis`)를 제거/개명하지 마라. 이유: 호출부 광범위 회귀.
- 링크/수신자를 로깅하지 마라.
- 기존 테스트를 깨뜨리지 마라.
