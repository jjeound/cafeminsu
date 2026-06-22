# 서버 연동 계약 (CafeMinsu ↔ 백엔드)

실서버 연동 phase(`16-server-integration`)의 **단일 진실**. 엔드포인트/DTO의 원본은 리포의
**`docs/openapi.json`**(OpenAPI 3, springdoc 원본 덤프)이고, 본 문서는 그 위에 **도메인 모델 매핑
규칙과 차이(gap)** 를 못 박는다. 도메인 계약은 `DATA_MODEL.md`, 보안은 `SECURITY.md`를 따른다.

- **Base URL**: `BuildConfig.BASE_URL`(`local.properties`의 `BASE_URL`, 예: `https://cafeminsu.duckdns.org/`).
  모든 API 경로는 `api/...`(예: `api/user/kakao-login`), 헬스체크만 `health`. Retrofit baseUrl은 루트(`/`).
- **HTTPS 강제 · cleartext 차단**(`SECURITY.md §2`). 키 부재(`BASE_URL` 공백) 시 해당 Repository는 Mock 폴백.

## 공통 응답 봉투 `BaseResponse<T>`
모든 응답은 `{ isSuccess: boolean, code: int, message: string, result: T }` 형태다.
- data 레이어에서 봉투를 **언랩**: `isSuccess==true` → `AppResult.Success(map(result))`,
  아니면 `AppResult.Failure(code/HTTP→DomainError)`. (스펙에 별도 에러 스키마는 없음 → `isSuccess`/`code` +
  HTTP 상태로 판정.) 예외→`DomainError` 매핑은 data 레이어에서만(`AppResult`/`DomainError` 재사용).

## 인증 (카카오 토큰 서버 교환, JWT Bearer)
- 글로벌 시큐리티 `bearerAuth`(HTTP Bearer JWT). `kakao-login`/`refresh`/`stores`/`menus`는 **public**,
  `profile`/`orders`/`payments`는 **Bearer 필요**.
- **로그인**: 카카오 SDK로 받은 **카카오 access token** → `POST api/user/kakao-login`
  `{ accessToken }` → `result {accessToken, refreshToken, isNewUser, nickname}`(앱 JWT 발급).
  → 기존 `RealKakaoLoginProvider`는 카카오 **accessToken**을 노출해야 한다(현재는 `AuthState`만 반환).
- **갱신**: `POST api/user/refresh`, 헤더 `Refresh-Token: <refreshToken>` → `result {accessToken}`.
  401 → refresh 1회 → 실패 시 `AuthState.Expired`.
- **프로필**: `GET api/user/profile`(Bearer) → `result {id, nickname, profileImageUrl, role:CUSTOMER|OWNER}`.
- **저장**: access/refresh JWT는 **EncryptedDataStore**(평문 금지), 로그아웃/만료 시 와이프(`SECURITY.md §1`).
- ⚠ **`userId` 파라미터 금지(스펙 stale)**: `docs/openapi.json`은 `orders/my`·`orders`·`orders/{id}`·
  `user/profile`·`payments/*`에 `userId`를 필수 query로 표기하지만, **서버는 이를 제거**하고 Bearer 토큰으로
  사용자를 식별한다. 클라이언트는 **userId를 보내지 않는다**. 인증 필요 호출 전 `ensureAuthenticated()`(Guest면
  `Unauthorized`)만 확인하고, 토큰은 OkHttp Authorization 인터셉터가 부착한다. (JWT `sub`=userId, `role`=role은
  토큰 클레임에 있으나 파라미터로 보낼 필요 없음.)

## 도메인 매핑 규칙 & 차이 (반드시 준수)
서버 모델은 도메인 모델(`DATA_MODEL.md`)과 일부 다르다. **매퍼(data 레이어)에서 흡수**한다.
도메인 모델/Repository 인터페이스 시그니처는 변경하지 않는다(UI 무변경 목표).

1. **ID 타입**: 서버는 숫자(int64), 도메인은 `String`. 매퍼에서 `Long.toString()` 왕복 변환.
2. **UserProfile**: 서버 `nickname`→도메인 `displayName`, `phoneLast4`는 미제공(null). `role`로 Customer/Owner.
3. **Store**: 목록(`StoreSearchItem{id,name,address,imageUrl}`/`NearbyStoreRes{...,distance}`)은 필드가 적고,
   상세(`GET api/stores/{id}` → `{address,latitude,longitude,phone,businessHours,imageUrl}`)에 더 있다.
   도메인 `Store`의 `status`/`amenities`/`closingTimeLabel`은 서버에 없음 → 기본값(예: `Open`/빈 목록/null)
   또는 `businessHours` 파생. `distance`는 nearby에서만.
4. **메뉴 카테고리**: **별도 카테고리 엔드포인트 없음.** `GET api/stores/{storeId}/menus`의
   `MenuListItemRes.category`(문자열)로 `observeCategories()`를 **distinct 파생**(sortOrder는 등장 순/이름순).
5. **메뉴 옵션**: 라이브 서버 `GET api/menus/{id}`의 `options`는 평면 `[{id,group,name,additionalPrice,isDefault}]`
   (OpenAPI 스펙의 `OptionRes{optionId,...}`와 필드명이 다름 → 메뉴 전용 `MenuOptionRes` DTO 사용).
   도메인 `MenuOptionGroup`(중첩)으로 **`group` 기준 그룹핑**. required/min/max 정보 없음 → 기본값.
   주문 응답(`ItemRes.options`)은 여전히 `OptionRes{optionId,optionGroup,optionName,optionPrice}`.
6. **장바구니**: **서버 카트 없음.** `CartRepository`는 로컬/Mock 유지(메뉴 정보는 활성 `MenuRepository`에서 해석 —
   서버 메뉴 id와 일치). 주문 생성 시 카트 → 주문 아이템 변환:
   `OrderCreateReq{ storeId, orderType:MOBILE|KIOSK, orderMethod:VOICE|MANUAL, items:[{menuId, quantity, optionIds:[]}] }`.
   - 모바일 주문은 `orderType=MOBILE`, 수동 담기는 `orderMethod=MANUAL`(음성주문은 VOICE).
   - 도메인 `OrderType{DineIn,Takeout}`는 **서버에 대응 필드 없음** → 주문 생성 시 사용하지 않거나
     `requestNote` 등으로만 보존(서버 전송 안 함). 화면 동작은 유지.
7. **주문 상태**: 서버 `PENDING/ACCEPTED/READY/DONE/CANCELLED` → 도메인 `OrderStatus` 매핑:
   PENDING→PendingPayment(결제 전)/Paid(결제 후 맥락), ACCEPTED→Accepted, READY→Ready, DONE→Completed,
   CANCELLED→Cancelled. (도메인 Preparing/Failed는 서버 미구분 → 가장 가까운 상태로.)
   생성 응답 `OrderCreateRes{orderId,orderNumber,totalAmount,status}`. `totalAmount`는 **서버 값**으로 확정.

## 결제 (PG 흐름 — prepare → PG SDK → verify)  ⚠ 결정/키 필요
서버 결제는 PG(아임포트/PortOne형) 2단계다. 단순 `pay()` 호출이 아니다.
- `POST api/payments/prepare {orderId, useGifticonId?, gifticonAmount?, cardAmount?}` → `{merchantUid, amount}`.
- **클라이언트가 PG SDK 실행**(결과 `impUid` 발급) — **별도 PG 제공자 SDK + 상점/채널 키 필요**.
- `POST api/payments/verify {impUid, merchantUid}` → `{paymentId, status:READY|PAID|FAILED|REFUNDED}`.
- `GET api/payments/{paymentId}` → 상태 확정 조회.
- 멱등: 서버는 **`merchantUid`** 로 식별(클라 Idempotency-Key 헤더 아님). 동일 주문 재시도는 같은 merchantUid.
- 도메인 매핑: `idempotencyKey ≈ merchantUid`, `paymentMethodToken ≈ impUid`, status PAID→Approved 등.
- **낙관 금지**: verify 응답/`payment detail` 확정 전 성공 화면 전환 금지(`ARCHITECTURE.md`). 카드 PAN/CVC
  미저장·미로깅(`SECURITY.md §3`), PG 토큰만.
- ⚠ **PG 제공자 SDK·키 부재 시 결제 step은 `blocked`** 또는 PG 호출부를 인터페이스 뒤로 추상화해 Mock
  impUid로 테스트만 통과시키고 실 PG는 후속으로 둔다(이 phase의 결제 접근은 step4 참고).

## 리워드 / 선물 / 알림 (phase 17 — 나머지 도메인 연동)
phase 16(인증·매장/메뉴·주문·결제)에 이어, **고객 측 리워드 도메인**을 실서버에 붙인다. 모두 기존
카카오 Bearer JWT(인터셉터 자동 부착)로 동작하며 `userId` 등 식별 파라미터는 보내지 않는다. DI 는
`BASE_URL` 키 게이트(`select*Repository`) 폴백을 따른다.

- **리워드(`RewardRepository`)**: `GET api/stamps`(내 스탬프) / `GET api/stamps/{storeId}`(매장 스탬프) →
  `StampCard`. 서버 스탬프는 **매장별**일 수 있으므로 선택 매장(`SelectedStoreHolder`) 있으면 그 매장,
  없으면 목록 대표(합산/첫 매장)로 매핑. `grantStampsForPaidOrder` 는 **서버 자동 적립**이라 별도 grant
  호출 없이 스탬프 **재조회**로 갱신. 기프티콘은 `GET api/gifticons/my`·`/{id}`·`POST /{id}/use`.
  ⚠ 바코드/QR 등 민감값 **미로깅·미복사**(`SECURITY.md §3`).
- **선물(`GiftRepository.sendGift`)**: `POST api/gifticons`(구매) → `POST api/gifticons/{id}/share`(선물 링크)
  2단계. 중간 실패 시 다음 단계 진행 금지(낙관 금지). 채널/수신자/메시지는 공유 스키마에 맞게 매핑.
- **알림(`NotificationRepository`)**: `GET api/notifications` → `List<AppNotification>`,
  `PATCH api/notifications/read-all` → 전체 읽음. 타입 문자열 → `NotificationType` 매핑, 불가 타입은 흡수/제외.
  도메인 계약에 없는 `unread-count`/개별 읽음(`{id}/read`)은 구현하지 않음.

## 범위 밖 (Mock 유지 — 사유 명시)
다음은 엔드포인트 유무·구조 차이로 이번 슬라이스에서 **연동하지 않고 기존 Mock 을 유지**한다(별도 결정/phase 대상):
- **점주(owner) 전체**(`OwnerOrderRepository`/`OwnerMenuRepository` 메뉴 노출·`stores/my`·`orders` 점주):
  앱의 점주 로그인은 **아이디/비번 Mock**(`OwnerAuthProvider`)인데 서버에는 아이디/비번 점주 로그인이 없고
  점주 식별은 **카카오 JWT `role=OWNER` + `api/user/become-owner` + `api/stores/my`** 다. 연동하려면 점주
  인증을 카카오 역할 기반으로 **재설계**해야 하므로(서버 영향 포함) 별도 결정 필요. 또한 메뉴 가시성(`setVisible`)은
  서버 엔드포인트 없음(판매토글 `PATCH api/menus/{menuId}/availability` 만 존재).
- **매출/정산(`SalesRepository`)**: 도메인 `SalesSummary`(합계·요일별·인기메뉴·정산)는 집계가 필요하나 서버는
  원시 결제 목록(`GET api/stores/{storeId}/payments`)만 제공 — 집계 엔드포인트 없음 → 과도한 클라 집계 회피 위해 Mock 유지.
- **쿠폰(`CouponRepository`)**: 서버에 쿠폰 엔드포인트 **자체가 없음** → Mock 유지.
- **추천(`api/stores/{storeId}/recommendations/today`)**: 홈의 "오늘의 추천"은 현재 메뉴 목록에서 **클라 파생**이고
  전용 Repository 가 없다. 연동 시 신규 Repository + UI 배선이 필요(기능 추가)하므로 repo 교체 슬라이스 범위 밖.
- **음성 주문 파싱(`api/orders/voice`)**: 온디바이스 규칙 파서 유지(클라우드 STT/LLM 미착수).
