# Step 1: nfc-claim-data-layer (NFC 쿠폰 발급 데이터/도메인 레이어)

손님용 NFC 쿠폰 발급의 1단계. 매장 NFC 태그에서 읽은 **코드**로 백엔드에 쿠폰 발급을 요청하는
**data/domain** 계약과, NDEF 페이로드(문자열/URL)에서 코드를 뽑는 **순수 파서**를 만든다. NFC 읽기(Android)·
화면·네비는 step2~3. 백엔드 `POST /api/nfc/claim` 미구현 시에도 빌드·테스트는 통과해야 한다(Mock/단위테스트).

발급된 쿠폰은 별도 타입이 아니라 **기존 기프티콘(전 매장 공용 금액형)** 이다 — 이 step 은 발급 API 응답을
다룰 도메인 모델만 만들고, 기프티콘 사용/결제 플로우는 기존 것을 그대로 재사용한다.

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md`(레이어, `AppResult`/`DomainError`, 예외 비전파, Real/Mock 키게이트)
- `/docs/SECURITY.md`(§3·4 민감값/토큰 미로깅, §6 입력 검증)
- `app/src/main/java/com/cafeminsu/core/AppResult.kt`(`AppResult`/`DomainError`)
- `app/src/main/java/com/cafeminsu/data/remote/GifticonApi.kt`(기존 API/DTO·Moshi `@JsonClass` 패턴)
- `app/src/main/java/com/cafeminsu/data/remote/RemoteResult.kt`(`runCatchingToAppResult`, `Throwable.toDomainError`)
- `app/src/main/java/com/cafeminsu/data/remote/NetworkModule.kt`(API `@Provides` 추가 위치, 인증 Retrofit, Moshi 제공)
- `app/src/main/java/com/cafeminsu/data/repository/RealGiftRepository.kt`(인증 확인 + HttpException → DomainError
  매핑 패턴: `toClaimDomainError()` 의 409 분기를 참고)
- `app/src/main/java/com/cafeminsu/data/repository/MockGiftRepository.kt`(Mock 리포지토리 패턴)
- `app/src/main/java/com/cafeminsu/di/RepositoryModule.kt`(BASE_URL 기반 Real/Mock 선택 `select...Repository` 패턴)
- `app/src/main/java/com/cafeminsu/ui/feature/gift/claim/GiftClaimDeepLink.kt`(코드 형식/길이 검증·쿼리파싱 참고)
- `app/src/test/java/com/cafeminsu/data/repository/RealGiftRepositoryTest.kt`(MockWebServer 테스트 패턴)

## API 계약 (이 step 의 단일 진실)

### 발급
- `POST api/nfc/claim` (인증 필요: `Authorization: Bearer <AccessToken>` — 인증 Retrofit/OkHttp 사용)
- Request body: `{ "tagCode": "NFC-AB7K-9QM2" }`
- 성공 200: `{ "gifticonId": 123, "amount": 1000, "expiresAt": "2026-12-25T10:30:00", "message": "방문 감사 쿠폰" }`

### 에러 (공통 포맷) — HTTP status 로 큰 분류, body `code` 문자열로 세부 분기
`{ "code": "NFC_CLAIM_COOLDOWN", "message": "..." }`

| HTTP | code | DomainError 매핑 |
|---|---|---|
| 409 | `NFC_CLAIM_COOLDOWN` | `DomainError.Payment("nfc-cooldown")` (오늘 이미 발급, 하루 1회) |
| 404 | `NFC_TAG_NOT_FOUND` | `DomainError.NotFound` (등록되지 않은/잘못된 태그) |
| 400 | `NFC_TAG_INACTIVE` | `DomainError.Payment("nfc-inactive")` (비활성 태그) |
| 400 | `VALIDATION_FAILED` | `DomainError.Validation("tagCode")` (tagCode 누락/빈값 — 앱 버그) |
| 401 | `UNAUTHORIZED`/`INVALID_TOKEN`/`EXPIRED_TOKEN` | `DomainError.Unauthorized` |

> 401 은 OkHttp `SessionAuthenticator` 가 1회 토큰 갱신 후 재시도한다. 그래도 401 이면 `Unauthorized`.
> body `code` 가 없거나 미지의 값이면 HTTP status 로 폴백(`Throwable.toDomainError()`). 400 두 케이스는
> status 만으로 구분 불가하므로 **반드시 body `code` 를 파싱**해 분기한다.

## 작업

1. **순수 코드 파서** — `domain` 비종속 순수 함수(예: `com.cafeminsu.domain.nfc.NfcTagCode` object 또는
   `ui/feature/nfc` 인접). NDEF 레코드에서 추출한 raw 문자열 → 발급용 `code` 로 변환:
   - `fun parse(raw: String?): String?`
     - null/공백 → null.
     - **URL** (`http://`/`https://` 로 시작 또는 `://` 포함): URI 파싱 후 쿼리파라미터 `code` 값만 추출
       (예: `https://x/nfc?code=NFC-AB7K-9QM2` → `NFC-AB7K-9QM2`). 디코딩·경계검증.
     - **순수 코드** (`NFC-AB7K-9QM2`): trim 후 그대로 사용.
     - 추출값은 형식/길이 검증(허용 문자 `[A-Za-z0-9-]`, 길이 1..64; 위반 시 null). `GiftClaimDeepLink` 의
       검증 스타일 참고.
   - **순수 함수로 풀 단위테스트**(아래 AC). 안드로이드 API 의존 금지(NDEF/Context 비주입).

2. **API + DTO** — `NfcApi`(신규, `data/remote`):
   ```kotlin
   interface NfcApi {
       @POST("api/nfc/claim")
       suspend fun claim(@Body request: NfcClaimReq): NfcClaimRes
   }
   ```
   - `NfcClaimReq(tagCode: String)`, `NfcClaimRes(gifticonId: Long?, amount: Int?, expiresAt: String?, message: String?)`,
     에러 body `NfcErrorBody(code: String?, message: String?)` — 모두 Moshi `@JsonClass(generateAdapter = true)`.
   - `NetworkModule` 에 `provideNfcApi(retrofit: Retrofit): NfcApi`(**인증** Retrofit) 추가.

3. **도메인 모델 + 리포지토리** — 발급 계약:
   - `domain/model` 에 `NfcCoupon(gifticonId: Long, amount: Int, expiresAtIso: String, message: String?)`
     (발급된 쿠폰 요약; 화면 표시·성공 안내용).
   - `domain/repository/NfcCouponRepository`: `suspend fun claim(tagCode: String): AppResult<NfcCoupon>`.
   - `data/repository/RealNfcCouponRepository`(`@Singleton`, `@Inject`): `NfcApi`·`SessionStateHolder`·
     `Moshi`(에러 body 파싱용)·`@IoDispatcher` 주입.
     - 입력 검증: `tagCode` 공백 → `DomainError.Validation("tagCode")`.
     - 인증 확인(미인증 → `DomainError.Unauthorized`) — `RealGiftRepository.ensureAuthenticated()` 패턴.
     - `nfcApi.claim(NfcClaimReq(tagCode))` 호출, `try/catch`(CancellationException 재throw). 성공 → `NfcCoupon`
       매핑. 실패(HttpException) → `response()?.errorBody()?.string()` 를 Moshi 로 `NfcErrorBody` 파싱해
       위 표대로 `code`→`DomainError` 매핑. body 파싱 실패/미지 code → `toDomainError()` 폴백.
   - `data/repository/MockNfcCouponRepository`: 동일 시그니처. 더미 성공(`NfcCoupon`) 반환, 단 알아보기 쉬운
     규칙으로 실패도 시뮬레이션 가능(예: tagCode 가 특정 접두/패턴이면 cooldown/notfound). 공백 → Validation.
   - `data/mapper` 에 `NfcClaimRes.toNfcCoupon(): AppResult<NfcCoupon>`(필수 필드 null 이면 `DomainError.Unknown`
     또는 `Validation`) 같은 매퍼. 민감값 없음이나 응답을 통째로 로깅하지 않는다.

4. **DI** — `RepositoryModule` 에 `NfcCouponRepository` 를 BASE_URL 기반 Real/Mock 선택으로 제공
   (기존 `RewardRepository`/`GiftRepository` 의 `select...Repository(baseUrl, realFactory, mockFactory)` 패턴 그대로).

### 핵심 규칙 (반드시 준수)

- 모든 외부 호출 `AppResult` 래핑, **예외 비전파**(`CancellationException` 만 재throw). 도메인은 안드로이드 비종속.
- 발급은 금전성 액션 — **낙관적 UI 금지**(이 step 은 데이터 계약만). 토큰/`tagCode`/응답 전체를 **로깅하지 않는다**.
- 400 두 케이스는 body `code` 로 구분(status 만으로 불가). `code` 미존재/미지 → status 폴백.
- 백엔드 미구현 가능성 → 테스트는 MockWebServer/Mock 으로 통과시키고 실동작은 서버 준비 후로 둔다.
- **TDD 가드**: `src/main` 의 모든 신규 `.kt` 는 대응 `...Test.kt`(같은 패키지 경로, `src/test`)를 **먼저** 만든다.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**:
   - `NfcTagCodeTest`(순수): 순수코드/URL/대소문자/공백/잘못된 scheme·형식·과대길이 → 추출·null 케이스.
   - `RealNfcCouponRepositoryTest`(MockWebServer): 성공(200 매핑), 409 cooldown, 404 notfound, 400 inactive,
     400 validation, 401 → 각 `DomainError` 매핑, 공백 tagCode → Validation, 미인증 → Unauthorized. 먼저 실패
     테스트 작성 후 구현.
   - `MockNfcCouponRepositoryTest`: 성공/검증 케이스.
2. AC 통과·무회귀(step0 결과 포함).
3. 체크리스트: `AppResult`/`DomainError` 규약, 도메인 비종속, 미로깅, body code 분기, Real/Mock 키게이트.
4. `phases/29-nfc-coupon-claim/index.json` step 1 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "NfcTagCode 순수 파서(코드/URL?code 추출·검증) + NfcApi(POST api/nfc/claim)+DTO + NfcCouponRepository(Real/Mock) claim(tagCode)->NfcCoupon, body code→DomainError 매핑(cooldown/notfound/inactive/validation/unauthorized), AppResult·미로깅·BASE_URL 키게이트."`
   - 3회 실패 → `"status": "error"` + `"error_message"` / 사용자 개입 필요 → `"status": "blocked"` + `"blocked_reason"`

## 금지사항

- UI/Compose/네비/매니페스트/`NfcAdapter` 등 안드로이드 NFC 읽기를 건드리지 마라(step2~3 범위).
- 토큰/`tagCode`/발급응답을 로깅하지 마라. 기존 테스트(스케줄링 포함)를 깨뜨리지 마라.
- 새 `DomainError` 변형을 추가하지 마라 — 기존 변형(Payment(reason)/NotFound/Validation/Unauthorized)으로 매핑.
