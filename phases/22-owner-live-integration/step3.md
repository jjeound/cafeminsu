# Step 3 — 매출/정산 실서버 연동 (RealSalesRepository, 클라 집계)

## 배경
매출/정산(`SalesRepository`)은 현재 `MockSalesRepository` 만 있다. 서버는 **집계 엔드포인트가 없고**
원시 결제 목록 `GET api/stores/{storeId}/payments` 만 제공한다(`SERVER_INTEGRATION.md` "범위 밖" 참조).
이 step 은 원시 결제 목록을 받아 **클라이언트에서 `SalesSummary` 로 집계**해 실연동하고 `BASE_URL`
키게이트로 Mock 폴백을 둔다. 도메인 모델/인터페이스/UI 는 변경하지 않는다.

## ⚠ 스키마 주의 (반드시 먼저 확인)
`docs/openapi.json` 덤프의 `StorePaymentsRes.payments` 는 제네릭 `Item{menuId,quantity,optionIds}` 를
참조하는 **stale 스키마**라 실제 결제 응답 형태로 신뢰할 수 없다. **구현 전에 라이브 스펙으로 실제 형태를
확인**하라:
```
curl -s "$BASE_URL/v3/api-docs" | python3 -m json.tool   # BASE_URL 은 local.properties 값
```
(공개 스펙이라 인증 불필요.) `StorePaymentsRes` 와 그 `payments` 항목의 **실제 필드**(예: 금액/일시/메뉴명/수량 등)를
확인해 DTO 를 정의한다. 라이브 스펙으로도 결제 항목에 **금액·일시 필드가 없어 `SalesSummary` 핵심값을 만들 수
없다면**, 억지 집계를 하지 말고 step 을 `blocked` + `blocked_reason`("payments 응답에 집계용 금액/일시 부재")
으로 기록하고 중단하라.

## 작업 범위 (집계 가능할 때)
1. **API**: `SalesApi.kt` 신설 — `getStorePayments(storeId, from?, to?): StorePaymentsRes`(인증 필요 Retrofit).
   step 0 의 `getMyStores()`/`MyStoreRes` 로 storeId 해석(재사용). DTO 는 **라이브 스펙 실제 형태**로 정의.
2. **집계 매퍼**: `SalesMapper.kt` — `StorePaymentsRes` + `SalesPeriod` → `SalesSummary`.
   - `period` 로 from/to 범위 계산(Today/Week/Month). `totalSales`=결제금액 합, `orderCount`=결제 건수,
     `topMenus`=메뉴별 판매수/매출 상위 N(가능한 필드로), `dailySales`=기간 내 일자 버킷 합(7칸 형식 유지).
   - 원시 데이터로 **산출 불가한 필드**(`deltaPercent`, `payoutAmount`, `payoutDateLabel`)는 도메인 nullable/0
     기본값(서버 미제공 사유 주석). 도메인 모델은 바꾸지 않는다.
3. **Repository**: `RealSalesRepository.observeSales(period)` — storeId 해석 → payments 조회 → 집계 emit.
   빈 stores/my/빈 결제 → 0/빈 `SalesSummary` 안전 처리. 외부 호출 `runCatchingToAppResult` 로 감싼다.
4. **DI 키게이트**: `RepositoryModule.kt` 에 `provideSalesRepository` + `selectSalesRepository` 추가,
   기존 `@Binds bindSalesRepository(MockSalesRepository)` 제거. `MockSalesRepository` 는 폴백 유지.
5. **테스트(먼저 작성)**: `RealSalesRepositoryTest.kt`(MockWebServer) — 결제 목록 픽스처 → 집계 결과
   (`totalSales`/`orderCount`/`topMenus`/`dailySales`) 단언, 빈 응답 → 0 집계, 비-2xx → Failure. 픽스처는
   라이브 스펙에서 확인한 실제 응답 모양으로 작성.

## 금지 / 불변
- 도메인 모델(`SalesSummary` 등)·`SalesRepository` 인터페이스·UI 변경 금지. `MockSalesRepository` 유지.
- 라이브 스펙 확인 없이 stale `Item` 스키마로 추측 구현 금지. 산출 불가 필드를 가짜 값으로 채우지 말 것.
- 보안/포맷 규칙 유지. 외부 호출은 `AppResult` 로 감싼다.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 3 을 `completed` + `summary` 로 갱신·커밋. 집계 불가(스키마 부재) 시 `blocked` + `blocked_reason` 후 중단.
