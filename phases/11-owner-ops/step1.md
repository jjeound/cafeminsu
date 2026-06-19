# Step 1 — 점주 매출·정산 (OWNER_SALES, MVVM + TDD)

> 첨부된 `점주 - 04 (매출·정산).png` + `docs/SCREENS.md`(OWNER_SALES)를 그대로 따른다. phase 10 step 0의 OWNER_SALES 플레이스홀더를 본구현으로 교체한다.
> 매출 모델·계약은 `docs/DATA_MODEL.md`(SalesPeriod/TopMenu/SalesSummary, SalesRepository)가 단일 진실.

## 만들 것 — `ui/feature/owner/sales/`
`docs/SCREENS.md` OWNER_SALES 레이아웃을 정확히 구현(위→아래):
- 상단 "매출 · 정산" `h1`.
- **기간 세그먼트**(`surface-card` pill 3분할): 오늘 · 이번 주(기본 선택=흰 배경/`ink`) · 이번 달 → `SalesPeriod`. 선택 시 데이터 갱신.
- "이번 주 매출" `caption`/`muted` + 큰 숫자 **"₩2,840,000"** `display`/`primary` + **"▲ 12% 지난주 대비"** `caption`/`success`(증감 부호색, 음수는 하강·`error` 톤).
- **"요일별 매출"** 카드(`surface-card`): 막대 차트(일~토, `dailySales`). 최고/당일 막대 `primary`, 나머지 `accent-soft`. 축 라벨 `meta`/`muted`. (Compose Canvas/Box 비율 막대, 외부 차트 라이브러리 금지.)
- **"인기 메뉴"** `h2`. 순위 행: 순위 숫자 `primary`, 메뉴명 `h3` + "{n}잔" `caption`/`muted`, 우측 금액 `bodyL`/`ink`.
- **"정산 예정 금액"** 다크 카드(`surface-dark`): 좌측 라벨 `muted` + "{payoutDateLabel}" `caption`, 우측 금액 `h2`/`on-dark`.

## 데이터 (도메인 계약)
- **`SalesRepository`**(`DATA_MODEL.md`) + `MockSalesRepository`(data, 인메모리 시드 + `@Binds`): `observeSales(period): Flow<AppResult<SalesSummary>>`.
  `SalesSummary`(totalSales, orderCount, deltaPercent, dailySales, topMenus, payoutAmount, payoutDateLabel) + `SalesPeriod`/`TopMenu` 모델 추가.
- `OwnerSalesViewModel`: 선택 기간(StateFlow) → `observeSales` 결합 → `OwnerSalesUiState`(Loading/Content/Empty/Error). `Failure`→Error, 예외 전파 금지.
- `OwnerSalesScreen` stateless + `OwnerSalesRoute`(hiltViewModel).

## ⚠ TDD — ViewModel 테스트 먼저
- 기간 전환 시 요약 갱신, dailySales→막대 비율/최고값 표시 로직, 증감 부호 처리, 인기 메뉴 순위 매핑, Empty/Error(Turbine).

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 금액 포맷 일관(₩/원). PII/민감값 로깅 금지.
- 외부 차트 라이브러리 추가 금지(Compose 기본 도형). 정산 "입금/이체" 실기능은 범위 밖(표시만).

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 매출·정산이 `점주 - 04`.png 구조(기간 세그먼트·총매출·요일별 막대·인기 메뉴·정산 예정)와 일치하고, 기간 전환이 동작한다.
- 통과하면 `phases/11-owner-ops/index.json`의 step 1 status를 `completed` + `summary` 기록.
