# Step 0 — 마이페이지 재설계 (MY, TDD)

> 첨부된 `MY - 01.png` + `docs/SCREENS.md`(MY)를 그대로 따른다.
> **배경**: 기존 `ui/feature/my/MyScreen.kt`(phase 7)는 화면 PNG 확정 전 옛 디자인(제목 "마이페이지", 라이트 프로필 카드)이다. 디자인에 맞게 재작업한다. 보호 화면(NeedsLogin)·로그아웃 와이프 로직은 유지.

## 바꿀 것 — `ui/feature/my/`
`docs/SCREENS.md` MY 레이아웃으로 재작업:
- 헤더 "MY" `h1` + 우상단 **설정 기어 아이콘**.
- **프로필 다크 카드**(`surface-dark`): 좌측 원형 아바타(이니셜, `primary`/`on-primary`), 이름 "{이름} 님" `h2`/`on-dark` +
  등급 배지 "GOLD"(`accent-soft`/`primary` pill). 하단 3분할 stats(세로 구분선): "{주문수} / 주문", "{스탬프}/10 / 스탬프", "{쿠폰} / 쿠폰".
- **빠른 메뉴** `h2`: 4개 아이콘 버튼(`surface-card`): 주문내역 / 선물하기 / 쿠폰 / 알림설정 (아이콘 `primary`, 라벨 `caption`).
  → 각각 `HISTORY` / `GIFT` / `COUPON` / (알림설정은 스텁/토스트) 라우트.
- **설정 리스트**(행 + `›`, `hairline` 구분): 이용 약관 / 자주 묻는 질문 / 고객센터(우 "1588-1234") / 버전 정보(우 "v1.0.0") /
  **로그아웃**(`primary` 텍스트) → 로그아웃 확인 다이얼로그 → `clearSession()` 와이프 → `LOGIN`.
- 하단 탭바(MY 활성).

## 데이터 (기존 재사용)
- 프로필/등급/이름: `SessionRepository`. 주문수: `OrderRepository`. 스탬프: `RewardRepository`(`StampCard`). 쿠폰 수: 기존 소스(임시).
- `MyViewModel`에 stats(주문/스탬프/쿠폰) 결합 추가(TDD). 보호 화면(NeedsLogin) 유지. 로그아웃 시 세션·민감데이터 와이프 의미 유지.

## ⚠ TDD — ViewModel 테스트 먼저(추가/변경분)
- 프로필·등급·3 stats 매핑, 로그아웃→세션 와이프, NeedsLogin 분기(Turbine). 기존 My 테스트는 새 구조로 갱신.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 안티슬롭 금지.
- 쿠폰/선물/주문내역 화면 재작업은 step 1~2 — 여기선 빠른 메뉴 라우트 연결만.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- MY가 `MY - 01`.png 구조(다크 프로필 카드·3 stats·빠른 메뉴 4타일·설정 리스트·로그아웃)와 일치한다.
- 통과하면 `phases/14-redesign-my/index.json`의 step 0 status를 `completed` + `summary` 기록.
