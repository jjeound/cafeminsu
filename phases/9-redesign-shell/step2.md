# Step 2 — 홈 화면 디자인 재작업 (TDD)

> 첨부된 `홈.png` + `docs/SCREENS.md`(HOME)를 그대로 따른다. 기존 `ui/feature/home`을 디자인에 맞게 재작업한다.

## 만들 것 / 바꿀 것 — `ui/feature/home/`
`docs/SCREENS.md` HOME 레이아웃을 정확히 구현(위→아래):
- 그리팅 "안녕하세요, {이름}님" `h1` + "오늘도 잘 부탁드려요" `muted`. 우상단 **알림 벨**(미읽음 빨간 점) → `NOTI`.
- **오늘의 추천 메뉴** 다크 product 카드(`surface-dark`): "오늘의 추천 메뉴" 라벨 + "🔥 인기" 태그,
  썸네일 + 메뉴명/설명, 가격 `primary` + 취소선 원가 `muted`, 폭 꽉 찬 "지금 주문하기 ›" 버튼.
- **사용 가능 쿠폰** 카드: "사용 가능 쿠폰 N장" + "1잔 무료 쿠폰 · 오늘 만료", `›` → `COUPON`.
- **다시 주문하기** 섹션(헤더 + "전체보기"): 2열 카드(메뉴명/옵션요약/시점/"가격 · 재주문" pill). 탭 → 재주문.

## 데이터 (기존 리포 재사용)
- 추천/최근 메뉴: `MenuRepository`. 최근 주문(다시 주문하기): `OrderRepository.observeOrderHistory()`.
- 스탬프/등급: `RewardRepository`(`StampCard`). **쿠폰 개수**: 정식 `CouponRepository`는 phase 12 — 지금은
  `RewardRepository.observeGifticons()`의 Available 개수 등 기존 소스로 카운트(임시). 세션 이름: `SessionRepository`.
- `HomeViewModel`: 위 Flow 결합 → `HomeUiState`(Loading/Content/Empty/Error). `Failure`→Error, 예외 전파 금지.
- `HomeScreen` stateless + `HomeRoute`(hiltViewModel + 네비 콜백: 추천 주문→주문 플로우, 쿠폰→COUPON, 벨→NOTI, 재주문).

## ⚠ TDD — ViewModel 테스트 먼저
- 리포 데이터 → 추천/쿠폰수/다시주문/그리팅이 Content에 매핑. `Failure`→Error. 빈 최근주문 처리.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 중앙 정렬 금지. 안티슬롭 금지.
- 알림 화면 본구현은 step 3(여기선 벨 → NOTI 라우트 연결만). 주문 플로우 화면 변경 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` 성공. 직접 실행해 확인하라.
- 홈이 `홈.png` 구조(그리팅/추천 다크카드/쿠폰/다시주문/벨)와 일치한다.
- 통과하면 `phases/9-redesign-shell/index.json`의 step 2 status를 `completed` + `summary` 기록.
