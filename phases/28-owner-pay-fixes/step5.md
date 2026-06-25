# Step 5: menu-image-fallback (메뉴 이미지 표시 + null 폴백)

고객 메뉴 리스트의 `MenuThumbnail()` 은 `menu.imageUrl` 을 **렌더하지 않는 빈 색상 Surface** 이고
`MenuItemUiModel` 에 `imageUrl` 필드조차 없다. 메뉴 이미지를 실제로 표시하되, **`imageUrl` 이 null 이면
번들된 로컬 이미지로 대체** 표시한다. 폴백 원본은 SVG 31개(메뉴 종류명) — Coil 2.7.0 은 있으나 SVG 디코더가
없으므로 `coil-svg` 를 추가한다.

## 읽어야 할 파일

- `app/src/main/java/com/cafeminsu/ui/feature/menu/MenuUiState.kt` (`MenuItemUiModel` — imageUrl 없음)
- `app/src/main/java/com/cafeminsu/ui/feature/menu/MenuViewModel.kt` (도메인 `MenuItem` → `MenuItemUiModel` 매핑)
- `app/src/main/java/com/cafeminsu/ui/feature/menu/MenuScreen.kt` (`MenuListItem`/`MenuThumbnail` 빈 Surface)
- `app/src/main/java/com/cafeminsu/ui/feature/menu/MenuDetailScreen.kt` (대표 이미지 영역)
- `app/src/main/java/com/cafeminsu/ui/feature/owner/menu/` (점주 메뉴 목록 `OwnerMenu*Screen` 썸네일)
- `app/src/main/java/com/cafeminsu/ui/feature/owner/menu/OwnerMenuAddScreen.kt` (이미 `AsyncImage` 사용 — 참고)
- `app/src/main/java/com/cafeminsu/domain/model/Menu.kt` (`MenuItem.imageUrl`, `categoryId`)
- `app/src/main/java/com/cafeminsu/CafeApplication.kt` (Application — Coil ImageLoader 설정 위치)
- `gradle/libs.versions.toml` (`coil = "2.7.0"`, `coil-compose`) · `app/build.gradle.kts`(deps)
- `app/src/test/java/com/cafeminsu/ui/feature/menu/MenuViewModelTest.kt`
- `/docs/UI_GUIDE.md` · `/docs/DESIGN_SYSTEM.md` (토큰만, 안티-AI슬롭)

## 작업

1. **폴백 에셋 번들** — `C:\Users\SSAFY\Downloads\menu\*.svg`(31개)를 `app/src/main/assets/menu/` 로 복사(파일명 유지). 예시:
   `americano.svg, cafe-latte.svg, latte.svg, vanilla-latte.svg, condensed-milk-latte.svg, cafe-mocha.svg, coffee.svg, ade.svg, lemon-ade.svg, lime-ade.svg, grapefruit-ade.svg, greengrape-ade.svg, green-tangerine-ade.svg, smoothie.svg, banana-smoothie.svg, kiwi-smoothie.svg, mango-smoothie.svg, strawberry-smoothie.svg, tea.svg, iced-tea.svg, earl-grey-tea.svg, chamomile-tea.svg, peppermint-tea.svg, rooibos-tea.svg, yuja-tea.svg, dessert.svg, brownie.svg, cheesecake.svg, choco-cake.svg, madeleine.svg, scone.svg`
   - (Git Bash) `mkdir -p app/src/main/assets/menu && cp /c/Users/SSAFY/Downloads/menu/*.svg app/src/main/assets/menu/`
   - 복사가 불가하면(원본 부재) step 을 `blocked` 처리하고 사유 기록.

2. **SVG 디코더** — `gradle/libs.versions.toml` 에 `coil-svg = { group = "io.coil-kt", name = "coil-svg", version.ref = "coil" }` 추가, `app/build.gradle.kts` 에 의존 추가.
   - `CafeApplication` 이 `coil.ImageLoaderFactory` 구현 → `ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build()`. (Coil 이 Application 의 팩토리를 자동 사용.)

3. **폴백 리졸버** — `ui/feature/menu/MenuImageFallback.kt`(또는 `ui/components/`):
   - `fun menuFallbackAsset(name: String, categoryId: String? = null): String` → `"file:///android_asset/menu/<file>.svg"`.
   - 매칭 우선순위: ① 이름(한글) 키워드 → 파일(예: "아메리카노"→americano, "바닐라라떼"→vanilla-latte, "카페라떼"→cafe-latte, "라떼"→latte, "모카"→cafe-mocha, "레몬에이드"→lemon-ade, "에이드"→ade, "딸기스무디"→strawberry-smoothie, "스무디"→smoothie, "얼그레이"→earl-grey-tea, "유자"→yuja-tea, "티/차"→tea, "치즈케이크"→cheesecake, "브라우니"→brownie, "스콘"→scone, "마들렌"→madeleine) → ② 카테고리 기본(커피→coffee, 티/차→tea, 에이드→ade, 스무디→smoothie, 디저트→dessert) → ③ 제네릭 기본 `coffee.svg`.
   - 매핑표는 **번들된 31개 파일명 기준**으로 작성(존재하지 않는 파일명 매핑 금지). 구체 키워드를 먼저, 포괄 키워드를 나중에 매칭.

4. **표시 컴포저블** — 재사용 `MenuImage(imageUrl: String?, menuName: String, categoryId: String? = null, modifier)`:
   - `val model = imageUrl?.takeIf { it.isNotBlank() } ?: menuFallbackAsset(menuName, categoryId)`
   - `AsyncImage(model = model, contentScale = Crop, ...)`. http(s) URL 과 asset SVG 모두 동일 ImageLoader 로 렌더.
   - 적용:
     - `MenuItemUiModel` 에 `imageUrl: String?`(+ 필요시 `categoryId`) 추가, `MenuViewModel` 매핑에서 `MenuItem.imageUrl`/`categoryId` 전달. 고객 `MenuScreen.MenuThumbnail()` → `MenuImage` 사용.
     - `MenuDetailScreen` 대표 이미지, 점주 메뉴 목록 썸네일 등 메뉴 이미지가 보이는 곳에 동일 컴포저블 적용(패턴 반복). 카트/추천에 메뉴 썸네일이 있으면 함께.
   - 토큰만(hex 금지), 한국어 `contentDescription`.

### 핵심 규칙 (반드시 준수)

- 표시 전용 변경 — 도메인/데이터/서버 전송 값은 바꾸지 마라(폴백은 UI 레이어, 서버로 asset URI 를 보내지 마라).
- 매핑은 실제 번들 파일에만 연결(깨진 asset 경로 금지). 기본값 `coffee.svg` 로 항상 안전 폴백.
- hex 리터럴 0, 안티-AI슬롭.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## 검증 절차

1. **테스트 우선(TDD)**: `MenuImageFallbackTest`(JVM 단위) — 이름/카테고리/공백·null 입력 → 기대 asset 파일명 매핑(제네릭 기본 포함). `MenuViewModelTest` 에 `imageUrl` 이 `MenuItemUiModel` 로 전달되는지 검증. 먼저 실패시킨 뒤 구현.
2. 위 AC 통과(빌드에 `assets/menu/*.svg` 포함). 기존 메뉴/홈/점주 테스트 **무회귀**.
3. 결과에 따라 `phases/28-owner-pay-fixes/index.json` 의 step 5 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "메뉴 SVG 31개 assets/menu 번들 + coil-svg(SvgDecoder) ImageLoader + MenuImageFallback(이름/카테고리→asset) + MenuImage 컴포저블로 imageUrl null 시 종류별 번들 SVG 폴백 표시(고객 목록/상세·점주 목록)"`
   - 3회 실패 → `"status": "error"`, `"error_message": "<에러>"`
   - 원본 SVG 부재 등 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "<사유>"` 후 중단

## 금지사항

- 폴백 asset URI 를 서버 `imageUrl`(메뉴 생성/수정)로 전송하지 마라(표시 전용).
- 존재하지 않는 asset 파일명에 매핑하지 마라. hex 색 리터럴 금지. 기존 테스트를 깨뜨리지 마라.
