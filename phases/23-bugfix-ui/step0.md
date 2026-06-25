# Step 0 — 바텀 네비게이션 ↔ 시스템 3버튼 네비바 겹침 수정 (window insets)

## 배경
`targetSdk = 35` 이므로 Android 15+ 에서 **edge-to-edge 가 강제**된다(앱 윈도우가 시스템 바 뒤까지 그려짐).
`ui/navigation/AppNavHost.kt` 의 커스텀 바텀바 `CafeBottomBar`(고객) · `OwnerBottomBar`(점주) 는
`Box(...).height(spacing.space18).background(colors.canvas)` 로 그려지지만 **`WindowInsets.navigationBars` 를
소비하지 않는다.** 그래서 **3버튼 내비게이션** 모드 기기에서 시스템 네비바(뒤로/홈/최근)와 앱 바텀탭의
아이콘·라벨·터치 영역이 **겹친다**(제스처 내비 기기는 인셋이 거의 0이라 증상이 약함).

`Scaffold` 는 콘텐츠(NavHost)에 status bar 인셋을 `innerPadding` 으로 전달하지만, bottomBar 슬롯이 있을 때
하단 내비바 인셋 처리는 **바텀바 자신의 책임**이다. 두 커스텀 바가 이를 처리하지 않아 발생하는 버그다.

## 작업 범위 (이 step에서만)
1. `CafeBottomBar` 와 `OwnerBottomBar` 가 하단 시스템 내비게이션 바 인셋만큼 **탭 콘텐츠를 위로 올리도록**
   `WindowInsets.navigationBars` 를 소비한다.
   - 권장 구현: 바깥 `Box` 에 `.background(colors.canvas)` 를 **먼저** 적용해 배경이 인셋 영역(화면 맨 아래)까지
     채우게 하고, 그 다음 `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` 로 내부 콘텐츠를 인셋 위로 올린다.
     탭 `Row` 의 시각 높이(`spacing.space18`)와 상단 hairline 선은 그대로 유지한다(탭은 시스템 네비바 위에 위치).
   - 또는 동등한 효과(탭이 시스템 네비바와 겹치지 않고, 배경/구분선은 자연스럽게 이어짐)를 내는 방식이면 된다.
2. 고객 바·점주 바 **둘 다 동일하게** 처리해 셸 일관성을 유지한다.
3. (선택, 무리하지 말 것) `MainActivity.onCreate` 에 `enableEdgeToEdge()` 를 추가해 시스템 바를 투명·일관 처리할 수
   있다. 추가한다면 상단(상태바) 콘텐츠가 가려지지 않는지(=`Scaffold` innerPadding 으로 top 인셋이 전달되는지)
   반드시 확인한다. 리스크가 있거나 불필요하면 생략하고 1·2 만 수행한다.

## 금지 / 불변
- 바의 시각 높이(`space18`)·디자인 토큰·hex 가드레일·탭 구성/라벨/아이콘을 바꾸지 않는다.
- 다른 화면/기능/네비 로직을 건드리지 않는다(인셋 처리 외 변경 금지).
- import 는 `androidx.compose.foundation.layout.WindowInsets`, `.navigationBars`, `.windowInsetsPadding` 를 사용한다.

## 테스트
- `AppNavHostTest`(androidTest) 가 이미 존재한다 — 컴파일이 깨지지 않게 유지한다. 윈도우 인셋은 계측 환경 의존이라
  단위/계측으로 정밀 검증이 어렵다. AC 의 빌드·컴파일 통과로 충분하다. 기존 테스트를 깨뜨리지 않는다.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 0 을 `completed` + `summary` 로 갱신·커밋.
