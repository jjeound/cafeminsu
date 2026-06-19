---
version: alpha
name: CafeMinsu Design System
inspired_by: Anthropic Claude (warm cream + coral + dark navy editorial)
applies_to:
  - Mobile App (primary, MVP)
  - Kiosk App (future, out of MVP scope)
figma_file: https://www.figma.com/design/D5KoOaDFzNRh7WMcyYM2cu
source_of_truth: Figma Variables (Collection "Colors") + Cover frame typography
implements_as: Jetpack Compose Theme (Color.kt / Type.kt / Shape.kt / Spacing.kt)
---

# CafeMinsu Design System

Anthropic Claude의 따뜻한 에디토리얼 감성에서 출발한 카페민수 모바일 앱 디자인 시스템.
크림 베이스 + 코랄 액센트 + 다크 네이비 서피스 조합으로 "차분하면서 손맛이 있는" 카페 톤을 만든다.

> **원칙**: 모든 색은 hex 직접 입력 금지. 반드시 토큰(=Figma Variable / Compose Theme 토큰) 이름으로 참조한다.
> Figma 파일의 Variable이 단일 진실 공급원이며, 본 문서의 토큰명은 Figma 및 Compose Theme와 1:1로 묶여 있다.
> 본 문서는 토큰·타이포·컴포넌트 **스펙의 단일 진실**이다. 화면별 적용 가이드와 안티슬롭 원칙은 `UI_GUIDE.md` 참조.

## 1. Personality

- **Warm** — 따뜻한 크림 `canvas` 캔버스, 다소 누런 톤
- **Considered** — 큼직한 헤드라인, 충분한 여백, 정돈된 그리드
- **Premium** — 한 발짝 떨어진 코랄 `primary`, 다크 네이비 `surface-dark` 카드
- **Calm** — 흰색이 아닌 미세한 sand surface, 강한 채도를 피한다

## 2. Color Tokens

### 2.1 Foundation (Figma synced)

Figma "Colors" Variable에 실제로 등록되어 있고 cover frame이 사용 중인 토큰.

| Token        | Hex      | 용도                                  |
| ------------ | -------- | ------------------------------------- |
| `canvas`     | #faf9f5  | 앱 전체 베이스 (warm cream)           |
| `ink`        | #141413  | 헤드라인, 강조 텍스트                 |
| `body`       | #3d3d3a  | 본문 텍스트                           |
| `muted`      | #6c6a64  | 보조 텍스트, placeholder, 비활성 아이콘 |
| `muted-soft` | #8e8b82  | 메타(버전, 타임스탬프), 가장 약한 텍스트 |
| `primary`    | #cc785c  | CTA, 강조 버튼, 활성 인디케이터, 액센트 |

### 2.2 Surface (mobile system tokens, pending Figma sync)

모바일 카드/리스트/구분선용. 본 문서 작성 시점에 Figma Variable에 미등록이므로 **Figma에 동일 명/값으로 추가 등록 필요**.

| Token          | Hex      | 용도                                  |
| -------------- | -------- | ------------------------------------- |
| `surface-card` | #efe9de  | 카드, 입력 박스, 리스트 셀 (warm sand) |
| `surface-dark` | #181715  | 프로덕트 메뉴 카드, 다크 강조 영역    |
| `hairline`     | #e6dfd1  | 세퍼레이터, 비활성 보더               |

> 이전 버전의 `surface-ink`(#141413)는 `ink`와 hex가 동일해 의미 중복이라 제거. 어두운 서피스는 `surface-dark` 하나로 통일한다.

### 2.3 Brand Extended (pending Figma sync)

| Token           | Hex      | 용도                            |
| --------------- | -------- | ------------------------------- |
| `primary-hover` | #b65f44  | hover / pressed                 |
| `accent-soft`   | #f1cdbd  | 태그, 뱃지 배경, 음성 펄스 외곽 |

### 2.4 On-Color (pending Figma sync)

대비 텍스트 전용. 직접 색을 쓰지 말고 항상 이 토큰을 참조한다.

| Token        | Hex      | 용도                              |
| ------------ | -------- | --------------------------------- |
| `on-primary` | #ffffff  | `primary` 위 텍스트/아이콘        |
| `on-dark`    | #faf9f5  | `surface-dark` 위 텍스트/아이콘   |

### 2.5 State (pending Figma sync)

| Token     | Hex      | 용도                            |
| --------- | -------- | ------------------------------- |
| `success` | #5b8a72  | 결제 성공, 스탬프 완료          |
| `warning` | #c08a2e  | 곧 만료되는 기프티콘            |
| `error`   | #b14b3a  | 결제 실패, 에러 토스트          |

## 3. Typography

전체 폰트는 **Noto Sans KR** 단일 패밀리. Cover frame에서 확인된 weight 4종(Black / Bold / Medium / Regular)만 사용한다.

### Family

| Role     | Family       | Weight  |
| -------- | ------------ | ------- |
| Display  | Noto Sans KR | Black   |
| Heading  | Noto Sans KR | Bold    |
| Subhead  | Noto Sans KR | Medium  |
| Body     | Noto Sans KR | Regular |
| Caption  | Noto Sans KR | Regular |

### Scale (Mobile)

| Token     | Size | Line | Weight   | 색상 토큰   | 예시                  |
| --------- | ---- | ---- | -------- | ----------- | --------------------- |
| `display` | 32   | 40   | Black    | `ink`       | 메인 그리팅, 결제 완료 |
| `h1`      | 24   | 32   | Bold     | `ink`       | 화면 타이틀           |
| `h2`      | 20   | 28   | Bold     | `ink`       | 섹션 헤더             |
| `h3`      | 17   | 24   | Medium   | `ink`       | 카드 타이틀, 메뉴명   |
| `bodyL`   | 16   | 24   | Medium   | `body`      | 본문 강조             |
| `body`    | 14   | 20   | Regular  | `body`      | 일반 본문             |
| `caption` | 12   | 16   | Regular  | `muted`     | 메타, 가격 보조       |
| `meta`    | 12   | 16   | Regular  | `muted-soft`| 가장 약한 메타(버전 등) |

> 색상은 디폴트값이며, 컨텍스트에 따라 다른 토큰으로 바인딩 가능. 단, 반드시 토큰 이름으로 지정한다.

## 4. Spacing & Layout

- 4px 그리드 기반
- 표준 스페이싱 토큰: `space-1=4` · `space-2=8` · `space-3=12` · `space-4=16` · `space-5=20` · `space-6=24` · `space-8=32` · `space-10=40` · `space-14=56` · `space-18=72`
- 화면 사이드 패딩: `space-5` (20)
- 카드 내부 패딩: `space-4`~`space-5` (16~20)
- 섹션 간격: `space-8` (32)

## 5. Radius

| Token         | px  | 사용처                            |
| ------------- | --- | --------------------------------- |
| `radius-sm`   | 8   | 칩, 작은 태그                     |
| `radius-md`   | 12  | 인풋, 작은 버튼                   |
| `radius-lg`   | 16  | 일반 카드, primary 버튼           |
| `radius-xl`   | 24  | 프로덕트 카드, 큰 모달            |
| `radius-pill` | 999 | 라운드 버튼, 카테고리 토글        |

## 6. Elevation

기본은 평평한 에디토리얼 톤. 필요 시 토큰으로만 사용한다. 그림자 색은 `ink` 기반.

- `elev-card`: `0 1 2 rgba(ink, 0.04)`
- `elev-overlay`: `0 8 24 rgba(ink, 0.08)`

## 7. Components

모든 컴포넌트의 색·배경·보더·텍스트는 **토큰 이름만** 사용한다. 코드에서 hex 리터럴이 나오면 안 된다.

### 7.1 Button

| Variant     | Background     | Text         | Border          | Pressed         | 용도         |
| ----------- | -------------- | ------------ | --------------- | --------------- | ------------ |
| `primary`   | `primary`      | `on-primary` | —               | `primary-hover` | 메인 CTA     |
| `secondary` | `canvas`       | `ink`        | 1px `hairline`  | `surface-card`  | 보조 CTA     |
| `ghost`     | transparent    | `primary`    | —               | `accent-soft`   | 텍스트 버튼  |
| `dark`      | `surface-dark` | `on-dark`    | —               | `ink`           | 다크 카드 CTA |

- height **52**, radius `radius-lg` (16)
- horizontal padding `space-5` (20)
- 아이콘+텍스트 gap `space-2` (8)

### 7.2 Card

| Type       | Background     | Text 기본 | Border          | Radius        | Padding  |
| ---------- | -------------- | --------- | --------------- | ------------- | -------- |
| `default`  | `surface-card` | `body`    | —               | `radius-lg`   | `space-5` |
| `product`  | `surface-dark` | `on-dark` | —               | `radius-xl`   | `space-5` |
| `info`     | `canvas`       | `body`    | 1px `hairline`  | `radius-lg`   | `space-5` |

- product 카드 가격 강조 텍스트: `primary`
- 헤드라인 텍스트: `ink` (default/info) / `on-dark` (product)

### 7.3 Input

- 배경 `surface-card`, radius `radius-md` (12)
- padding `space-3` `space-4` (12 / 16)
- placeholder 텍스트 `muted`
- 입력 텍스트 `ink`
- focus: 1.5px `primary` 보더
- error: 1.5px `error` 보더 + helper text `error`

### 7.4 Status Bar / App Bar

- 높이 44 / 56
- 배경 `canvas`
- 타이틀 텍스트 `ink`, weight Bold (h2 스케일)
- 아이콘 24px stroke, 색 `ink`

### 7.5 Tab Bar (Bottom Navigation)

- 높이 72
- 배경 `canvas`, 상단 1px `hairline` top border
- 활성 아이콘/라벨 `primary`
- 비활성 아이콘/라벨 `muted`
- MVP 탭: 홈 · 메뉴 · 스탬프 · 마이페이지

### 7.6 Chip / Tag

- 배경 `accent-soft`, 텍스트 `primary`
- 선택 상태: 배경 `primary`, 텍스트 `on-primary`
- radius `radius-pill`, height 32, horizontal padding `space-3`

### 7.7 Toast / Snackbar

- 배경 `surface-dark`, 텍스트 `on-dark`
- 성공 아이콘 `success`, 에러 아이콘 `error`, 경고 아이콘 `warning`
- radius `radius-md`

## 8. Iconography

- 사이즈: 20 / 24 / 32px
- stroke 1.5px 라인 아이콘
- 색상 토큰
  - 기본: `ink`
  - 활성: `primary`
  - 비활성/보조: `muted`
  - 다크 서피스 위: `on-dark`

## 9. Voice / AI Visual Cue

음성 AI 주문(M-04) 화면 전용:

- 원형 코랄 펄스: `primary` → `accent-soft` 그라데이션
- 중앙 마이크 아이콘 색 `on-primary`
- 화면 배경 `canvas`
- 하단 transcript 텍스트 `ink` (확정), `muted` (interim)

## 10. Photography / Illustration Direction

- 메뉴 이미지: 따뜻한 톤(약간 노란기), `surface-dark` 카드 위에서 잘 살아나는 색감
- 일러스트는 사용 자제, 필요 시 `ink` 단색 라인 일러스트만

## 11. Compose 매핑

토큰은 Jetpack Compose 테마로 1:1 구현한다. **hex 리터럴은 오직 `Color.kt`의 토큰 정의 한 곳에만** 존재하고, 그 외 모든 코드는 `MaterialTheme`/커스텀 `CafeTheme` 토큰명을 통해서만 색을 참조한다.

| 본 문서 토큰 카테고리 | Compose 구현 위치           | 비고                                              |
| --------------------- | --------------------------- | ------------------------------------------------- |
| Color Tokens (§2)     | `ui/theme/Color.kt`         | `val canvas = Color(0xFFFAF9F5)` … 정의는 여기만  |
| Color → 시맨틱 매핑   | `ui/theme/CafeColors.kt`    | `CompositionLocal`로 토큰 묶음 제공(`LocalCafeColors`) |
| Typography (§3)       | `ui/theme/Type.kt`          | `display`/`h1`…`meta` → Compose `TextStyle`       |
| Radius (§5)           | `ui/theme/Shape.kt`         | `radius-sm`…`radius-pill` → `RoundedCornerShape`  |
| Spacing (§4)          | `ui/theme/Spacing.kt`       | `space-1`…`space-18` → `Dp` 상수                  |
| Elevation (§6)        | `ui/theme/Elevation.kt`     | `elev-card`/`elev-overlay`                         |
| Components (§7)       | `ui/components/*`           | 위 토큰만 조합, 자체 hex 금지                     |

> Material3를 베이스로 쓰되 컬러 롤은 본 디자인 시스템 토큰으로 오버라이드한다. `MaterialTheme.colorScheme`의 기본값(보라/인디고 등)을 그대로 노출하지 않는다.

## 12. 적용 메모
- Figma 파일: https://www.figma.com/design/D5KoOaDFzNRh7WMcyYM2cu
- Figma Variable Collection: **Colors** (현재 6개 토큰 등록: `canvas`, `ink`, `body`, `muted`, `muted-soft`, `primary`)
- 본 문서의 `surface-*`, `primary-hover`, `accent-soft`, `on-*`, state 토큰은 모바일 적용 시 필요하므로 **Figma Variable에 동일 명/값으로 추가 등록** 후 사용할 것
- 코드 / Figma 디자인 둘 다 색상은 hex 직접 입력 금지. 항상 Variable binding(토큰명) 사용
- 폰트는 Noto Sans KR 단일 패밀리, weight만 변경
- 키오스크 앱은 MVP 범위 밖. 토큰은 플랫폼 비종속이므로 추후 키오스크에 그대로 재사용한다.
