# Step 0: claim-data-layer (기프티콘 등록 데이터 레이어)

받는 사람 수동 등록의 1단계. 받은 **claimCode** 로 기프티콘을 내 계정에 귀속시키는 **data/domain** 계약을
만든다. 화면·딥링크는 step1. 백엔드 `POST api/gifticons/claim` 미구현 시에도 빌드·테스트는 통과해야 한다
(MockWebServer/Mock 폴백).

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md`(레이어, `AppResult`/`DomainError`, 예외 비전파), `/docs/SECURITY.md`(§3·4 민감값 미로깅)
- `/docs/KAKAO_GIFT_BACKEND.md`(§2 등록/claim 계약: `POST api/gifticons/claim {claimCode}` → 기프티콘)
- `app/src/main/java/com/cafeminsu/data/remote/GifticonApi.kt`(기존 기프티콘 API/DTO 패턴)
- `app/src/main/java/com/cafeminsu/data/repository/RealGiftRepository.kt`,
  `app/src/main/java/com/cafeminsu/data/repository/MockGiftRepository.kt`(선물 도메인 리포지토리 패턴)
- `app/src/main/java/com/cafeminsu/domain/repository/GiftRepository.kt`, `domain/model/Reward.kt`(`Gifticon`)
- `app/src/main/java/com/cafeminsu/data/mapper/GiftMapper.kt`, `data/remote/runCatchingToAppResult`
- `app/src/main/java/com/cafeminsu/di/RepositoryModule.kt`(Real/Mock 키게이트 패턴)
- `app/src/test/java/com/cafeminsu/data/repository/RealGiftRepositoryTest.kt`

## 작업

1. **API** — `GifticonApi` 에 등록 엔드포인트 추가:
   ```kotlin
   @POST("api/gifticons/claim")
   suspend fun claimGifticon(@Body request: GifticonClaimReq): GifticonClaimRes
   ```
   - `GifticonClaimReq(claimCode: String)`, `GifticonClaimRes(gifticonId, title, barcodeValue, qrValue, expiresAtMillis, status)`
     (`docs/KAKAO_GIFT_BACKEND.md` §2 응답에 맞춤). Moshi `@JsonClass`.

2. **도메인/리포지토리** — 등록 계약:
   - `GiftRepository` 에 `suspend fun claimGift(claimCode: String): AppResult<Gifticon>` 추가.
   - `RealGiftRepository`: 인증 확인 → `claimGifticon` 호출 → `Gifticon` 매핑(`AppResult` 래핑, 예외 비전파).
     입력 검증: `claimCode` 공백 → `DomainError.Validation("claimCode")`. 이미등록/만료/없음 → 서버 에러를
     `DomainError`(예: `NotFound`, `Payment("already-claimed")` 등 적절히)로 매핑.
   - `MockGiftRepository`: 동일 시그니처 구현(더미 `Gifticon` 반환 또는 코드 규칙 기반 성공/실패).

3. **매핑** — `GifticonClaimRes` → `Gifticon`(`GiftMapper.kt` 또는 인접 매퍼). 민감값(barcode/qr) 미로깅.

### 핵심 규칙 (반드시 준수)

- 모든 외부 호출 `AppResult` 래핑, 예외 비전파. 도메인은 안드로이드 비종속.
- `claimCode`·바코드/QR 등 민감값 **미로깅**(SECURITY §3·4).
- 백엔드 미구현 가능성 → 테스트는 MockWebServer/Mock 으로 통과시키고, 실 동작은 서버 준비 후로 둔다.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**: `RealGiftRepositoryTest`(MockWebServer)에 claim 성공/실패(공백·이미등록·없음) 케이스를
   먼저 작성한 뒤 구현.
2. AC 통과 확인. 기존 선물 테스트 무회귀.
3. 체크리스트: `AppResult`/`DomainError` 규약, 도메인 비종속, 민감값 미로깅.
4. `phases/27-gift-claim-register/index.json` step 0 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "GifticonApi.claimGifticon + GiftRepository.claimGift(claimCode)->Gifticon 매핑(Real/Mock, AppResult, 민감값 미로깅)"`
   - 3회 실패 → `"status": "error"` + `"error_message"` / 사용자 개입 필요 → `"status": "blocked"` + `"blocked_reason"`

## 금지사항

- UI/Compose/네비/딥링크/매니페스트를 건드리지 마라. 이유: step1 범위.
- `claimCode`/바코드/QR 을 로깅하지 마라. 기존 테스트를 깨뜨리지 마라.
