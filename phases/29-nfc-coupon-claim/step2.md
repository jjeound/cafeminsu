# Step 2: nfc-claim-viewmodel (NFC 발급 화면 ViewModel/상태)

손님용 NFC 쿠폰 발급의 2단계. step1 의 `NfcCouponRepository.claim` 과 `NfcTagCode.parse` 를 사용하는
**ViewModel + UiState** 를 만든다. NFC 하드웨어 읽기/Compose 화면/네비는 step3. 이 step 은 **순수 단위테스트**
(Turbine + Fake 리포지토리)로 검증 가능한 상태기계에 집중한다.

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md`(MVVM/UDF, `StateFlow<UiState>`, 일회성 이벤트)
- `app/src/main/java/com/cafeminsu/ui/feature/gift/claim/GiftClaimViewModel.kt`,
  `ui/feature/gift/claim/GiftClaimUiState.kt`(제출중 가드·일회성 이벤트·`DomainError`→메시지 매핑 패턴 — 그대로 따른다)
- `app/src/main/java/com/cafeminsu/ui/feature/gifticon/GifticonViewModel.kt`(`DomainError`→한국어 메시지/재시도 매핑 참고)
- `phases/29-nfc-coupon-claim/step1.md` 산출물(`NfcCouponRepository.claim`, `NfcCoupon`, `NfcTagCode.parse`)
- `app/src/test/java/com/cafeminsu/ui/feature/gift/claim/GiftClaimViewModelTest.kt`(Turbine + Fake 패턴)

## 작업

1. **UiState/이벤트** — `ui/feature/nfc/NfcClaimUiState.kt`:
   - 진행 단계 상태(예):
     ```kotlin
     data class NfcClaimUiState(
         val claiming: Boolean = false,        // 발급 호출 진행중(따닥/중복 가드)
         val claimedCoupon: NfcClaimResultUi? = null, // 발급 성공 결과(요약)
         val errorMessage: String? = null,     // 인라인 에러 안내(한국어)
     )
     data class NfcClaimResultUi(val amountLabel: String, val expiresLabel: String, val message: String?)
     sealed interface NfcClaimEvent {
         data class Claimed(val coupon: NfcClaimResultUi) : NfcClaimEvent // 토스트/다이얼로그 + 기프티콘 목록 이동
     }
     ```
   - 금액/유효기한 라벨 포맷(예: `1000` → "1,000원", `expiresAtIso` → "2026.12.25 까지")은 ViewModel 또는 매퍼에서
     처리. ISO 파싱 실패는 graceful(원문 또는 빈 라벨). 디자인 토큰/한국어.

2. **ViewModel** — `ui/feature/nfc/NfcClaimViewModel.kt`(`@HiltViewModel`, `NfcCouponRepository` 주입):
   - `val uiState: StateFlow<NfcClaimUiState>` + `val events: SharedFlow<NfcClaimEvent>`(GiftClaim 패턴).
   - `fun onTagRead(rawPayload: String)`:
     - `NfcTagCode.parse(rawPayload)` 로 코드 추출. **null 이면** `errorMessage = "유효하지 않은 태그예요"` 설정 후 종료.
     - **중복/따닥 가드**: `uiState.value.claiming == true` 이면 즉시 무시(재진입 차단). 또한 직전 성공 결과가
       있으면(이미 발급 완료) 재호출 무시 또는 새 시도 허용 — 최소한 진행중 재호출은 반드시 막는다.
     - `claiming = true, errorMessage = null` 로 전이 → `repository.claim(code)` 호출.
       - 성공 → `claiming = false`, `claimedCoupon = 결과`, `events.emit(Claimed(결과))`.
       - 실패 → `claiming = false`, `errorMessage = error.toNfcMessage()`.
     - 호출은 예외 비전파로 감싼다(`CancellationException` 재throw, 그 외 → `DomainError.Unknown` 취급).
   - `fun consumeError()`/`onRetry()` 등 에러 해제 헬퍼(선택). 화면 이탈/재태깅 시 상태 리셋 헬퍼 제공.
   - `DomainError`→한국어 메시지 매핑(`toNfcMessage()`):
     - `Payment("nfc-cooldown")` → "오늘은 이미 받았어요. 내일 다시 시도해 주세요"
     - `Payment("nfc-inactive")` → "사용할 수 없는 태그예요"
     - `Payment(_)` (기타) → "쿠폰을 발급할 수 없어요"
     - `NotFound` → "유효하지 않은 태그예요"
     - `Unauthorized` → "로그인이 필요해요. 다시 로그인해 주세요"
     - `Validation` → "태그를 다시 인식해 주세요"
     - `Network` → "네트워크 연결을 확인하고 다시 시도해 주세요"
     - `Timeout` → "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
     - `Unknown` → "쿠폰 발급에 실패했어요. 잠시 후 다시 시도해 주세요"

### 핵심 규칙 (반드시 준수)

- 레이어 분리: 화면→ViewModel→`NfcCouponRepository` 만. ViewModel 에 안드로이드 `NfcAdapter`/`Context` 비주입
  (raw 문자열만 받는다). 도메인/데이터 비종속.
- 금전성 발급 — 낙관적 UI 금지: 성공 이벤트/결과는 **리포지토리 성공 응답 이후에만** 방출.
- `tagCode`/raw 페이로드/발급 결과의 민감 식별자를 로깅하지 마라.
- **TDD 가드**: 신규 `.kt` 마다 대응 `...Test.kt` 를 먼저 만든다(`NfcClaimViewModelTest`, 필요시 `NfcClaimUiStateTest`).

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)** — `NfcClaimViewModelTest`(Turbine + Fake `NfcCouponRepository`):
   - 정상 코드 태깅 → `claiming` true→false, `Claimed` 이벤트 1회, `claimedCoupon` 채워짐.
   - 잘못된 raw(파싱 null) → 즉시 `errorMessage`, 리포지토리 호출 없음.
   - 진행중 재태깅(따닥) → 두 번째 호출 무시(리포지토리 1회만 호출).
   - 각 `DomainError` → 해당 한국어 메시지 매핑(cooldown/inactive/notfound/unauthorized/network 등).
2. AC 통과·무회귀.
3. 체크리스트: 중복 가드, 낙관적 금지(성공 후 이벤트), 메시지 매핑, 레이어 분리, 미로깅.
4. `phases/29-nfc-coupon-claim/index.json` step 2 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "NfcClaimViewModel/UiState — onTagRead(raw)→NfcTagCode.parse→claim, 진행중/따닥 중복가드, 성공시 Claimed 일회성이벤트(낙관 금지), DomainError→한국어 메시지(cooldown/inactive/notfound/unauthorized/network...). Turbine 단위테스트."`
   - 3회 실패 → `"status": "error"` + `"error_message"` / 사용자 개입 필요 → `"status": "blocked"` + `"blocked_reason"`

## 금지사항

- `NfcAdapter`/리더모드/Compose 화면/매니페스트/네비를 건드리지 마라(step3 범위).
- step1 의 data 계약(파서/API/리포지토리)을 재설계하지 마라. 이미 만든 것을 사용하라.
- 성공 응답 전에 성공 UI/이벤트를 내지 마라(낙관적 UI 금지). 민감값 로깅 금지.
