# 카카오톡 친구 선물 — 백엔드 연동 가이드

앱의 선물 흐름을 **카카오 친구 피커 → 선택 친구에게 카카오톡 메시지로 기프티콘 링크 전송 → 받는 사람이
앱에서 수동 등록(claim)** 으로 바꾼다. 친구 피커는 카카오 **friend uuid** 만 주고, 앱은 본인의 숫자 userId 를
보유하지 않으며(토큰은 **발신자**만 식별), 서버는 현재 구매 시 `receiverId`/`receiverPhone` 를 필수로 요구한다.

따라서 "친구에게 보내는 선물"이 성립하려면 백엔드가 아래 두 가지를 지원해야 한다. 이 문서는 그 변경 사항을
정리한다.

> 전체 흐름
> `발신자: 친구 피커(uuid) → 구매(수신자 미지정, JWT=발신자) → claimCode/shareLink 발급 →`
> `카카오톡 메시지(또는 공유)로 친구에게 링크 전달`
> `수신자: 링크/코드 → 앱에서 등록(claim, JWT=받는 사람) → 내 기프티콘에 귀속`

---

## 1. 구매 — 수신자 미지정 허용 (변경)

현재 `POST api/gifticons` 는 `receiverId` 또는 `receiverPhone` 가 필수다. 친구 선물에서는 구매 시점에 수신자를
바인딩하지 않는다(받는 사람은 등록 시점에 확정). 따라서 **수신자 없이도 구매 가능**해야 한다.

```
POST api/gifticons
Authorization: Bearer {카카오 JWT}   ← 구매자(발신자) 식별
Content-Type: application/json

요청  { "amount": 10000, "message": "오늘 하루 수고했어 ☕" }   ← receiverId/receiverPhone 없음
응답  { "gifticonId": 123, "claimCode": "GFT-XXXX-XXXX", "shareLink": "https://.../gift?code=GFT-XXXX-XXXX" }
```

- 구매자는 **JWT 로 식별**(별도 식별 파라미터 미전송, 기존 규약과 동일).
- `claimCode`: 받는 사람이 등록할 때 쓰는 **1회성 클레임 코드**. 추측 불가하게 발급.
- `shareLink`: 클레임 안내 웹페이지 또는 앱 딥링크로 연결되는 URL. 딥링크 형식은 §4 참고.
- 기존 SMS/직접 수신자 지정 구매(`receiverPhone`)는 **그대로 유지**(하위호환). 수신자 미지정은 추가 케이스.
- 낙관 금지·결제 정합 등 기존 선물/결제 규약 유지.

> ⚠️ 보안: `claimCode` 는 베어러 토큰처럼 동작하므로 추측 불가·만료·1회 사용으로 관리. 로그 미노출.

---

## 2. 등록(claim) — 받는 사람이 코드로 귀속 (신규)

받는 사람이 앱에서 코드/링크로 기프티콘을 자기 계정에 등록한다.

```
POST api/gifticons/claim
Authorization: Bearer {카카오 JWT}   ← 받는 사람 식별
Content-Type: application/json

요청  { "claimCode": "GFT-XXXX-XXXX" }
응답  { "gifticonId": 123, "title": "금액형 기프티콘 10,000원", "barcodeValue": "...", "qrValue": "...",
        "expiresAtMillis": 1750000000000, "status": "AVAILABLE" }
```

처리 규약:
- 유효한 코드 → 호출자(JWT) 계정에 귀속하고 기프티콘 반환. 이후 `GET api/gifticons/my` 에 노출.
- **이미 등록됨** → 409/적절한 에러(예: `already-claimed`). 본인이 이미 등록했으면 멱등 성공 처리 가능.
- **만료/취소/없음** → 4xx 에러(코드별 구분). 앱은 `AppResult.Failure(DomainError.*)` 로 매핑.
- 발신자 본인이 자기 코드를 등록하는 케이스 정책 명시(허용/차단).

---

## 3. 카카오톡 메시지 템플릿용 필드 (권장)

앱은 선택한 친구(uuid)에게 카카오 `TalkApiClient.sendDefaultMessage` 로 메시지를 보낸다(앱 미가입 친구는
공유로 폴백). 메시지 구성을 위해 구매 응답이 아래를 제공하면 좋다(없으면 앱이 기본 문구 사용).

| 필드 | 용도 |
|------|------|
| `amount` | 메시지 타이틀("10,000원 기프티콘 도착") |
| `shareLink` | 메시지 버튼/링크 목적지(클레임 페이지/딥링크) |
| `message` | 보낸 사람의 한마디(선택) |

메시지/공유 링크를 누르면 §4 딥링크 또는 클레임 웹페이지로 진입 → 앱에서 등록(claim).

---

## 4. 공유 링크 / 딥링크 규약

- `shareLink` 는 다음 중 하나로 귀결:
  - 앱 딥링크 **`cafeminsu://gift?code=GFT-XXXX-XXXX`** (앱 설치 시 등록 화면 자동 진입), 또는
  - 웹 클레임 안내 페이지(앱 미설치자용) → "앱에서 열기" 버튼이 위 딥링크로 연결.
- 앱은 `cafeminsu://gift` 딥링크를 수신해 `code` 를 추출하고 등록 화면을 띄운다(매니페스트 intent-filter).
- 수동 입력 대비: 앱 등록 화면에서 `claimCode` 직접 입력도 지원.

---

## 5. 앱 측 동작 / 게이트 (참고)

- 발신자: 카카오톡 채널 → 친구 피커(`PickerApi`)로 친구 선택 → 구매(수신자 미지정) → `TalkApiClient` 메시지
  전송. 앱 미가입 친구/메시지 실패 시 `ShareClient` 공유로 폴백.
- ⚠ 카카오 `friends`/`talk_message` 스코프는 **검수 + 비즈니스 앱** 필요. 검수 전엔 등록 팀원 계정만 동작.
- 백엔드 §1·§2 미구현 시 앱 신경로는 Mock/게이트로 무동작(기존 동작·테스트 무회귀). 구현 후 활성.

---

## 참조 (앱 측 근거)
- 선물 흐름: `app/src/main/java/com/cafeminsu/data/repository/RealGiftRepository.kt`,
  `app/src/main/java/com/cafeminsu/data/remote/GifticonApi.kt`
- 발신자 식별(토큰): `app/src/main/java/com/cafeminsu/data/remote/AuthApi.kt`(`UserProfile.id`=`server-user` 상수),
  인증 인터셉터(Bearer JWT)
- 공유 폴백: `app/src/main/java/com/cafeminsu/ui/feature/gift/GiftScreen.kt`(`launchKakaoShare`)
- 기존 선물 서버 규약: `docs/SERVER_INTEGRATION.md`(선물 절)
