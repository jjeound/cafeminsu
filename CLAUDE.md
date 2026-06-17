# CafeMinsu — 프로젝트 규칙 (Guardrails)

동네 카페 "민수"의 **네이티브 Android 모바일 주문·적립 앱**. 본 문서는 모든 빌드 step에 주입되는
핵심 규칙이다. 상세 스펙은 `docs/`를 따른다: 제품=`PRD.md`, 구조=`ARCHITECTURE.md`,
결정=`ADR.md`, 데이터 계약=`DATA_MODEL.md`, 화면 적용=`UI_GUIDE.md`, 디자인 토큰=`DESIGN_SYSTEM.md`,
보안=`SECURITY.md`.

## 타깃 / 스택
- 플랫폼: **Android (네이티브)**, 언어: **Kotlin**
- UI: **Jetpack Compose** (+ Material3 베이스, 컬러 롤은 디자인 시스템 토큰으로 오버라이드)
- min SDK 26 / target SDK 35 (확정·변경은 `ARCHITECTURE.md` 기준)
- 빌드: Gradle (Kotlin DSL, `build.gradle.kts`), 버전 카탈로그(`libs.versions.toml`)
- 비동기: Coroutines + Flow / StateFlow
- DI: Hilt · 네트워킹: Retrofit(+OkHttp) · 로컬: Room · 이미지: Coil
- 키오스크 앱은 **MVP 범위 밖**(모바일 우선).

## 아키텍처 (요약)
- 패턴: **Compose + MVVM + 단방향 데이터 흐름(UDF)**
- 레이어: `ui / domain / data` (의존 방향 ui → domain ← data, domain은 안드로이드 비종속)
- 흐름: UI(Compose) → ViewModel(`StateFlow<UiState>`) → UseCase → Repository → Remote/Local
- 네비게이션: Navigation Compose, 화면은 PRD의 **M-코드** 라우트로 관리
- 자세한 내용은 `docs/ARCHITECTURE.md`.

## 디자인 규칙 (엄수)
- 색·치수·타이포는 **반드시 토큰(Compose Theme)으로만** 참조한다.
- **hex 리터럴 금지.** hex는 오직 `ui/theme/Color.kt` 토큰 정의 한 곳에만 존재한다.
- 토큰명·값·컴포넌트 스펙의 단일 진실은 `docs/DESIGN_SYSTEM.md`. 임의로 새 색/토큰을 만들지 않는다.
- 안티-AI슬롭 do/don't는 `docs/UI_GUIDE.md`를 따른다(보라/인디고 기본색·글래스모피즘·네온 글로우 금지).
- UI 카피는 **한국어**.

## 테스트 (TDD)
- **테스트 우선.** 도메인/데이터 로직은 구현 전에 실패하는 테스트부터 작성한다.
- 단위 테스트: JUnit + MockK + Turbine(Flow 검증). UI: Compose UI Test(`createComposeRule`).
- 기존 테스트를 깨뜨리지 않는다. AC(Acceptance Criteria)는 직접 실행해 검증한다.
- `scripts/hooks/tdd-guard.sh`는 Gradle 스캐폴드가 생긴 뒤 `src/main` Kotlin 구현 파일에 대응하는
  `src/test` 또는 `src/androidTest` 테스트 파일이 먼저 있는지 확인한다.

## 보안 · 에러 (필수)
- 인증 토큰·세션은 **EncryptedDataStore**(평문 저장 금지), 로그아웃 시 와이프. 캐시는 Room.
- 통신은 **HTTPS 강제 · cleartext 차단**. 결제 카드 PAN/CVC 미저장·미로깅, PG 토큰화만.
- 릴리스에서 토큰·PII·결제정보 **로깅 금지**. 비밀키 하드코딩 금지(`local.properties`/CI secret).
- 수량·금액·옵션·**딥링크** 입력 검증. 권한(마이크/알림) 최소화 + rationale.
- 모든 외부 호출은 **`AppResult`로 감싸** 화면 `UiState.Error`로 변환(예외 전파 금지). 금전 액션은 낙관적 UI 금지.
- 상세 규칙의 단일 진실: `docs/SECURITY.md` (에러/엣지 처리는 `docs/ARCHITECTURE.md`).

## 코드 컨벤션
- 패키지 루트: `com.cafeminsu`. 기능별 패키지(feature-first) + 공통 `ui/theme`, `core`.
- 포맷/린트: ktlint + detekt 통과 기준. 한 파일 한 책임, 함수는 작게.
- 문자열·치수·색은 리소스/토큰화. 매직 넘버 금지.

## 작업 / 커밋 규칙 (하네스)
- 각 step은 **명시된 작업만** 수행한다. 추가 기능·파일을 임의로 만들지 않는다.
- 커밋 메시지: `feat(<phase>): step N — <name>` (코드), `chore(<phase>): step N output` (산출물).
  → `scripts/execute.py`의 `FEAT_MSG`/`CHORE_MSG` 규칙과 일치시킨다.
- 사용자 개입 필요(API 키·인증·결제 PG 키 등) 시 해당 step을 `blocked` 처리하고 즉시 중단한다.
