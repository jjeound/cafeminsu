# Step 3 — 마이 탭 "알림설정" 실제 화면 신설 (준비중 → 동작)

## 배경
마이 탭(`ui/feature/my/MyScreen.kt`) 빠른메뉴의 "알림설정" 타일은 현재 `MyRoute` 에서
`Toast.makeText(context, "알림설정은 준비 중이에요", ...)` 만 띄운다(`onNotificationSettingsClick`). 이 "준비 중"을
없애고 **실제로 동작하는 알림 설정 화면**을 신설해 연결한다. 푸시 알림 카테고리 on/off 를 로컬 영속한다.
(전용 디자인 PNG 는 없다. 첨부 `docs/screens/알림.png`·`docs/screens/MY - 01.png` 의 톤&매너와 디자인 시스템
`DESIGN_SYSTEM.md`/`UI_GUIDE.md` 를 따라 일관되게 구성한다.)

영속은 `data/local/prefs/UserPreferencesDataStore`(phase 20) 를 재사용한다. **단순 on/off boolean 플래그만** 저장한다
(토큰·PII 등 민감정보는 평문 DataStore 금지 — 그대로 EncryptedSharedPreferences 유지).

## 작업 범위 (이 step에서만)
1. **라우트**: `ui/navigation/Routes.kt` 에 `const val NOTI_SETTINGS = "noti_settings"` 추가. `RoutesTest` 갱신.
2. **DataStore**: `UserPreferencesDataStore` 에 알림 설정 boolean 키 + observe/set 메서드를 추가한다. 카테고리 3종:
   - 주문 상태 알림 `orderStatus` — 기본 **on**
   - 혜택·이벤트 알림 `promotion` — 기본 **on**
   - 마케팅 정보 수신 `marketing` — 기본 **off**
   기존 키/메서드 패턴(`booleanPreferencesKey`, `read{}`, `setXxx`)을 그대로 따른다. `UserPreferencesDataStoreTest` 갱신
   (기본값·set 후 observe 반영).
3. **화면(MVVM, `ui/feature/notification/settings/`)** — **테스트 먼저(TDD)**:
   - `NotificationSettingsUiState`(토글 3종 상태를 담는 모델) + (필요 시 `NotificationSettingsUiStateTest`).
   - `NotificationSettingsViewModel`(`@HiltViewModel`, `UserPreferencesDataStore` 주입): 세 플래그를 `combine` 해
     `StateFlow<NotificationSettingsUiState>` 로 노출, `onToggle(category, enabled)` → DataStore set. `NotificationSettingsViewModelTest`
     로 observe→state, toggle→영속 검증(가짜/인메모리 DataStore 또는 추상화). 토글은 로컬 동작이라 낙관적 UI 허용(금전 아님).
   - `NotificationSettingsScreen` + `NotificationSettingsRoute`(`hiltViewModel()`): `CafeTopBar` 제목 "알림 설정", 좌측 `‹`
     back. 토글 행 3개(라벨 + 부제 설명 + **Material3 `Switch`**). `Switch` 색은 디자인 토큰(예: checked track `colors.primary`)
     으로 지정. 카드/구분선/여백은 `MyScreen` 의 `SettingsList` 스타일과 일관되게. **hex 금지**, 한국어 카피.
     androidTest `NotificationSettingsScreenTest`(제목·토글 라벨 표시, 컴파일).
4. **네비 연결**:
   - `AppNavHost` 에 `composable(Routes.NOTI_SETTINGS) { NotificationSettingsRoute(onBackClick = { navController.popBackStack() }) }` 추가.
   - `MyRoute` 의 `onNotificationSettingsClick` 을 **Toast 제거**하고 `onNotificationSettingsClick: () -> Unit` 파라미터로
     올린다(라우트 콜백). `AppNavHost` 의 `MyRoute(...)` 호출부에서 `onNotificationSettingsClick = { navController.navigate(Routes.NOTI_SETTINGS) }`
     를 전달. `MyScreen` 의 해당 콜백 배선은 유지. `MyScreenTest` 가 시그니처 변경에 맞게 컴파일되도록 갱신.
   - `NOTI_SETTINGS` 는 `selectedTabRoute` 에 없으므로 바텀바는 자동으로 숨겨진다(서브 화면, GIFT/COUPON 과 동일 패턴) — 추가 처리 불필요.

## 금지 / 불변
- 알림 도메인(`NotificationRepository`)·FCM 토큰 등록/수신 로직을 변경하지 않는다. 이 step 의 토글은 **로컬 설정 표시·영속**
  까지이며, 실제 서버 구독/푸시 게이팅 연동은 범위 밖(필요 시 후속). `summary` 에 이 한계를 명시한다.
- 다른 화면/탭/기능을 건드리지 않는다. 디자인 토큰·hex·한국어 카피·보안(평문 민감정보 금지) 가드레일 준수.
- 외부 호출은 없다(로컬 only). 새 도메인 모델·기능을 불필요하게 추가하지 않는다.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 3 을 `completed` + `summary` 로 갱신·커밋.
