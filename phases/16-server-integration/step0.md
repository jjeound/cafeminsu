# Step 0 — 네트워크 토대 (Retrofit/OkHttp/Moshi · 보안 설정 · 예외→DomainError 매핑)

실서버 연동의 공통 토대를 만든다. 이 step은 **네트워크 인프라만** 만들고, 실제 Repository 교체는
다음 step부터다. `ARCHITECTURE.md`(데이터 흐름·에러/타임아웃) · `SECURITY.md`(§2 네트워크)를 따른다.

> **API 스펙**: 이 phase의 모든 DTO/엔드포인트는 리포의 **`docs/openapi.json`** 를 단일 진실로 한다.
> 작업 전 반드시 그 파일을 열어 base path·인증 헤더·공통 에러 응답 형태를 확인하라.
> (이 step에서는 DTO를 만들지 않지만, 공통 헤더/에러 규약을 토대에 반영한다.)

## ⚠ 사전 조건 (없으면 blocked)
- `docs/openapi.json` 가 존재하지 않으면 → 이 step을 **blocked**(`blocked_reason: "docs/openapi.json 누락"`)로
  표시하고 즉시 중단하라.
- `local.properties` 에 `BASE_URL` 키가 없어도 빌드는 깨지지 않게 한다(빈 문자열 폴백). 단 값이 있으면
  반드시 **https://** 여야 한다.

## ⚠ TDD — 테스트를 먼저 작성하라
`okhttp-mockwebserver`로 검증한다. 예외→`DomainError` 매핑과 Retrofit 호출 경로를 **실패 테스트 먼저**
작성하고 구현하라.

## 만들 것
### 1) 빌드 의존성 — `gradle/libs.versions.toml` + `app/build.gradle.kts`
카탈로그에 이미 선언된 것 사용: `retrofit`, `retrofit-converter-moshi`, `okhttp`,
`okhttp-logging-interceptor`. **카탈로그에 신규 추가**:
- `moshi` + `moshi-kotlin-codegen`(ksp), 버전 `[versions]`에 `moshi`.
- `androidx.datastore:datastore-preferences`(토큰 저장은 step 1이지만 카탈로그/의존성은 여기서 추가 OK).
- `androidx.security:security-crypto`(EncryptedDataStore/Keystore 래핑용).
- test: `com.squareup.okhttp3:mockwebserver`(testImplementation).
`app/build.gradle.kts` 의 `dependencies`에 위를 `implementation`/`ksp`/`testImplementation`으로 연결.

### 2) 설정값 주입 — `app/build.gradle.kts`
기존 `KAKAO_NATIVE_APP_KEY` 패턴 그대로:
- `local.properties` 의 `BASE_URL` 를 읽어 `buildConfigField("String", "BASE_URL", "\"$baseUrl\"")`.
  (없으면 빈 문자열.) 하드코딩·리포 커밋 금지(`SECURITY.md §7`).

### 3) 네트워크 보안 — `res/xml/network_security_config.xml` + `AndroidManifest.xml`
- `cleartextTrafficPermitted=false`. `AndroidManifest` `<application>` 에
  `android:usesCleartextTraffic="false"` + `android:networkSecurityConfig="@xml/network_security_config"`.
- `INTERNET` 권한이 없으면 추가.

### 4) Hilt `NetworkModule` — `app/src/main/java/com/cafeminsu/data/remote/`
- `OkHttpClient`: connectTimeout 10s / readTimeout 15s(`ARCHITECTURE.md`).
  `HttpLoggingInterceptor` 는 **`BuildConfig.DEBUG` 일 때만** BODY, 릴리스는 미설치/NONE
  (토큰·PII 로깅 금지 — `SECURITY.md §5`).
- `Moshi`, `Retrofit`(baseUrl = `BuildConfig.BASE_URL`, MoshiConverterFactory).
- `@Module @InstallIn(SingletonComponent::class)`.

### 5) 예외→`DomainError` 매핑 — `data/remote/`(data 레이어)
- `Throwable.toDomainError()` 또는 `runCatchingToAppResult { }` 헬퍼: `IOException`→`Network`,
  `SocketTimeoutException`→`Timeout`, HTTP 401→`Unauthorized`, 404→`NotFound`, 그 외→`Unknown`.
- `core/AppResult.kt` 의 `AppResult`/`DomainError` 를 재사용한다(새 결과 타입 만들지 마라).
- **예외는 data 레이어에서만 매핑**하고 도메인/UI로 전파하지 않는다(`ARCHITECTURE.md`).

## 하지 말 것
- 실제 `*Api` 인터페이스·DTO·Repository 구현 금지(다음 step 소관).
- DI 바인딩 교체(Mock→Real) 금지(다음 step). 화면/ViewModel 수정 금지.
- 새 색/토큰·UI 변경 금지. `AppResult`/`DomainError` 외 새 결과 타입 금지.

## Acceptance Criteria
- `./gradlew :app:assembleDebug` 가 성공한다(의존성/매니페스트/BuildConfig 정상).
- `./gradlew :app:testDebugUnitTest` 가 성공한다(MockWebServer 매핑 테스트 + 기존 테스트 무파손).
- `AndroidManifest`/`network_security_config.xml` 에 cleartext 차단이 반영돼 있다.
- 통과하면 `phases/16-server-integration/index.json` 의 step 0 status를 `completed`로 바꾸고 `summary`에
  한 줄 요약을 적어라.
