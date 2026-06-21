# Step 2 — 알림 실연동 (알림 목록 + 전체 읽음)

알림 목록과 전체 읽음 처리를 실서버에 붙인다. `NotificationRepository` 를 실서버 구현으로 교체한다
(키 게이트 폴백). step 16 의 `NetworkModule`/Auth 인터셉터/`SessionStateHolder` 를 재사용한다.

> **API 스펙**: `docs/openapi.json` 의 알림 엔드포인트를 단일 진실로 한다. 매핑 규칙은
> `docs/SERVER_INTEGRATION.md`(알림 절)를 따른다. 도메인 모델은 `DATA_MODEL.md`(`AppNotification`/
> `NotificationType`) 계약 그대로.

## ⚠ 사전 조건 (없으면 blocked)
- `docs/openapi.json` 에 알림 목록(`GET api/notifications`)·전체 읽음(`PATCH api/notifications/read-all`)
  엔드포인트가 없으면 → **blocked**(`blocked_reason` 명시) 후 중단.
- `BuildConfig.BASE_URL` 부재 시 Real 비활성·**Mock 폴백** 유지(코드/테스트는 MockWebServer 로 작성·통과).

## ⚠ TDD — 테스트를 먼저 작성하라
MockWebServer 로 다음을 **실패 테스트 먼저** 작성:
- 알림 목록 매핑(타입 enum 매핑, 읽음 여부, 생성 시각 정렬), 빈 목록,
- `markAllRead` 가 `PATCH api/notifications/read-all` 를 호출하고 `AppResult.Success(Unit)`,
- BaseResponse 에러 → `Failure(DomainError)`, 비로그인(`AuthState.Guest`) → 네트워크 전 `Unauthorized` 차단.

## 만들 것
### 1) `NotificationApi` + DTO + mapper — `data/remote/`, `data/mapper/`
- `GET api/notifications` → `List<AppNotification>`, `PATCH api/notifications/read-all` → 전체 읽음.
- DTO→domain 매퍼. **타입 매핑**: 서버 알림 타입 문자열 → 도메인 `NotificationType`
  (`OrderAccepted`/`OrderReady`/`OrderCompleted`/`StampEarned`/`GifticonReceived`). 매핑 불가한 타입은
  `SERVER_INTEGRATION.md` 규칙대로 가장 가까운 값으로 흡수하거나 목록에서 제외(임의 신규 타입 추가 금지).
- ID Long↔String, 시각 epoch millis(`SERVER_INTEGRATION.md §1`).

### 2) `RealNotificationRepository` — `data/repository/`
- `observeNotifications(): Flow<AppResult<List<AppNotification>>>` / `markAllRead(): AppResult<Unit>`
  (`domain/repository/NotificationRepository.kt` 계약 그대로).
- 인증 필요 호출 전 `ensureAuthenticated()`(Guest → `Unauthorized`). 토큰은 인터셉터가 부착(식별 파라미터 미전송).

### 3) DI 교체 (키 게이트 폴백) — `di/RepositoryModule.kt`
- `NotificationRepository` 의 `@Binds`(MockNotificationRepository) → companion `@Provides` +
  `selectNotificationRepository(...)` 키 게이트로 교체(step 0 과 동일 패턴). **다른 Repository 바인딩 무변경.**

## 하지 말 것
- 화면/ViewModel·도메인 시그니처 변경 금지(UI 무변경 목표). 새 결과 타입 금지(예외→`AppResult`).
- 스펙에 없는 필드·엔드포인트·알림 타입 임의 추가 금지. 리워드/선물/쿠폰/점주 Repository 교체 금지(범위 밖).
- `unread-count`/개별 읽음(`{id}/read`)은 도메인 계약에 없으므로 이번 step 에서 **추가 구현하지 않는다**.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 가 성공한다(알림 매핑·전체읽음·에러·비로그인 차단 MockWebServer 테스트
  + 기존 무파손).
- `BASE_URL` 부재 시 Mock 폴백으로 정상 구동됨을 테스트로 확인.
- 통과하면 step 2 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
