# Step 1 — 기프티콘 (M-09, MVVM + TDD, 보호 화면) — 민감 화면 보안

`PRD.md` M-09(보유 목록 / 상세 / 사용(바코드·QR))을 구현한다. 플레이스홀더 `ui/feature/gifticon/GifticonScreen`을 채운다.
`RewardRepository`(observeGifticons / getGifticon / markGifticonUsed) 사용. **민감 화면 보안 규칙**(아래)을 지킨다.

## 보안 (DATA_MODEL · SECURITY §4 엄수)
- 기프티콘 **바코드/QR 값을 로그에 남기지 않는다**. 클립보드 **자동 복사 금지**.
- 사용 화면(바코드/QR 노출)은 캡처 노출에 유의(필요 시 해당 화면 한정 `FLAG_SECURE` 고려, 과용 금지).
- 상태 표시: `Available`/`Used`/`Expired`, 곧 만료는 `warning` 토큰.

## 보호 화면
- M-09도 보호 화면 → `Guest`/`Expired`면 NeedsLogin 상태(step 0과 동일 패턴 재사용).

## 네비게이션
- 목록(`Routes.GIFTICON` `m09`)과 상세를 분리: 상세는 `gifticonId` 인자 라우트(예: `"m09/{gifticonId}"`).
  목록 항목 클릭 → 상세. `AppNavHost`에 등록.

## 패턴 / 만들 것 — `ui/feature/gifticon/`
- `GifticonViewModel`(목록): `observeGifticons` → `GifticonListUiState`(Content/Empty/Error/NeedsLogin).
- `GifticonDetailViewModel`(`SavedStateHandle`의 `gifticonId`): `getGifticon` → 상세, `onUse()` →
  `markGifticonUsed(id)` 후 상태 반영(사용 완료). `Available`만 사용 가능, `Used`/`Expired`는 사용 비활성.
- `GifticonScreen.kt`(목록) — `CafeCard` 목록(제목 `h3`, 만료일 `caption`, 상태 배지: Available/Used/Expired,
  곧 만료 `warning`). 빈 목록 `EmptyView`.
- `GifticonDetailScreen.kt`(상세/사용) — 제목/만료/상태 + **바코드/QR 표시 영역**(실제 바코드 렌더는 단순
  표현으로 충분 — 값 텍스트는 노출하되 로그/클립보드 금지). 하단 `CafeButton`("사용하기", Available일 때만 활성).
  사용 후 상태 갱신 + 안내. 토큰/컴포넌트만.

## ⚠ TDD — ViewModel 테스트 먼저
- `GifticonViewModelTest.kt`: 목록 Content/Empty/Error/NeedsLogin.
- `GifticonDetailViewModelTest.kt`: 상세 로드, `onUse()`가 `markGifticonUsed`를 호출하고 상태가 `Used`로,
  `Used`/`Expired`는 사용 불가, 없는 id는 Error.

## 하지 말 것
- 바코드/QR 값 로깅·클립보드 자동복사 금지. 마이(M-10)·음성 구현 금지. hex/새 토큰 금지. 카피 한국어.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(Gifticon 테스트 포함). `./gradlew :app:assembleDebug` + `:app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 기프티콘 바코드/QR 값이 로그/클립보드로 노출되지 않는다(코드 확인).
- 통과하면 `phases/6-stamp-gifticon/index.json`의 step 1 status를 `completed` + `summary` 기록.
