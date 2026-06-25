# Step 1: owner-store-selector (점주 대시보드 매장명 실연동 + 선택)

점주 대시보드 헤더의 매장명이 **하드코딩**(`OwnerHomeViewModel.DefaultOwnerProfile.storeName = "강남점"`)이고,
`"$storeName ▾"` 드롭다운 표식만 있을 뿐 선택 동작이 없다. 실제 점주 매장 목록(`GET api/stores/my` →
`{id, name, imageUrl}`, 이미 `OwnerOrderApi.getMyStores()` 존재)으로 **실제 매장명 표시** + **매장 선택**을
가능하게 한다. 선택은 대시보드 주문/통계에 반영한다.

## 읽어야 할 파일

- `app/src/main/java/com/cafeminsu/data/remote/OwnerOrderApi.kt` (`getMyStores(): List<MyStoreRes>`)
- `app/src/main/java/com/cafeminsu/domain/repository/OwnerOrderRepository.kt`
- `app/src/main/java/com/cafeminsu/data/repository/RealOwnerOrderRepository.kt` (`resolveStoreId()` = stores/my 첫 매장)
- `app/src/main/java/com/cafeminsu/data/repository/RealOwnerMenuRepository.kt` (`resolveStoreId()` 동일 패턴)
- `app/src/main/java/com/cafeminsu/data/repository/MockOwnerOrderRepository.kt`
- `app/src/main/java/com/cafeminsu/ui/feature/owner/home/OwnerHomeViewModel.kt` (하드코딩 `DefaultOwnerProfile`)
- `app/src/main/java/com/cafeminsu/ui/feature/owner/home/OwnerHomeUiState.kt`
- `app/src/main/java/com/cafeminsu/ui/feature/owner/home/OwnerHomeScreen.kt` (헤더 `"$storeName ▾"`)
- `app/src/main/java/com/cafeminsu/domain/auth/OwnerAuthProvider.kt` · `domain/model/Session.kt`(`OwnerProfile`)
- `app/src/test/java/com/cafeminsu/data/repository/RealOwnerOrderRepositoryTest.kt`
- `/docs/UI_GUIDE.md` · `/docs/DESIGN_SYSTEM.md` (드롭다운/칩 — 토큰만, 안티-AI슬롭)
- (참고) `app/src/main/java/com/cafeminsu/data/datastore/` 의 선택 매장 holder 가 이미 있으면 재사용(중복 정의 금지)

## 작업

1. **도메인 모델 + 매장 목록 노출**:
   - `domain/model/` 에 `OwnerStore(id: String, name: String)` 추가(서버 id Long → String 매퍼 변환).
   - `OwnerOrderRepository` 에 `suspend fun getStores(): AppResult<List<OwnerStore>>` 추가.
   - `RealOwnerOrderRepository`: `getMyStores()` → `List<OwnerStore>` 매핑 구현. `MockOwnerOrderRepository`: 더미 매장(예: 강남점/판교점) 반환.

2. **선택 매장 공유 상태**:
   - 싱글톤 `SelectedOwnerStoreHolder`(`data/repository/` 또는 기존 datastore holder 재사용): `StateFlow<String?> selectedStoreId` + `select(id)`.
     - 이미 `20-local-persistence` 의 선택 매장 holder 가 있으면 **그것을 재사용**(점주용 키 추가). 없으면 신규 인메모리 싱글톤.
   - `RealOwnerOrderRepository.resolveStoreId()`·`RealOwnerMenuRepository.resolveStoreId()`: holder 의 선택 id 사용, 없으면 stores/my **첫 매장**(기존 동작 보존).
   - `observeIncomingOrders`: 선택 변경 시 **재로드**되도록 holder 흐름과 결합(캐시 무효화 — 선택 id 가 바뀌면 cachedOrders 초기화 후 재조회).

3. **OwnerHomeViewModel**:
   - 하드코딩 `DefaultOwnerProfile.storeName = "강남점"` 제거. 초기화 시 `getStores()` 조회 → 실제 매장 목록/선택 매장명을 상태로.
   - `OwnerHomeUiState`(Content/Empty)에 `stores: List<OwnerStore>` 와 `selectedStoreId`(또는 선택 매장명) 노출. `onSelectStore(storeId)` → holder.select + 새로고침.
   - 매장 1개면 선택 UI 없이 이름만, ≥2개면 드롭다운.

4. **OwnerHomeScreen 헤더**:
   - `"$storeName ▾"` 를 실제 매장 드롭다운(예: `DropdownMenu`)으로 연결(매장 ≥2개일 때). 선택 시 `onSelectStore` 호출.
   - 토큰만(hex 금지), 한국어, 안티-AI슬롭.

### 핵심 규칙 (반드시 준수)

- **무회귀**: stores/my 가 비어 있으면(테스트 계정) 기존처럼 빈 목록·안전 폴백 유지. 매장 1개면 기존과 동일하게 동작.
- **AppResult 래핑**: 모든 서버 호출 `runCatchingToAppResult`. 예외 비전파.
- **레이어**: 선택 상태는 data/도메인 레이어 holder, UI 는 ViewModel 경유로만 접근.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## 검증 절차

1. **테스트 우선(TDD)**:
   - `RealOwnerOrderRepositoryTest`: `getStores()` 매핑, 선택 매장 변경 시 `getStoreOrders` 가 선택 storeId 로 호출되는지(MockWebServer 경로 검증) 추가.
   - `OwnerHomeViewModelTest`(없으면 신설): 매장 목록 노출, `onSelectStore` 후 매장명/주문 갱신 검증(Turbine).
2. 위 AC 통과. 기존 점주 홈/주문 테스트 **무회귀**.
3. 결과에 따라 `phases/28-owner-pay-fixes/index.json` 의 step 1 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "OwnerStore + OwnerOrderRepository.getStores(stores/my 매핑) + SelectedOwnerStoreHolder 로 점주 대시보드 매장명 실연동·선택(선택 시 주문/통계 재조회), 하드코딩 강남점 제거"`
   - 3회 실패 → `"status": "error"`, `"error_message": "<에러>"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "<사유>"` 후 중단

## 금지사항

- `OwnerOrderRepository`/도메인 시그니처를 UI 가 깨지도록 바꾸지 마라(추가만, 기존 보존).
- 매장명을 다시 하드코딩하지 마라. hex 색 리터럴 금지(토큰만). 기존 테스트를 깨뜨리지 마라.
