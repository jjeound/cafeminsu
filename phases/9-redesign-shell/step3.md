# Step 3 — 알림 화면 (Notification, MVVM + TDD)

> 첨부된 `알림.png` + `docs/SCREENS.md`(NOTI)를 그대로 따른다.

홈 우상단 벨로 진입하는 알림 화면과 알림 도메인/Mock 리포지토리를 구현한다.

## 만들 것
1. **도메인**(`DATA_MODEL.md`): `AppNotification`(id, type, title, body, createdAtMillis, read) +
   `NotificationType`(OrderAccepted/OrderReady/OrderCompleted/StampEarned/GifticonReceived).
2. **`MockNotificationRepository`**(data) + 시드(디자인의 항목들: 주문 준비/수락, 스탬프 적립, 기프티콘 도착, 주문 완료) +
   `@Binds` DI. `observeNotifications()`, `markAllRead()`.
3. **화면** — `ui/feature/notification/`: `NotiViewModel`(StateFlow<NotiUiState>) + `NotiScreen`(`docs/SCREENS.md` NOTI):
   `CafeTopBar`(‹ 알림). "오늘"/"어제" 그룹 헤더. 행: 타입별 원형 아이콘(주문=코랄, 스탬프=`warning`, 기프티콘=`success` 계열),
   제목 `bodyL` + 본문 `muted` + 우측 시간 `meta` + 미읽음 코랄 점. 빈 상태 `EmptyView`. 진입 시 `markAllRead` 호출(또는 표시).
4. **연결**: 홈 벨(step 2) → `NOTI` 라우트.

## ⚠ TDD — ViewModel 테스트 먼저
- 알림 목록 Content/Empty/Error 매핑(Turbine). `markAllRead` 후 미읽음 해제 반영. 그룹(오늘/어제) 분류 로직 단위 테스트.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. PII/민감값 로깅 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 홈 벨 → 알림 화면 진입, `알림.png` 구조와 일치.
- 통과하면 `phases/9-redesign-shell/index.json`의 step 3 status를 `completed` + `summary` 기록.
