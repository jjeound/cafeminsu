# Step 2 — 주문 성공 / 주문 실패 화면 재설계 (ORDER_OK / ORDER_FAIL, TDD)

> 첨부된 `주문 - 08 (주문 성공).png`(ORDER_OK) + `주문 - 09 (주문 실패).png`(ORDER_FAIL) + `docs/SCREENS.md`(ORDER_OK, ORDER_FAIL)를 그대로 따른다.
> **배경**: 기존 주문 성공/실패 표현(phase 4~5)을 디자인에 맞는 전용 화면/다이얼로그로 재작업한다.

## 바꿀 것
1. **ORDER_OK** — `ui/feature/order/`(또는 기존 위치): 풀스크린(`canvas`), 우상단 `✕`.
   중앙 **코랄 원 + 체크**(`primary`/`on-primary`), **"주문이 완료됐어요"** `display` + 안내 `muted`.
   **다크 요약 카드**(`surface-dark`): "주문 번호" + 번호 `h1`/`on-dark`, 픽업 매장·예상 완성·결제 금액 행(라벨 `muted`/값 `on-dark` 우측 정렬).
   **스탬프 적립 배너**(`surface-card`): "☆ 스탬프 1개가 적립됐어요 (N/10)". 하단 **"주문 상태 보기"**(`primary`)→`HISTORY` · **"홈으로 이동"**(secondary)→`HOME`.
2. **ORDER_FAIL** — 스크림 + 중앙 **다이얼로그 카드**(`canvas`): 상단 **빨간 원 + ✕**(`error`),
   "결제에 실패했어요" `h2` + 사유 `muted`, 에러코드 칩(`surface-card`/`muted`), 버튼 [취소(secondary)] [다시 시도(`primary`, **같은 멱등키 재시도**)].

## 데이터 (기존 재사용)
- 주문번호/매장/예상시간/금액/적립 스탬프는 기존 주문·스탬프 도메인에서 가져온다(없으면 요약 모델 보강). 실패 사유/에러코드는 결제 결과에서 매핑.
- "다시 시도"는 결제 step의 멱등키를 재사용(중복 결제 방지).

## ⚠ TDD — 매핑 로직 테스트
- 성공 요약(번호/금액/스탬프) 매핑, 실패 사유→에러코드/메시지 매핑, "다시 시도" 멱등키 재사용(Turbine/단위). 기존 테스트는 새 구조로 갱신.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. (성공/실패 중앙 정렬은 결과 화면 예외 허용.)
- 결제 보안·멱등 로직 약화 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 성공/실패가 `주문 - 08/09`.png 구조와 일치하고, 성공→주문상태/홈, 실패→다시 시도(멱등) 흐름이 동작한다.
- 통과하면 `phases/13-redesign-cart-pay/index.json`의 step 2 status를 `completed` + `summary` 기록.
