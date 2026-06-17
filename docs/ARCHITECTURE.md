# 아키텍처 (Android / Kotlin / Compose)

## 스택 요약
- Kotlin · Jetpack Compose · Material3(토큰 오버라이드)
- min SDK 26 / target SDK 35 · Gradle Kotlin DSL + 버전 카탈로그(`libs.versions.toml`)
- Coroutines/Flow · Hilt(DI) · Retrofit+OkHttp(Remote) · Room(Local) · Coil(이미지) · Navigation Compose
- 엔티티/Repository/공통 UiState 계약은 `docs/DATA_MODEL.md`를 따른다.

## 패키지 / 레이어 구조
```
com.cafeminsu/
├── CafeApplication.kt        # @HiltAndroidApp
├── MainActivity.kt           # single-activity, setContent { CafeTheme { AppNavHost() } }
├── core/                     # 공통 유틸, Result 래퍼, 확장함수
├── ui/
│   ├── theme/                # Color.kt / Type.kt / Shape.kt / Spacing.kt / CafeTheme
│   ├── components/           # 디자인 시스템 컴포넌트 (Button, Card, Chip, ...)
│   ├── navigation/           # AppNavHost, Routes(M-코드)
│   └── feature/              # 화면별 (home, menu, voice, cart, payment, stamp, gifticon, my)
│       └── <feature>/        #   <Feature>Screen.kt + <Feature>ViewModel.kt + <Feature>UiState.kt
├── domain/                   # 안드로이드 비종속: model, repository(interface), usecase
└── data/                     # repository 구현, remote(api/dto), local(room/dao), mapper
```
- 의존 방향: `ui → domain ← data`. **domain은 Android/프레임워크에 의존하지 않는다.**
- feature-first 패키징: 한 화면의 Screen/ViewModel/UiState를 같은 패키지에 둔다.

## 패턴
- **Compose + MVVM + 단방향 데이터 흐름(UDF)**
- ViewModel은 화면 상태를 `StateFlow<UiState>`로 노출, 사용자 액션은 함수/이벤트로 수신.
- 데이터 화면 `UiState`는 sealed/`data class`로 표현하고 Loading / Content / Empty / Error / Offline 의미를 빠뜨리지 않는다.
- Composable은 상태를 받기만 하고(stateless), 이벤트를 위로 올린다(state hoisting).

## 데이터 흐름
```
사용자 입력(Composable)
  → ViewModel (intent/이벤트 처리, StateFlow<UiState> 갱신)
    → UseCase (도메인 규칙, 단일 책임)
      → Repository (interface @ domain, 구현 @ data)
        → Remote(Retrofit api → DTO) / Local(Room dao → Entity)
        → Mapper로 domain model 변환
  ← Flow/AppResult 로 결과 방출 → UiState 갱신 → Compose recomposition
```
- 네트워크/DB 호출은 `Dispatchers.IO`. 결과는 `AppResult`/`sealed` 로 성공·실패 표현.
- 캐싱 전략(MVP): 로컬 우선(Room) → 필요 시 원격 갱신. 백엔드 부재 시 Mock Repository로 대체.

## 상태 관리
- 화면 상태: ViewModel의 `MutableStateFlow` → `asStateFlow()`.
- 단발성 이벤트(토스트·네비게이션): `Channel`/`SharedFlow`(replay 0).
- Compose 내 로컬 UI 상태만 `remember`/`rememberSaveable`.

## 에러 처리 & 회복 (Resilience)
- 표준 결과 타입: `sealed interface AppResult<out T> { Success<T>; Failure(error: DomainError) }`.
- 표준 에러: `sealed interface DomainError { Network; Timeout; Unauthorized; NotFound; Payment(reason); Validation(field); Unknown }`. 예외→`DomainError` 매핑은 **`data` 레이어**에서. 도메인/UI는 예외를 던지지 않는다.
- 모든 Repository/UseCase의 외부 호출은 `AppResult`로 감싼다. ViewModel은 `Failure`를 화면 `UiState.Error`로 변환.
- 재시도/백오프: **일시적 네트워크 오류 + 멱등 작업**에만 지수 백오프(최대 N회). 비멱등(결제 등) 자동 재시도 금지.
- 타임아웃: connect/read 기본값 명시(예: 10s/15s).
- 오프라인: 연결 없으면 캐시(Room)를 **읽기 전용**으로 노출 + 오프라인 배너. 쓰기 작업은 차단/큐잉.
- `Unauthorized`(401): 토큰 갱신 1회 시도 → 실패 시 세션 만료(재로그인 유도).

## 인증 / 세션
- 로그인 제공자와 화면 UX는 PRD 오픈 이슈이나, 앱 내부 계약은 Phase 0부터 둔다.
- `SessionRepository`는 `AuthState.Unknown / Guest / Authenticated / Expired`를 노출한다.
- 보호 화면(M-06 결제, M-08 스탬프, M-09 기프티콘, M-10 마이)은 Guest/Expired 진입 시 재로그인 유도 상태로 전환한다.
- 로그아웃·세션 만료는 EncryptedDataStore 토큰과 로컬 민감 데이터를 와이프한다.

## 결제 안전 처리
- **멱등키**(클라이언트 생성 UUID)로 중복 결제 방지. 동일 주문 재시도는 같은 키 사용.
- 결제 **타임아웃/네트워크 끊김 시 낙관적 성공 금지** → 서버에 결제 상태 조회로 확정 후 화면 갱신.
- 결제 버튼은 처리 중 비활성 + 더블탭/중복 제출 가드(진행 중 플래그).
- 결제 민감정보는 저장·로깅 금지(`docs/SECURITY.md`).

## 엣지 케이스 처리 원칙
| 상황 | 처리 |
| --- | --- |
| 메뉴 품절 | 담기 비활성 + 품절 배지. 장바구니에 있으면 결제 전 알림·제거 |
| 영업 종료 / 주문 컷오프 | 주문 버튼 비활성 + 안내. 영업시간 외 진입 차단 |
| 최소 주문 금액 미달 | 결제 차단 + 부족 금액 안내 |
| 장바구니↔결제 사이 가격/재고 변경 | 결제 직전 재검증 → 변경 시 사용자 확인 후 진행 |
| 빈 장바구니 | 결제 진입 불가, 빈 상태 + 메뉴로 유도 |
| 세션 만료 | 보호 화면 진입 시 재로그인, 작업 컨텍스트 보존 |
| 권한 거부(마이크/알림) | 대체 경로 제공 + 설정 유도, 크래시 금지 |

## 네비게이션
- Navigation Compose 단일 `AppNavHost`. 라우트는 PRD **M-코드**와 1:1.
- 라우트 상수: `ui/navigation/Routes.kt` (`HOME="m01"`, `MENU="m02"`, … `VOICE="m04"`).
- 하단 탭(홈/메뉴/스탬프/마이)은 `Scaffold`의 `bottomBar`. 음성 주문(M-04)은 풀스크린 모달 라우트.

## 음성 AI 주문 파이프라인 (M-04)
```
마이크 입력 → STT(SpeechRecognizer 또는 클라우드 STT) → 텍스트 transcript
  → 주문 파싱(UseCase: 발화 → {메뉴, 옵션, 수량} 구조화; LLM 또는 규칙기반)
  → CartRepository 에 반영 → 사용자 확인(M-04 결과 영역) → 장바구니(M-05)
```
- STT 경로·파싱 방식 결정은 `ADR.md` ADR-004. 권한(RECORD_AUDIO)·키 필요 시 step `blocked`.

## DI / 모듈
- Hilt: `@HiltAndroidApp`, ViewModel은 `@HiltViewModel`.
- Repository 바인딩은 `@Module @InstallIn(SingletonComponent::class)`의 `@Binds`로 interface↔구현 연결.
- Mock/Real Repository 교체를 DI 모듈 수준에서 스위칭 가능하게 둔다.

## 테스트 구조
- `test/` (JVM 단위): domain UseCase, ViewModel(StateFlow는 Turbine), Mapper. Mock은 MockK.
- `androidTest/` (계측): Compose UI Test(`createComposeRule`), 핵심 화면 스모크.
- TDD: 도메인/데이터는 실패 테스트 → 구현 순서. (CLAUDE.md 테스트 규칙 참조)
