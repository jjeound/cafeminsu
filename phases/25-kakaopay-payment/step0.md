# Step 0: kakaopay-pg-client (카카오페이 PG 클라이언트 + 키게이트 DI)

카카오페이 결제 연동의 1단계. 카카오페이 `ready → (리다이렉트) → approve` 흐름을 기존 `PgClient`
인터페이스 뒤에 둔다. 리다이렉트 UI(브리지 실구현)·결제 화면은 **step1**. 이 step 은 **data + di 레이어만**.
카카오페이 미설정(키/엔드포인트 부재) 시 `MockPgClient` 폴백으로 기존 결제 플로우/테스트가 무회귀해야 한다.

> **중요(보안)**: 카카오페이 어드민키(시크릿)는 **서버 전용**이다. 앱은 자사 백엔드의 프록시 엔드포인트만
> 호출한다(`api/payments/kakaopay/ready`·`approve`). 앱은 어드민키를 보관·로깅하지 않는다.

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md` (결제 안전: 멱등키, 낙관 금지, 예외 비전파)
- `/docs/SERVER_INTEGRATION.md` (§결제: prepare→PG SDK→verify 2단계, 멱등 `merchantUid`,
  `paymentMethodToken ≈ impUid` 매핑)
- `/docs/SECURITY.md` (§3 결제: 카드 PAN/CVC·토큰 미저장·미로깅, PG 토큰만)
- `/docs/ADR.md` (ADR-006/010: 결제 Mock-first, 실 키 필요 step 처리)
- `app/src/main/java/com/cafeminsu/data/payment/PgClient.kt` (`authorize(merchantUid, amount): AppResult<String>`, `MockPgClient`)
- `app/src/main/java/com/cafeminsu/data/repository/RealPaymentRepository.kt` (prepare→authorize→verify→getPayment)
- `app/src/main/java/com/cafeminsu/data/remote/PaymentApi.kt` (prepare/verify/getPayment 계약)
- `app/src/main/java/com/cafeminsu/data/remote/NetworkModule.kt` (Retrofit/api provide 패턴)
- `app/src/main/java/com/cafeminsu/di/RepositoryModule.kt` (`bindPgClient`, `select*Repository` 키게이트 패턴)
- `app/src/main/java/com/cafeminsu/core/AppResult.kt`, `data/remote/runCatchingToAppResult` 위치
- `app/src/test/java/com/cafeminsu/data/payment/PgClientTest.kt`
- `app/src/test/java/com/cafeminsu/data/repository/RealPaymentRepositoryTest.kt`
- `app/build.gradle.kts` (buildConfigField/`local.properties` 로딩 패턴 — `BASE_URL`/`KAKAO_NATIVE_APP_KEY` 참고)

## 작업

1. **KakaoPayApi (Retrofit)** — `data/remote/KakaoPayApi.kt`:
   - `POST api/payments/kakaopay/ready {merchantUid, amount}` → `{tid, redirectUrl}`
   - `POST api/payments/kakaopay/approve {tid, pgToken, merchantUid}` → `{paymentToken}`
     (`paymentToken` 은 기존 `verify(impUid, merchantUid)` 의 `impUid` 슬롯에 들어갈 값)
   - 요청/응답 DTO 정의. `NetworkModule` 에 `provideKakaoPayApi(retrofit)` 추가.

2. **KakaoPayRedirectBridge (인터페이스)** — `data/payment/KakaoPayRedirectBridge.kt`:
   ```kotlin
   interface KakaoPayRedirectBridge {
       suspend fun awaitPgToken(redirectUrl: String): AppResult<String>
   }
   ```
   - 실구현(커스텀 탭 + `pg_token` 캡처)은 **step1**. 이 step 에서는 인터페이스만 선언하고, 테스트에서는
     Fake(고정 `pg_token` 반환)를 사용한다. DI 에 임시 기본 구현이 필요하면 `pg_token` 없이 실패를 돌려주는
     no-op 바인딩을 둬도 되나, **키게이트가 false(기본)면 KakaoPayPgClient 자체가 주입되지 않으므로** no-op 로 충분.

3. **KakaoPayPgClient : PgClient** — `data/payment/KakaoPayPgClient.kt`:
   - `authorize(merchantUid, amount)`:
     1. `kakaoPayApi.ready(merchantUid, amount)` → `tid`, `redirectUrl`
     2. `redirectBridge.awaitPgToken(redirectUrl)` → `pgToken`
     3. `kakaoPayApi.approve(tid, pgToken, merchantUid)` → `paymentToken`
     4. `AppResult.Success(paymentToken)` 반환
   - 모든 호출 `runCatchingToAppResult`/`AppResult` 래핑, 실패는 `DomainError.Payment(...)`/`Network` 등으로 매핑.
   - 검증: `merchantUid` 공백 → `DomainError.Validation("merchantUid")`, `amount <= 0` → `Validation("amount")`
     (MockPgClient 와 동일 가드).

4. **키게이트 DI** — `di/RepositoryModule.kt`:
   - `app/build.gradle.kts` 에 `KAKAOPAY_ENABLED` buildConfigField 추가(`local.properties` 의 `KAKAOPAY_ENABLED`,
     없으면 기본 `"false"`). `BASE_URL`/`KAKAO_NATIVE_APP_KEY` 로딩 패턴을 그대로 따른다.
   - 기존 `@Binds bindPgClient(MockPgClient)` 를 `@Provides` 키게이트로 교체:
     ```kotlin
     @Provides @Singleton
     fun providePgClient(
         real: Provider<KakaoPayPgClient>,
         mock: Provider<MockPgClient>,
     ): PgClient =
         if (BuildConfig.KAKAOPAY_ENABLED) real.get() else mock.get()
     ```
   - 키 부재(현재 기본) → `MockPgClient` 유지. `RealPaymentRepository` 의 prepare/verify/멱등(`merchantUid`)
     경로는 **건드리지 않는다**.

### 핵심 규칙 (반드시 준수)

- **미저장·미로깅**: 카드 PAN/CVC 는 모델에 없음(유지), `pgToken`/`tid`/`paymentToken` 을 로그에 남기지 마라.
  이유: PCI 범위 축소·민감정보 유출 방지(SECURITY §3).
- **멱등 유지**: 서버 식별은 기존 `merchantUid`. `RealPaymentRepository` 의 멱등/낙관금지 로직을 변경하지 마라.
- **`AppResult` 래핑**: PG/네트워크 호출에서 예외를 던지지 마라.
- **무회귀**: 기본(키게이트 false)에서 결제 플로우와 모든 기존 테스트가 그대로 통과해야 한다.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**: 구현 전에 `KakaoPayPgClientTest`(`app/src/test/.../data/payment/`)를 작성한다.
   MockWebServer 로 `ready`→`approve` 응답을 스텁하고, `KakaoPayRedirectBridge` 는 Fake(고정 `pg_token`) 사용:
   - 성공: ready→approve 거쳐 `paymentToken` 반환
   - approve 실패(HTTP 4xx/5xx) → `AppResult.Failure(DomainError.Payment/...)`
   - `merchantUid` 공백/`amount<=0` 검증 실패
   먼저 실패하는 테스트를 만든 뒤 구현한다(`scripts/hooks/tdd-guard.sh`).
2. 위 AC 통과 확인. **기존** `RealPaymentRepositoryTest`·`PgClientTest`(Mock) **무회귀**.
3. 아키텍처 체크리스트:
   - 키게이트 false 에서 `MockPgClient` 가 주입되는가? 결제 플로우 무회귀인가?
   - 멱등(`merchantUid`)·낙관금지 경로를 보존했는가?
   - 토큰/PG 식별자 미로깅인가?
4. 결과에 따라 `phases/25-kakaopay-payment/index.json` 의 step 0 을 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "KakaoPayApi(ready/approve) + KakaoPayPgClient(PgClient) + KakaoPayRedirectBridge 인터페이스 + KAKAOPAY_ENABLED 키게이트 DI(기본 Mock 폴백). 멱등 merchantUid 유지"`
   - 3회 실패 → `"status": "error"`, `"error_message": "<에러>"`
   - 사용자 개입 필요(실 키 강제 필요 등) → `"status": "blocked"`, `"blocked_reason": "<사유>"` 후 중단

## 금지사항

- 카카오페이 어드민키/시크릿을 앱·`local.properties` 에 넣거나 클라이언트에서 카카오페이 서버를 직접 호출하지
  마라. 이유: 시크릿은 서버 전용, PCI/유출 위험. 앱은 자사 백엔드 프록시만 호출한다.
- `RealPaymentRepository` 의 prepare/verify/멱등 로직을 변경하지 마라. 이유: 결제 무결성 회귀 위험.
- UI/Compose/AndroidManifest/리다이렉트 실구현을 건드리지 마라. 이유: step1 범위.
- 토큰/PG 식별자를 로깅하지 마라. 기존 테스트를 깨뜨리지 마라.
