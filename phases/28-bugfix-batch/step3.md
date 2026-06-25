# Step 3 — 메뉴 이미지 없으면 기본 이미지로

## 배경 / 요구
고객 메뉴 화면은 실제 이미지를 렌더링하지 않고 **빈 placeholder 박스**만 보여 준다:
`ui/feature/menu/MenuScreen.kt` 의 `MenuThumbnail`(빈 Surface), `MenuDetailScreen.kt` 의
`MenuImageHero`(원형 placeholder). 메뉴 모델 `domain/model/Menu.kt` 에는 `imageUrl: String?` 가 있다.
요구: **메뉴 이미지를 렌더링하고, 이미지가 없으면(=imageUrl 이 null/blank) 기본 이미지**(아래 coffee.svg)로 표시.

- Coil 은 이미 의존성에 있음(`coil-compose` 2.7.0). (coil-svg 는 없음 → 원격 SVG 디코딩은 불필요, imageUrl 은
  래스터로 가정.) 점주 메뉴 추가 화면(`owner/menu/OwnerMenuAddScreen.kt`)이 이미 `AsyncImage` 사용 패턴 보유.
- 기본 이미지 원본: `/Users/jje/Downloads/menu/coffee.svg` — 이 파일을 **Read 도구로 열어** 내용을 보고
  Android **벡터 드로어블**(`app/src/main/res/drawable/img_menu_default.xml`)로 변환해 추가한다(hex 색은 토큰
  규칙 예외인 리소스 파일이므로 드로어블 내부 색상은 허용; 단 디자인 톤과 어울리게).

## 작업 범위 (이 step에서만)
1. `coffee.svg` → `res/drawable/img_menu_default.xml` 벡터 드로어블 변환·추가.
2. `MenuThumbnail` 과 `MenuImageHero`(및 메뉴 목록/상세에서 이미지를 보여 주는 곳)가 `imageUrl` 을
   **Coil `AsyncImage`** 로 표시하도록 한다. `placeholder`/`error`/`fallback` 을 `img_menu_default` 로 지정해
   **imageUrl 이 null/blank 이면 기본 이미지**가 보이게 한다. `ContentScale.Crop`, 기존 크기/모양/토큰 유지.
3. `imageUrl` 을 UiState/아이템 모델에서 해당 컴포저블까지 전달(현재 `MenuThumbnail`/`MenuImageHero` 는 인자가
   없으니 파라미터 추가). 메뉴 목록 아이템·상세 모두 적용.

## 테스트
- 단위/androidTest 컴파일 유지. 가능하면 UiState 에 `imageUrl` 전달 검증을 추가한다(최소 컴파일 보장).

## 금지 / 불변
- 새 네트워킹/이미지 라이브러리 추가 금지(이미 있는 `coil-compose` 만 사용).
- 색·치수·타이포는 토큰만(드로어블 리소스 내부 색 제외). 한국어 카피 가드레일 준수.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 3 을 `completed` + `summary` 로 갱신·커밋.
