# Step 4 — 오늘의 추천 실서버 연동 (RecommendationRepository + 홈 배선)

## 배경
홈(M-01)의 "오늘의 추천"은 현재 전용 Repository 가 없고 `HomeViewModel` 이 **메뉴 목록에서 클라 파생**한다
(`menus.firstOrNull { !it.isSoldOut }.toHomeRecommendedMenu()`). 서버에는
`GET api/stores/{storeId}/recommendations/today` 가 있다. 이 step 은 추천 전용 도메인 Repository 를 신설해
실연동하고, 홈에서 그것을 사용하되 **실패/빈 결과 시 기존 메뉴 파생으로 폴백**해 UI 동작을 유지한다.
`BASE_URL` 키게이트로 Mock 폴백을 둔다. (가장 리스크 큰 step — 신규 인터페이스+UI 배선.)

## ⚠ 스키마 주의 (반드시 먼저 확인)
`docs/openapi.json` 의 `TodayRecommendationRes` 는 stale(제네릭 `Item{menuId,quantity,optionIds}` 참조)이다.
**구현 전에 라이브 스펙으로 실제 형태 확인**:
```
curl -s "$BASE_URL/v3/api-docs" | python3 -m json.tool
```
`recommendations` 항목의 실제 필드를 확인한다. 추천이 단순 `menuId` 목록이면 **메뉴 상세/목록을 조회해
표시 정보를 보강**한다(선택 매장의 `listByStore` 재사용). 실제 형태가 표시에 쓸 수 없을 만큼 빈약하면(예: id 만
있고 메뉴 해석 경로도 불명) `blocked` + `blocked_reason` 기록 후 중단.

## 작업 범위 (이 step에서만)
1. **도메인**: `domain/repository/RecommendationRepository.kt` 신설 —
   `fun observeTodayRecommendation(): Flow<AppResult<MenuItem?>>` (또는 홈이 기대하는 최소 모델). 기존 도메인 모델
   `MenuItem` 재사용 권장 — **새 도메인 모델은 꼭 필요할 때만** 추가하고 최소화한다.
2. **API**: `RecommendationApi.kt` — `getTodayRecommendation(storeId): TodayRecommendationRes`(DTO 는 라이브 스펙 기준).
   추천이 매장 종속이므로 `SelectedStoreHolder` 로 선택 매장 id 사용(없으면 빈 결과). NetworkModule provider 추가.
3. **매퍼/Repository**: `RealRecommendationRepository` — 추천 응답 → `MenuItem`(필요시 메뉴 조회 보강).
   `MockRecommendationRepository` — 기존 Mock 메뉴에서 대표 1건 반환(현 동작과 유사). `runCatchingToAppResult` 사용.
4. **DI 키게이트**: `RepositoryModule.kt` 에 `provideRecommendationRepository` + `selectRecommendationRepository` 추가.
5. **홈 배선(최소 변경)**: `HomeViewModel` 이 `RecommendationRepository` 를 주입받아 추천을 사용하되,
   **실패/빈/미선택매장 시 기존 `menus` 파생 로직으로 폴백**(현재 화면 동작·테스트 보존). `HomeUiState`/`HomeScreen` 의
   공개 형태는 가능한 한 유지(불가피한 경우 최소 변경). 기존 `HomeViewModelTest` 가 깨지지 않게 한다.
6. **테스트(먼저 작성)**: `RealRecommendationRepositoryTest.kt`(MockWebServer) + `HomeViewModel` 폴백/성공 경로 테스트.

## 금지 / 불변
- 추천 외 다른 도메인/이전 step 산출물 변경 금지. 신규 모델·필드는 최소화(불필요한 기능 추가 금지).
- 홈의 기존 UX(추천 카드 표시, 빈 상태 문구 "추천할 메뉴가 아직 없어요")는 유지. 기존 테스트 보존.
- 보안/포맷 규칙 유지. 외부 호출은 `AppResult` 로 감싼다. 라이브 스펙 확인 없이 stale 스키마 추측 구현 금지.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 4 를 `completed` + `summary` 로 갱신·커밋. 구현 불가(스키마/배선) 시 `blocked` + `blocked_reason` 후 중단.
