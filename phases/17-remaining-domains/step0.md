# Step 0 — 리워드 실연동 (스탬프 + 기프티콘 조회/사용)

스탬프 적립 현황과 보유 기프티콘을 실서버에서 가져온다. `RewardRepository` 를 실서버 구현으로
교체한다(키 게이트 폴백). step 16 의 `NetworkModule`/Auth 인터셉터/`SessionStateHolder` 를 재사용한다.

> **API 스펙**: `docs/openapi.json` 의 스탬프/기프티콘 엔드포인트를 단일 진실로 한다. 매핑 규칙·차이는
> `docs/SERVER_INTEGRATION.md`(리워드 절)를 따른다. 도메인 모델은 `DATA_MODEL.md`(`StampCard`/`StampEvent`/
> `Gifticon`/`GifticonStatus`) 계약 그대로.

## ⚠ 사전 조건 (없으면 blocked)
- `docs/openapi.json` 에 스탬프(`api/stamps`)·기프티콘(`api/gifticons/*`) 엔드포인트가 없으면 → **blocked**
  (`blocked_reason` 명시) 후 중단.
- `BuildConfig.BASE_URL` 가 비어 있으면 Real 을 활성화하지 말고 **Mock 폴백**으로 두되(아래 DI),
  이 step 의 코드/테스트 자체는 작성·통과시킨다(테스트는 MockWebServer 사용).

## ⚠ TDD — 테스트를 먼저 작성하라
MockWebServer 로 다음을 **실패 테스트 먼저** 작성:
- 스탬프 카드 매핑(현재 적립 수/목표/이력), 기프티콘 목록·상세 매핑, 사용 처리 후 상태 전이,
- BaseResponse `isSuccess=false`/HTTP 에러 → `AppResult.Failure(DomainError)`,
- 비로그인(`AuthState.Guest`) 시 네트워크 호출 전에 `Unauthorized` 로 차단.

## 만들 것
### 1) `StampApi`/`GifticonApi` + DTO + mapper — `data/remote/`, `data/mapper/`
- 엔드포인트별 Retrofit 인터페이스 + DTO(스펙 그대로) + DTO→domain 매퍼.
- `GET api/stamps`(내 스탬프 목록) / `GET api/stamps/{storeId}`(특정 매장 상세) → `StampCard`.
  서버 스탬프는 **매장별**일 수 있다(도메인 `StampCard` 는 단일). 선택 매장(`SelectedStoreHolder`)이 있으면
  해당 매장 스탬프를, 없으면 목록의 대표(합산 또는 첫 매장)를 매핑한다 — `SERVER_INTEGRATION.md` 규칙 준수.
- `GET api/gifticons/my` → `List<Gifticon>`, `GET api/gifticons/{id}` → `Gifticon`,
  `POST api/gifticons/{id}/use` → 사용 처리 후 갱신 기프티콘(상태 `Used`).
- ID 는 Long↔String 왕복(`SERVER_INTEGRATION.md §1`). 만료/시각은 epoch millis 로 변환.

### 2) `RealRewardRepository` — `data/repository/`
- `observeStampCard()`/`grantStampsForPaidOrder(orderId)`/`observeGifticons()`/`getGifticon(id)`/
  `markGifticonUsed(id)` (`domain/repository/RewardRepository.kt` 계약 그대로).
- `grantStampsForPaidOrder(orderId)`: **서버는 결제 완료 시 스탬프를 자동 적립**한다(클라 적립 엔드포인트
  없음). 따라서 이 메서드는 **스탬프 카드 재조회**로 갱신 결과를 반환한다(별도 grant 호출 금지).
- 인증 필요 호출 전 `ensureAuthenticated()`(Guest 면 `Unauthorized`)만 확인하고, 토큰은 OkHttp Authorization
  인터셉터가 부착한다(`userId` 등 식별 파라미터 전송 금지 — `SERVER_INTEGRATION.md` 인증 절).

### 3) DI 교체 (키 게이트 폴백) — `di/RepositoryModule.kt`
- 기존 `RewardRepository` 의 `@Binds`(MockRewardRepository) 를 companion `@Provides` + `selectRewardRepository(
  baseUrl, realFactory, mockFactory)` 키 게이트로 교체(`BuildConfig.BASE_URL` 있으면 Real, 없으면 Mock).
  기존 `selectMenuRepository`/`selectOrderRepository` 패턴을 그대로 모사한다. **다른 Repository 바인딩은 무변경.**

## 하지 말 것
- 기프티콘 **바코드/QR 값을 로그·예외 메시지·평문 캐시에 노출 금지**(`SECURITY.md §3` 민감값).
- 화면/ViewModel·도메인 모델/Repository 시그니처 변경 금지(UI 무변경 목표). 새 결과 타입 금지(예외→`AppResult`).
- 스펙에 없는 필드·엔드포인트 임의 추가 금지. 점주/선물/알림/쿠폰 Repository 교체 금지(이 step 범위 밖).

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 가 성공한다(스탬프/기프티콘 매핑·사용·에러·비로그인 차단 MockWebServer
  테스트 + 기존 무파손).
- `BASE_URL` 부재 시 Mock 폴백으로 정상 구동됨을 테스트로 확인.
- `grep` 으로 바코드/QR/토큰이 로그에 노출되지 않음을 확인.
- 통과하면 step 0 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
