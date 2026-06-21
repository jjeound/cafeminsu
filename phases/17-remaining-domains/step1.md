# Step 1 — 선물 실연동 (기프티콘 구매 → 선물 링크 발급)

기프티콘 선물 보내기를 실서버에 붙인다. `GiftRepository` 를 실서버 구현으로 교체한다(키 게이트 폴백).
step 16 의 `NetworkModule`/Auth 인터셉터/`SessionStateHolder` 와 step 0 의 `GifticonApi` 를 재사용한다.

> **API 스펙**: `docs/openapi.json` 의 기프티콘 구매/공유 엔드포인트를 단일 진실로 한다. 매핑 규칙은
> `docs/SERVER_INTEGRATION.md`(선물 절)를 따른다. 도메인 모델은 `DATA_MODEL.md`(`GiftSendRequest`/
> `GiftChannel`/`GiftSendResult`) 계약 그대로.

## ⚠ 사전 조건 (없으면 blocked)
- `docs/openapi.json` 에 기프티콘 구매(`POST api/gifticons`)·공유(`POST api/gifticons/{id}/share`)
  엔드포인트가 없으면 → **blocked**(`blocked_reason` 명시) 후 중단.
- `BuildConfig.BASE_URL` 부재 시 Real 비활성·**Mock 폴백** 유지(코드/테스트는 MockWebServer 로 작성·통과).

## ⚠ TDD — 테스트를 먼저 작성하라
MockWebServer 로 다음을 **실패 테스트 먼저** 작성:
- `sendGift` 성공: 구매 응답의 gifticonId 로 공유 호출 → `GiftSendResult` 매핑(giftId/발송 시각),
- 구매 실패 또는 공유 실패 시 `AppResult.Failure(DomainError)` 로 중단(두 번째 호출 안 함),
- 금액 ≤ 0 등 입력 검증(`Validation`), 비로그인(`AuthState.Guest`) → 네트워크 전 `Unauthorized` 차단.

## 만들 것
### 1) `GiftApi`(또는 기존 `GifticonApi` 확장) + DTO + mapper — `data/remote/`, `data/mapper/`
- `POST api/gifticons`(기프티콘 구매: `amount`) → 발급 gifticonId/정보.
- `POST api/gifticons/{gifticonId}/share`(선물 링크 발급) → 공유 결과. 요청 바디는 스펙 그대로
  (채널/수신자/메시지 필드가 있으면 매핑, 없으면 가능한 값만 전달하고 도메인 계약은 유지).
- DTO→`GiftSendResult` 매퍼. ID Long↔String, 시각 epoch millis(`SERVER_INTEGRATION.md §1`).

### 2) `RealGiftRepository` — `data/repository/`
- `sendGift(request: GiftSendRequest): AppResult<GiftSendResult>` (`domain/repository/GiftRepository.kt` 계약).
  흐름: **구매(`POST api/gifticons`) → 발급 id 로 공유(`POST .../share`) → `GiftSendResult` 매핑**.
  중간 실패는 다음 단계로 진행하지 말고 즉시 `Failure` 반환(낙관 금지).
- `GiftSendRequest.channel`(KakaoTalk/Sms)·`recipientRef`·`message` 는 공유 요청 스키마에 맞게 매핑
  (서버가 채널 구분을 받지 않으면 링크만 발급하고 도메인 결과로 채널 정보는 보존). 금액 검증은 호출 전 수행.
- 인증 필요 호출 전 `ensureAuthenticated()`(Guest → `Unauthorized`). 토큰은 인터셉터가 부착(식별 파라미터 미전송).

### 3) DI 교체 (키 게이트 폴백) — `di/RepositoryModule.kt`
- `GiftRepository` 의 `@Binds`(MockGiftRepository) → companion `@Provides` + `selectGiftRepository(...)`
  키 게이트로 교체(step 0 과 동일 패턴). **다른 Repository 바인딩 무변경.**

## 하지 말 것
- 선물 링크/바코드/QR 등 민감값 로그·예외 노출 금지(`SECURITY.md §3`). 새 결과 타입 금지(예외→`AppResult`).
- 화면/ViewModel·도메인 시그니처 변경 금지. 리워드/알림/쿠폰/점주 Repository 교체 금지(범위 밖).
- 스펙에 없는 필드·엔드포인트 임의 추가 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 가 성공한다(구매→공유 성공·중간 실패·검증·비로그인 차단 MockWebServer
  테스트 + 기존 무파손).
- `BASE_URL` 부재 시 Mock 폴백으로 정상 구동됨을 테스트로 확인.
- 통과하면 step 1 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
