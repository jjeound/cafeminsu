# Step 5 — 점주 대시보드 헤더에서 여러 매장 중 하나 선택

## 배경 / 요구
점주 대시보드(`ui/feature/owner/home/OwnerHomeScreen.kt`)의 헤더는 이미 `"$storeName ▾"` 형태로 **드롭다운
표식(▾)** 을 보여 주지만, 실제 매장 전환은 없다. `domain/...OwnerProfile`(id, storeId, storeName, loginId,
isStoreOpen)은 **단일 매장**만 담는다. 요구: 점주가 **자신의 여러 매장 중 하나를 헤더에서 선택**해 대시보드가
그 매장 기준으로 동작하게 한다.

## 작업 범위 (이 step에서만)
1. **점주 매장 목록 소스**: 로그인한 점주의 매장 목록을 제공한다. `OwnerAuthProvider`(또는 적절한 점주 리포지토리)
   에 매장 목록을 노출한다. 실서버 다중매장 API가 없으면 **Mock 다중 매장(2~3개)** 으로 제공하고 실연동은 후속
   과제로 남긴다(요구의 핵심은 선택 UX 동작).
2. **헤더 매장 선택 드롭다운**: `OwnerHomeScreen` 헤더의 `"$storeName ▾"` 를 탭하면 Material3 `DropdownMenu`
   로 매장 목록을 띄우고, 선택 시 활성 매장을 전환한다. 전환은 대시보드의 매장 종속 데이터(헤더 매장명,
   영업 토글 `isStoreOpen`, 들어오는 주문 등)에 반영된다.
3. **상태/지속성**: 선택 매장을 `OwnerHomeViewModel` 상태로 관리(필요 시 phase 20 의
   `UserPreferencesDataStore` 의 선택-매장 패턴을 참고해 영속). 매장이 1개뿐이면 종전처럼 단순 표시(드롭다운
   비활성/없음).
4. 점주 주문/메뉴/매출 화면은 깨지지 않게 유지(이 step은 대시보드 헤더 매장 선택까지).

## 테스트 (먼저 작성)
- `OwnerHomeViewModelTest` 에 매장 목록 노출·매장 전환 시 상태(헤더 매장명 등) 갱신 검증.
- 가능하면 `OwnerHomeScreen`(androidTest)에 드롭다운 표시/선택 케이스 추가(최소 컴파일 유지).

## 금지 / 불변
- 색·치수·타이포 토큰만, hex 금지, 한국어 카피.
- 점주 주문/메뉴/매출 기존 동작 불변.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 5 를 `completed` + `summary` 로 갱신·커밋.
