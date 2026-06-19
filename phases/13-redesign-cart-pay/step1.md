# Step 1 — 결제 화면 재설계 (PAY, TDD)

> 첨부된 `주문 - 07(결제).png` + `docs/SCREENS.md`(PAY)를 그대로 따른다.
> **배경**: 기존 `ui/feature/payment/PaymentScreen.kt`(phase 5)는 옛 디자인이다. 디자인에 맞게 재작업한다. **Mock PG·멱등키·낙관금지·PAN/CVC 미저장** 보안 규칙과 결제 도메인 로직은 그대로 유지·재사용.

## 바꿀 것 — `ui/feature/payment/`
`docs/SCREENS.md` PAY 레이아웃으로 재작업:
- `CafeTopBar`: 좌 `‹`, 중앙 "결제".
- **정보 배너**(`accent-soft`/`muted`): "ⓘ PG 미연동 — Mock 성공/실패 분기로 대체".
- "결제 수단" 라벨 + 칩/3분할 [신용카드(선택=`surface-dark`/`on-dark`)] [간편결제] [쿠폰].
- "주문 요약" 카드(`surface-card`): 항목별 "메뉴 (옵션) ✕ N ... 금액", 구분선, **"총 결제 금액" ... 금액** `ink` 강조.
- 하단 2버튼(Mock): **"결제 실패"**(secondary) → `ORDER_FAIL` · **"결제 성공"**(`primary`) → `ORDER_OK`.

## 데이터 / 보안 (유지·재사용)
- 기존 결제 ViewModel/UiState/Mock PG·**멱등키**·스탬프 적립 로직 재사용. **카드 PAN/CVC 미저장·미로깅, 낙관적 UI 금지, Unknown→성공 처리 금지**(`SECURITY.md`/`ARCHITECTURE.md`). 처리 중 버튼 비활성·중복 가드.

## ⚠ TDD — 기존 결제 테스트 유지
- 성공→ORDER_OK·스탬프 적립, 실패→ORDER_FAIL, 멱등키 재사용, 처리 중 중복 차단(Turbine). 기존 테스트가 구조 변경으로 깨지면 새 구조로 갱신(보안 단언은 유지).

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 결제 보안 규칙 약화 금지.
- 주문 성공/실패 화면 재작업은 step 2 — 분기 라우팅만 연결.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 결제가 `주문 - 07`.png 구조(정보 배너·결제 수단·주문 요약·실패/성공 2버튼)와 일치하고, 성공/실패 분기·멱등·낙관금지가 유지된다.
- 통과하면 `phases/13-redesign-cart-pay/index.json`의 step 1 status를 `completed` + `summary` 기록.
