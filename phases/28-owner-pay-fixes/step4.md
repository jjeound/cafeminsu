# Step 4: kakaopay-real-checkout (카카오페이 ready/approve 실연동)

결제 화면이 아직 **Mock "결제 성공/실패" 버튼**("PG 미연동" 배너)으로 동작한다. 실제 결제 API 는
**카카오페이 `ready`/`approve`** (`POST /api/payments/kakaopay/ready`·`/approve`)다. 관련 코드
(`KakaoPayPgClient`, `RealKakaoPayRedirectBridge`(커스텀탭+pg_token), 매니페스트 딥링크 `cafeminsu://kakaopay`)는
phase 25 에서 이미 구현됐으나 **`KAKAOPAY_ENABLED` 키게이트가 미설정→`false`** 라 `MockPgClient` 로 폴백된다.
이 step 은 키게이트를 켜고, 결제 화면이 **"카카오페이로 결제하기"** 버튼으로 실제 ready→approve→verify 를 타게 한다.

> **보안**: 카카오페이 어드민키/시크릿은 **서버 전용**. 앱은 자사 백엔드 프록시(`kakaopay/ready`·`approve`)만
> 호출한다. `pg_token`/`tid`/`paymentToken` 미저장·미로깅(`SECURITY §3`). verify 확정 전 성공화면 금지(낙관 금지).

## 읽어야 할 파일

- `app/src/main/java/com/cafeminsu/ui/feature/payment/PaymentScreen.kt` (Mock 성공/실패 버튼, "PG 미연동" 배너, 결제수단 칩)
- `app/src/main/java/com/cafeminsu/ui/feature/payment/PaymentViewModel.kt` (`onPay`/`onPaySuccess`/`payWithMockOutcome`, `paymentMethods` mock 토큰)
- `app/src/main/java/com/cafeminsu/ui/feature/payment/PaymentUiState.kt` (결제수단·진행상태)
- `app/src/main/java/com/cafeminsu/data/repository/RealPaymentRepository.kt` (prepare→`pgClient.authorize`→verify)
- `app/src/main/java/com/cafeminsu/data/payment/KakaoPayPgClient.kt` · `KakaoPayRedirectBridge.kt`
- `app/src/main/java/com/cafeminsu/data/platform/RealKakaoPayRedirectBridge.kt` (커스텀탭+딥링크 캡처)
- `app/src/main/java/com/cafeminsu/di/RepositoryModule.kt` (`providePgClient`/`provideKakaoPayRedirectBridge` 키게이트)
- `app/build.gradle.kts` (`KAKAOPAY_ENABLED` buildConfigField) · `local.properties`
- `app/src/main/AndroidManifest.xml` (`cafeminsu://kakaopay` 딥링크 — phase 25)
- `app/src/test/java/com/cafeminsu/ui/feature/payment/PaymentViewModelTest.kt`
- `phases/25-kakaopay-payment/step0.md` · `step1.md`
- `/docs/ARCHITECTURE.md`(결제 안전) · `/docs/SECURITY.md`(§3)

## 작업

1. **키게이트 활성화** — `local.properties` 에 `KAKAOPAY_ENABLED=true` 추가/설정(로컬·gitignore).
   - DI `providePgClient` → `KakaoPayPgClient`, `provideKakaoPayRedirectBridge` → `RealKakaoPayRedirectBridge` 주입됨.
   - `app/build.gradle.kts` 의 `KAKAOPAY_ENABLED` buildConfigField 로딩은 phase 25 에서 존재 — 확인만(없으면 추가).

2. **결제 화면 실결제 전환** — `PaymentScreen`/`PaymentViewModel`/`PaymentUiState`:
   - Mock "결제 성공"/"결제 실패" 이중 버튼과 "PG 미연동" 안내 배너를 제거하고, 단일 **primary "카카오페이로 결제하기"** 버튼이 `onPay()` 를 호출하게 한다.
   - `onPay()` 는 선택 결제수단(기본 카카오페이) 토큰으로 `paymentRepository.pay(request)` 호출 → `RealPaymentRepository` 가 `prepare → KakaoPayPgClient.authorize`(ready→커스텀탭 pg_token→approve) `→ verify → getPayment` 를 수행. **verify/상태조회 확정 후에만** `ORDER_OK` 전환.
   - `payWithMockOutcome`/`MockFailureToken`/`MockPaymentOutcome`/mock 토큰 의존 정리: 실패는 실제 결과(verify FAILED)·사용자 취소(pg_token 없음 → `kakaopay-cancelled`)에서 발생. `onPayFailure`/`onPaySuccess` 같은 mock 진입점은 제거하거나 테스트 전용으로 격리.
   - 처리 중 버튼 비활성·중복제출 가드·Unknown 미성공·낙관 금지 — **기존 안전 로직 유지**. 실패 시 `ORDER_FAIL` 다이얼로그(재시도).
   - 결제수단 칩은 유지하되 카카오페이가 기본 선택. (신용카드/간편결제는 현재 백엔드 미지원이면 비활성/숨김 — 화면 회귀 최소화 범위에서 처리.)

### 핵심 규칙 (반드시 준수)

- **낙관 금지 / Unknown**: verify·상태조회 확정 전 성공화면 금지. Unknown 은 성공 아님.
- **미로깅**: `pg_token`/`tid`/`paymentToken`/PG 토큰 로그 금지.
- **레이어**: Activity/커스텀탭/딥링크 의존은 UI·플랫폼 레이어(`RealKakaoPayRedirectBridge`)에만. `PaymentRepository`/도메인은 Context 비종속.
- **무회귀**: prepare/verify/멱등(`merchantUid`) 경로·기존 결제 안전 가드를 깨지 마라.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**: `PaymentViewModelTest` 갱신 — "카카오페이로 결제하기 → 승인(verify PAID) 시 `PaymentApproved` 이벤트/ORDER_OK", "사용자 취소·verify FAILED → `PaymentFailed`/ORDER_FAIL", "처리 중 중복 제출 무시" 를 Turbine 으로. 리다이렉트 브리지는 Fake(고정/취소 pg_token). mock 성공/실패 진입점 제거에 맞춰 기존 테스트 정리.
2. 위 AC 통과. **기존** `RealPaymentRepositoryTest`·`KakaoPayPgClientTest` **무회귀**.
3. 체크리스트: 낙관금지·Unknown 미성공·중복가드 유지? pg_token 미로깅? Activity/딥링크 의존이 UI/플랫폼에만?
4. 결과에 따라 `phases/28-owner-pay-fixes/index.json` 의 step 4 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "KAKAOPAY_ENABLED=true 로 카카오페이 실연동 활성화 + 결제화면 Mock 성공/실패 버튼 제거 → '카카오페이로 결제하기'가 ready→approve→verify 실결제(낙관금지·중복가드·pg_token 미로깅 유지)"`
   - 3회 실패 → `"status": "error"`, `"error_message": "<에러>"`
   - 사용자 개입 필요(서버 카카오페이 cid 미설정 등) → `"status": "blocked"`, `"blocked_reason": "<사유>"` 후 중단

## 금지사항

- 카카오페이 어드민키/시크릿을 앱·`local.properties` 에 넣지 마라(서버 전용). 클라이언트에서 카카오페이 서버 직접 호출 금지.
- verify 확정 전 성공화면 전환 금지(낙관 금지). `pg_token`/PG 토큰 로깅 금지.
- `RealPaymentRepository` 의 prepare/verify/멱등 로직을 변경하지 마라. 기존 결제 테스트를 깨뜨리지 마라.
