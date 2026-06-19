# Step 2 — 주문내역 재설계 (HISTORY, TDD)

> 첨부된 `MY - 04 (주문내역).png` + `MY - 05 (주문내역 empty).png` + `docs/SCREENS.md`(HISTORY)를 그대로 따른다.
> **배경**: 기존 `ui/feature/order/OrderStatusScreen.kt`(phase 4)를 디자인의 HISTORY로 재작업한다. 진행중 주문 상태 스텝퍼 + 지난 주문 + 재주문 + 빈 상태.

## 바꿀 것 — `ui/feature/history/`(또는 기존 order 위치)
`docs/SCREENS.md` HISTORY 레이아웃으로 재작업:
- `CafeTopBar`: 좌 `‹`, 중앙 "주문내역".
- **진행중 주문 다크 카드**(`surface-dark`): "진행중인 주문" `muted` + 라이브 점(`success`), 주문번호 `h1`/`on-dark`,
  **단계 스텝퍼**(접수→수락→준비중→완료: 지난 단계 `primary` 채움+연결선, 현재 강조, 미도달 `muted`), 하단 품목 요약 + 금액.
- "지난 주문" `caption`. 지난 주문 카드(`surface-card`): 매장명 `h3` + 금액, 날짜 `caption`, 품목 요약 `body`, 폭 꽉 찬 **"↻ 재주문"**(secondary).
- **빈 상태**(`MY-05`): 중앙 영수증 아이콘(`accent-soft`/`primary`), "아직 주문 내역이 없어요" + "첫 번째 한 잔을 주문해보세요" `muted`.

## 데이터 (기존 재사용)
- 진행중/지난 주문: `OrderRepository.observeOrderHistory()` + 현재 주문 상태. 단계 스텝퍼는 기존 `OrderStatus`(접수/수락/준비중/완료) 매핑.
- `HistoryViewModel`: 진행중 1건 + 지난 목록 분리 → Content/Empty/Error. 재주문 → 장바구니/상세. `Failure`→Error.

## ⚠ TDD — ViewModel 테스트 먼저
- 진행중/지난 주문 분리, OrderStatus→스텝퍼 단계 매핑, 빈 상태, 재주문 흐름(Turbine). 기존 OrderStatus 테스트는 새 구조로 갱신.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. (빈 상태 중앙 정렬 예외 허용.)

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 주문내역이 `MY - 04`(진행중 스텝퍼·지난 주문·재주문) / `MY - 05`(빈 상태) 구조와 일치한다.
- 통과하면 `phases/14-redesign-my/index.json`의 step 2 status를 `completed` + `summary` 기록.
