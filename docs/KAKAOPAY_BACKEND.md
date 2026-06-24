# 카카오페이 결제 — 백엔드 연동 가이드

앱(안드로이드)의 카카오페이 결제(phase 25)는 **자사 백엔드 프록시**만 호출하도록 구현돼 있다. 앱은
카카오페이 가맹점 시크릿을 **보관하지 않는다**(SECURITY §3, PCI 범위 축소). 실제 카카오페이 가맹점
CID·시크릿키와 `ready`/`approve` REST 호출은 **백엔드 서버**가 담당해야 한다.

이 문서는 백엔드 팀이 그대로 구현할 수 있도록 서버가 보관할 키, 구현할 엔드포인트, 요청/응답 계약,
카카오페이 매핑, 리다이렉트 규약, verify 연계를 정리한다.

> 전체 흐름 요약
> `앱 결제 → ready(앱→서버→카카오페이) → 앱이 redirectUrl 브라우저 오픈 → 사용자 인증 →`
> `pg_token 딥링크로 앱 복귀 → approve(앱→서버→카카오페이) → verify(앱→서버, 기존 흐름) → 결제 확정`

---

## 1. 서버가 보관할 키 (앱 아님)

서버 환경변수/시크릿으로만 보관하고 **로그에 남기지 않는다.**

| 키 | 설명 |
|----|------|
| `KAKAOPAY_CID` | 가맹점 코드. 테스트 `TC0ONETIME`, 운영은 카카오페이에서 발급받은 CID. |
| `KAKAOPAY_SECRET_KEY` (신규 오픈API) **또는** `KAKAOPAY_ADMIN_KEY` (레거시) | 카카오페이 인증 키. 서버 전용. |

카카오페이 호출 시 Authorization 헤더:

- 신규 오픈API(`https://open-api.kakaopay.com`): `Authorization: SECRET_KEY {KAKAOPAY_SECRET_KEY}`
- 레거시(`https://kapi.kakao.com`): `Authorization: KakaoAK {KAKAOPAY_ADMIN_KEY}`

> ⚠️ 앱에는 카카오페이 시크릿을 절대 넣지 않는다. 앱이 가진 카카오페이 설정은 `KAKAOPAY_ENABLED` on/off
> 플래그뿐이다(§6).

---

## 2. 앱 ↔ 백엔드 계약 (이미 앱에 구현됨 — 서버가 맞춰야 함)

출처: `app/src/main/java/com/cafeminsu/data/remote/KakaoPayApi.kt`. 필드명·타입을 그대로 맞춘다(camelCase).
인증은 기존 보호 API와 동일하게 **카카오 Bearer JWT** 인터셉터가 자동 부착된다.

### 2-1. ready

```
POST {BASE_URL}/api/payments/kakaopay/ready
Authorization: Bearer {카카오 JWT}
Content-Type: application/json

요청  { "merchantUid": "string", "amount": 0 }
응답  { "tid": "string", "redirectUrl": "string" }
```

### 2-2. approve

```
POST {BASE_URL}/api/payments/kakaopay/approve
Authorization: Bearer {카카오 JWT}
Content-Type: application/json

요청  { "tid": "string", "pgToken": "string", "merchantUid": "string" }
응답  { "paymentToken": "string" }
```

`paymentToken` 은 이후 **기존 verify** 의 `impUid` 슬롯에 그대로 들어간다(§5).

---

## 3. 서버 → 카카오페이 매핑

### 3-1. ready (서버가 카카오페이 `payment/ready` 호출)

| 카카오페이 파라미터 | 값 |
|---------------------|-----|
| `cid` | `KAKAOPAY_CID` |
| `partner_order_id` | 앱이 보낸 `merchantUid` |
| `partner_user_id` | 인증된 사용자 식별자(JWT 기준) |
| `item_name` | 주문 요약(예: "민수 주문") |
| `quantity` | `1` |
| `total_amount` | 앱이 보낸 `amount` |
| `tax_free_amount` | `0` |
| `approval_url` | **`cafeminsu://kakaopay`** (핵심 — §4) |
| `cancel_url` | `cafeminsu://kakaopay` (파라미터 없음) 권장 |
| `fail_url` | `cafeminsu://kakaopay` (파라미터 없음) 권장 |

- 카카오페이 응답의 `tid` 를 **`merchantUid` 키로 서버에 저장**한다(approve 에서 사용).
- **멱등**: 동일 `merchantUid` 재요청은 동일 `tid` 를 반환(중복 결제 방지). 기존 서버 결제 멱등 규약과 동일.
- 앱에 `{ "tid": tid, "redirectUrl": next_redirect_mobile_url }` 반환
  (모바일 웹뷰가 아니면 `next_redirect_app_url` 사용).

### 3-2. approve (서버가 카카오페이 `payment/approve` 호출)

| 카카오페이 파라미터 | 값 |
|---------------------|-----|
| `cid` | `KAKAOPAY_CID` |
| `tid` | 앱이 보낸 `tid`(저장분과 대조) |
| `partner_order_id` | `merchantUid` |
| `partner_user_id` | 인증 사용자 식별자 |
| `pg_token` | 앱이 보낸 `pgToken` |

- 성공 시 앱에 `{ "paymentToken": ... }` 반환.
- `paymentToken` 은 **verify 가 이 결제를 조회·확정할 수 있는 값**이어야 한다(예: 카카오페이 `aid`, 또는
  서버 내부 결제 식별자). 임의 난수 금지 — verify 와 연결되는 키여야 한다.

---

## 4. 리다이렉트(`pg_token`) 규약 — 반드시 일치

출처: `KakaoPayRedirectActivity.kt`, `RealKakaoPayRedirectBridge.kt`, `app/src/main/AndroidManifest.xml`.

1. 앱이 ready 응답의 `redirectUrl` 을 외부 브라우저로 연다.
2. 사용자가 카카오페이에서 결제 인증을 마치면, 카카오페이가 `approval_url` 로 리다이렉트하며 `pg_token` 을 부여한다.
3. 앱은 다음 형태의 딥링크만 수락해 `pg_token` 을 추출한다(화이트리스트):

   ```
   cafeminsu://kakaopay?pg_token=<pg_token>
   ```
   - scheme = `cafeminsu`, host = `kakaopay`, query = `pg_token`

따라서 서버가 카카오페이에 넘기는 `approval_url` 은 **최종적으로 위 딥링크로 귀결**돼야 한다.
가장 단순한 방법은 `approval_url` 을 곧장 `cafeminsu://kakaopay` 로 두는 것이다(카카오페이가 `pg_token` 을
자동 append). 서버 중간 URL 을 쓰는 경우, 해당 URL 이 302 로 `cafeminsu://kakaopay?pg_token=<pg_token>` 로
리다이렉트하면 된다.

**취소/실패**: 앱은 `pg_token` 이 없으면 결제 취소(`kakaopay-cancelled`)로 처리한다. `cancel_url`/`fail_url` 을
파라미터 없는 `cafeminsu://kakaopay` 로 보내면 자연스럽게 취소로 귀결된다.

---

## 5. verify 연계 (기존 흐름 불변)

출처: `RealPaymentRepository.kt`(`authorize → verify`), `data/remote/PaymentApi.kt`,
`docs/SERVER_INTEGRATION.md`(§결제).

approve 성공 후 앱은 **기존 결제 검증 엔드포인트를 그대로** 호출한다(카카오페이 전용 verify 신설 불필요):

```
POST {BASE_URL}/api/payments/verify
요청  { "impUid": "<paymentToken>", "merchantUid": "<merchantUid>" }
응답  { "paymentId": ..., "status": "READY|PAID|FAILED|REFUNDED" }
```

- 서버 `verify` 는 카카오페이 결제의 `paymentToken` 을 `impUid` 로 받아 결제를 조회·확정해야 한다.
- **낙관 금지**: `PAID` 확정 전에는 성공으로 처리하지 않는다(앱도 verify/상태조회 확정 후에만 성공 화면 전환).
- **멱등 식별**은 기존과 동일하게 `merchantUid` 를 사용한다.

---

## 6. 앱 활성화 절차 (백엔드 준비 후)

앱 측은 코드 변경 없이 **플래그 하나**로 Mock ↔ 실연동을 전환한다.

1. `local.properties` 에 추가:
   ```properties
   KAKAOPAY_ENABLED=true
   ```
2. `app/build.gradle.kts` 가 이 값을 `BuildConfig.KAKAOPAY_ENABLED` 로 굽고,
   `RepositoryModule.providePgClient` 가 `true` 면 `KakaoPayPgClient`(실연동), `false`/미설정이면 `MockPgClient`
   를 주입한다.

> ⚠️ 백엔드에 `ready`/`approve` 엔드포인트가 없는 상태에서 `KAKAOPAY_ENABLED=true` 로 켜면 ready 호출이
> 404 로 실패한다. **백엔드 구현·배포 확인 후** 켠다. 기본값(`false`)에서는 Mock 폴백이라 빌드·테스트·기존
> 결제에 영향이 없다.

---

## 참조 (앱 측 근거 파일)

- 계약: `app/src/main/java/com/cafeminsu/data/remote/KakaoPayApi.kt`
- PG 흐름: `app/src/main/java/com/cafeminsu/data/payment/KakaoPayPgClient.kt`
- 리다이렉트: `app/src/main/java/com/cafeminsu/ui/feature/payment/KakaoPayRedirectActivity.kt`,
  `app/src/main/java/com/cafeminsu/data/platform/RealKakaoPayRedirectBridge.kt`, `app/src/main/AndroidManifest.xml`
- verify 연계: `app/src/main/java/com/cafeminsu/data/repository/RealPaymentRepository.kt`,
  `app/src/main/java/com/cafeminsu/data/remote/PaymentApi.kt`
- 키게이트: `app/build.gradle.kts`, `app/src/main/java/com/cafeminsu/di/RepositoryModule.kt`
- 기존 서버 결제 규약: `docs/SERVER_INTEGRATION.md`(§결제)
