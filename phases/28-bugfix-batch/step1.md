# Step 1 — 읽은 알림은 목록에서 숨기기

## 배경 / 요구
알림 화면(`ui/feature/notification/`)은 진입 시 `NotiScreen.kt` 의 `LaunchedEffect` 에서
`viewModel.markAllRead()` 로 전부 읽음 처리하지만, `NotiViewModel.toUiState()` 는 **읽음 여부와 무관하게
모든 알림을 보여 준다.** 요구: **읽은 알림은 목록에 보이지 않게** 한다(한 번 보고 나면 다음 진입부터 숨김).

주의: 단순히 `read == true` 를 필터링하면, 진입 시 `markAllRead()` 가 즉시 전부 읽음 처리하므로 **보던 알림이
그 자리에서 사라진다.** 그래서 "이번 진입에서 보여 줄 알림"을 진입 시점에 고정해 두는 방식이 필요하다.

## 작업 범위 (이 step에서만)
1. **`NotiViewModel`**: 이번 구독(진입) 동안 노출할 알림 id 집합(`sessionVisibleIds`)을 둔다.
   - 업스트림 구독 시작 시 비운다(`observeNotifications().onStart { sessionVisibleIds.clear() }`).
   - 매 emission 마다 **아직 안 읽은(read==false) 알림의 id 를 집합에 누적**한다.
   - 화면에는 **id 가 집합에 든 알림만** 표시한다(그 결과가 비면 `NotiUiState.Empty`).
   - 효과: 진입 시 안 읽은 알림은 세션 동안 계속 보이고(보는 도중 `markAllRead` 로 read=true 가 돼도 유지),
     이미 읽은 알림은 처음부터 숨겨지며, 다음 진입(재구독)에는 모두 사라진다.
2. **`NotiScreen`**: 진입 시 `markAllRead()` 호출은 **유지**한다(다음 진입에 숨겨지도록 읽음 상태를 영속).
   그룹핑/시간 라벨/정렬 로직은 변경하지 않는다.

## 테스트 (먼저 작성/수정)
- `app/src/test/.../notification/NotiViewModelTest.kt`:
  - 진입 시 read==true 알림은 그룹/목록에 **나타나지 않는다**(기존
    `notificationResultsProduceContentGroupedByTodayAndYesterday` 가 읽은 'gift'를 어제 그룹에 기대하던 부분을
    새 동작에 맞게 수정).
  - 세션 중 `markAllRead()` 후에도 진입 시 보이던 항목은 **계속 보이되 unread=false** 가 된다
    (`markAllReadClearsUnreadIndicators` 가 그대로 통과하도록).

## 금지 / 불변
- `NotificationRepository`/`markAllRead` 계약, 날짜 그룹/시간 라벨 포맷 불변.
- 디자인 토큰/한국어 카피 가드레일 준수.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 1 을 `completed` + `summary` 로 갱신·커밋.
