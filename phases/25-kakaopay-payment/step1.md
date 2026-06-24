# Step 1: redirect-bridge-and-pay-ui (카카오페이 리다이렉트 브리지 + 결제 화면)

카카오페이 결제 연동의 2단계. step0 에서 인터페이스로 둔 `KakaoPayRedirectBridge` 를 실구현하고
(커스텀 탭으로 `redirectUrl` 열기 → `pg_token` 캡처), 결제 화면에 **"카카오페이"** 수단을 추가해
`PgClient`(카카오페이) 경유 결제를 연결한다. Activity/매니페스트 의존은 **UI/플랫폼 레이어**에만 둔다.

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md` (결제 안전: 처리 중 비활성·중복제출 가드·낙관 금지·Unknown 성공처리 금지)
- `/docs/SECURITY.md` (§3: PG 토큰/`pg_token` 미저장·미로깅)
- `/docs/UI_GUIDE.md`, `/docs/DESIGN_SYSTEM.md`, `/docs/SCREENS.md` (PAY 화면: 결제수단 칩, primary/secondary,
  성공→ORDER_OK·실패→ORDER_FAIL, 토큰만·한국어)
- `phases/25-kakaopay-payment/step0.md` 및 step0 산출물(`KakaoPayPgClient`, `KakaoPayRedirectBridge`, `KakaoPayApi`, 키게이트 DI)
- `app/src/main/java/com/cafeminsu/ui/feature/payment/PaymentScreen.kt`
- `app/src/main/java/com/cafeminsu/ui/feature/payment/PaymentViewModel.kt`
- `app/src/main/java/com/cafeminsu/ui/feature/payment/PaymentUiState.kt`
- `app/src/main/AndroidManifest.xml` (`AuthCodeHandlerActivity` 의 `kakao{key}://oauth` 리다이렉트 스킴 패턴)
- `app/src/main/java/com/cafeminsu/CafeApplication.kt` (Kakao 초기화)
- `app/src/main/java/com/cafeminsu/ui/navigation/AppNavHost.kt` (`Routes.PAY` → `ORDER_OK`/`ORDER_FAIL`)
- `app/src/test/java/com/cafeminsu/ui/feature/payment/PaymentViewModelTest.kt`

step0 의 PG 클라이언트/브리지 인터페이스를 읽고 의도를 이해한 뒤 작업하라.

## 작업

1. **KakaoPayRedirectBridge 실구현** — `data/payment/` 또는 `ui/feature/payment/`(Activity 의존이면 플랫폼/UI 쪽):
   - `awaitPgToken(redirectUrl)`: 커스텀 탭(또는 브라우저 Intent)으로 `redirectUrl` 을 열고, 매니페스트에 등록한
     리다이렉트 스킴(예: `cafeminsu://kakaopay`)으로 돌아오는 인텐트에서 `pg_token` 쿼리 파라미터를 캡처해
     코루틴을 재개한다(`suspendCancellableCoroutine`). 취소/오류 시 `AppResult.Failure(DomainError.Payment("kakaopay-cancelled"/...))`.
   - `AndroidManifest.xml` 에 리다이렉트 스킴 intent-filter 를 추가한다(기존 카카오 로그인 `AuthCodeHandlerActivity`
     패턴 참고: `<data android:scheme="cafeminsu" android:host="kakaopay" />`). 캡처용 액티비티/딥링크 처리 추가.
   - DI: 키게이트(`KAKAOPAY_ENABLED`)가 true 일 때 이 실구현이 `KakaoPayRedirectBridge` 로 주입되도록 바인딩.

2. **결제 화면** — `PaymentScreen`/`PaymentViewModel`/`PaymentUiState`:
   - 결제 수단 칩에 **"카카오페이"** 를 추가한다(디자인 토큰만, hex 금지). 선택 시 `PaymentRequest.paymentMethodToken`
     경로가 `PgClient`(카카오페이) 를 타도록 한다. (기존 결제수단 모델/식별자에 카카오페이 옵션 추가.)
   - 결제 처리 중 버튼 비활성 + 중복제출 가드(기존 규칙 유지). 성공 → `ORDER_OK`, 실패 → `ORDER_FAIL` 다이얼로그.
   - **Unknown 상태 성공처리 금지**, **낙관 금지**(verify/상태조회 확정 후 화면 전환) — 기존 경로 유지.
   - 키게이트 false(기본)면 카카오페이 칩을 노출하되 `MockPgClient` 토큰 경로로 동작(스모크 가능).

### 핵심 규칙 (반드시 준수)

- **로깅 금지**: `pg_token`/`tid`/PG 토큰을 로그에 남기지 마라. 이유: 민감정보 유출.
- **낙관 금지 / Unknown**: verify·상태조회로 확정되기 전 성공 화면으로 전환하지 마라. Unknown 은 성공 아님.
- **레이어 분리**: Activity/커스텀탭/딥링크 의존은 UI·플랫폼 레이어에. 도메인/데이터(`PaymentRepository`)에 Context
  를 들이지 마라(step0 의 `KakaoPayPgClient` 는 브리지 인터페이스만 의존).
- **무회귀**: 기본(키게이트 false)에서 기존 결제(카드/Mock 성공·실패) 플로우와 모든 기존 테스트가 통과해야 한다.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**: 구현 전에 `PaymentViewModelTest` 에 "카카오페이 수단 선택 → 결제 시 승인/실패 분기가
   올바른 이벤트(`PaymentApproved`/`PaymentFailed`)로 이어진다"는 실패 테스트를 Turbine 으로 추가한 뒤 구현한다.
   리다이렉트 브리지는 Activity 의존이라 단위 테스트는 Fake 브리지/계약 위주.
2. 위 AC 통과 확인. **기존** `PaymentViewModelTest`·`RealPaymentRepositoryTest` **무회귀**.
3. 아키텍처/디자인 체크리스트:
   - Activity/딥링크 의존이 UI·플랫폼 레이어에만 있는가? 도메인/데이터 비종속인가?
   - 처리 중 비활성·중복제출 가드·낙관금지·Unknown 미성공이 유지되는가?
   - hex 리터럴 0(토큰만), 카피 한국어, 안티-AI슬롭 준수인가?
   - PG 토큰/`pg_token` 미로깅인가?
4. 결과에 따라 `phases/25-kakaopay-payment/index.json` 의 step 1 을 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "KakaoPayRedirectBridge 실구현(커스텀탭+pg_token 딥링크 캡처, cafeminsu://kakaopay 매니페스트) + 결제 화면 카카오페이 수단 칩 연결(낙관금지/중복가드 유지, 기본 Mock 폴백)"`
   - 3회 실패 → `"status": "error"`, `"error_message": "<에러>"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "<사유>"` 후 중단

## 금지사항

- `PaymentRepository`/도메인에 Android `Context`/Activity 를 주입하지 마라. 이유: 레이어 비종속 규칙.
- verify/상태조회 확정 전 성공 화면으로 전환하지 마라(낙관 금지). Unknown 을 성공으로 처리하지 마라.
- `pg_token`/PG 토큰을 로깅하지 마라.
- hex 색 리터럴 금지(토큰만). 기존 결제 테스트를 깨뜨리지 마라.
