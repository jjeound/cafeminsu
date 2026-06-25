# Step 4 — 결제를 카카오페이로 통합 (스웨거 정합 확인)

## 배경
백엔드(BASE_URL=`https://cafeminsu.duckdns.org/`) 스웨거를 재확인한 결과, 카카오페이 프록시 엔드포인트는
앱의 `data/remote/KakaoPayApi.kt` 계약과 **정확히 일치**한다:
- `POST /api/payments/kakaopay/ready` — 요청 `{ merchantUid: string, amount: int(min 1) }` → 응답 `{ tid, redirectUrl }`
- `POST /api/payments/kakaopay/approve` — 요청 `{ tid, pgToken, merchantUid }` → 응답 `{ paymentToken }`
- 이후 `POST /api/payments/verify` `{ impUid=<paymentToken>, merchantUid }` → `{ paymentId, status }` (기존 흐름).

PG 게이트는 `di/RepositoryModule.providePgClient` 가 `BuildConfig.KAKAOPAY_ENABLED` 로
`KakaoPayPgClient`(실연동) vs `MockPgClient` 를 선택한다. (실제 on/off 는 `local.properties` 의
`KAKAOPAY_ENABLED` — **런타임/로컬 설정이라 이 step의 코드 변경 범위 밖**.)

문제: 결제 화면은 신용카드/간편결제/카카오페이 3개 수단을 보여 주지만, 실연동 시 어떤 수단을 골라도 동일한
PgClient(카카오페이)를 탄다. 요구("결제는 카카오페이로 통합"): 표시 수단을 실제 PG(카카오페이)에 맞춘다.

## 작업 범위 (이 step에서만)
1. **스웨거 정합 재검증**: `KakaoPayApi.kt`(ready/approve 요청·응답 필드명·타입)와 `RealPaymentRepository`
   (prepare→authorize(pgClient)→verify) 흐름이 위 계약과 일치하는지 확인하고, **불일치가 있으면만** 맞춘다
   (현재로선 변경 없을 것으로 예상 — 불필요한 수정 금지).
2. **결제 수단 통합**: 결제 화면의 기본/주 결제수단을 **카카오페이**로 한다.
   `ui/feature/payment/PaymentUiState.kt` 의 `defaultPaymentMethods()` 와 `PaymentViewModel` 의
   내부 `paymentMethods` 가 일관되도록 정리하고, **진입 시 기본 선택을 `kakaopay`** 로 한다.
   (신용카드/간편결제 목 수단은 제거하거나 카카오페이를 첫번째·기본 선택으로 두는 방식 중 코드 영향이 작은 쪽을
   택한다. 핵심: 사용자가 보는 결제수단이 실제 PG=카카오페이와 일치.)
3. 관련 테스트(`PaymentViewModelTest`, `PaymentUiState`/메서드 선택 테스트)를 새 수단 구성에 맞게 갱신.

## 금지 / 불변
- 카카오페이 시크릿/PG 토큰 저장·로깅 금지. `KAKAOPAY_ENABLED` 플래그를 코드에서 켜지 않는다.
- 결제 멱등키·낙관 금지·`AppResult` 에러 매핑·verify 확정 흐름 불변.
- 디자인 토큰/한국어 카피 가드레일 준수.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 4 를 `completed` + `summary`(스웨거 정합 확인 결과 포함)로 갱신·커밋.
