# Step 0 — Gradle 스캐폴드 & 빌드 가능한 빈 앱

CafeMinsu 안드로이드 프로젝트의 빌드 기반을 만든다. 이 step의 목표는 **`./gradlew :app:assembleDebug`가
성공하는 최소 Compose 앱**이다. 화면 기능은 만들지 않는다(다음 step). 토큰/네비게이션/도메인 로직도 여기서는 만들지 않는다.

## 환경 (이 머신에 이미 설치됨 — 새로 설치하지 마라)
- JDK 17 (PATH의 `java`). `org.gradle.java.installations` 설정 불필요.
- Android SDK: `/Users/jje/Library/Android/sdk` (`platforms/android-35`, `build-tools/35.0.0` 설치됨).
  `ANDROID_HOME`이 비어 있으므로 **`local.properties`에 `sdk.dir=/Users/jje/Library/Android/sdk`** 를 적어라.
  `local.properties`는 `.gitignore`에 있으니 커밋되지 않는다(정상).
- **시스템 gradle 없음.** Gradle 8.14 배포본이 `~/.gradle/wrapper/dists/gradle-8.14-bin/`에 캐시되어 있다.
  Wrapper 파일이 없으면 캐시된 배포본의 `bin/gradle`로 한 번 생성하라:
  `~/.gradle/wrapper/dists/gradle-8.14-bin/*/gradle-8.14/bin/gradle wrapper --gradle-version 8.14 --distribution-type bin`
  (이후로는 `./gradlew`를 쓴다.) `gradle-wrapper.jar`를 직접 base64로 만들지 마라.

## 만들 것
1. **Gradle 설정**
   - `settings.gradle.kts`: `pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }`,
     `dependencyResolutionManagement { repositories { google(); mavenCentral() } }`,
     `rootProject.name = "CafeMinsu"`, `include(":app")`.
   - 루트 `build.gradle.kts`: 플러그인을 `apply false`로 선언(android-application, kotlin-android,
     kotlin-compose, hilt, ksp). 버전은 카탈로그에서.
   - `gradle.properties`: `org.gradle.jvmargs=-Xmx2048m`, `android.useAndroidX=true`, `kotlin.code.style=official`.
   - `gradle/libs.versions.toml`: 버전 카탈로그. 아래 권장 baseline을 시작점으로 쓰되, **상호 호환되도록**
     조정하라(AC는 빌드 성공이 기준이다):
     AGP 8.7.x, Kotlin 2.0.21, Compose Compiler 플러그인 2.0.21, Compose BOM 2024.10.x, Hilt 2.52,
     KSP 2.0.21-1.0.28, Coroutines 1.9.x, Navigation-Compose 2.8.x, AndroidX core-ktx/activity-compose/lifecycle.
     (Retrofit/OkHttp/Room/Coil/MockK/Turbine 좌표도 카탈로그에 **선언만** 해두면 이후 step이 재사용한다.
      이 step의 `:app` 의존성에는 Compose+Hilt+Coroutines+테스트(JUnit)만 실제로 추가한다.)
   - Gradle wrapper 파일 일습(`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`,
     `gradle/wrapper/gradle-wrapper.jar`). `distributionUrl`은 `gradle-8.14-bin.zip`.
2. **`:app` 모듈** — `app/build.gradle.kts`
   - `namespace = "com.cafeminsu"`, `compileSdk = 35`, `defaultConfig { applicationId = "com.cafeminsu";
     minSdk = 26; targetSdk = 35 }`, `buildFeatures { compose = true }`.
   - `compileOptions`/`kotlinOptions` JVM 17.
   - `buildTypes`: `release { isMinifyEnabled = true; proguardFiles(...) }` (룰 파일은 기본 + 빈 `proguard-rules.pro`).
   - 플러그인: android-application, kotlin-android, kotlin-compose, hilt, ksp.
   - 의존성은 **버전 카탈로그 별칭**으로만 추가한다(하드코딩된 버전 문자열 금지).
3. **Manifest & 리소스** — `app/src/main/`
   - `AndroidManifest.xml`: `<application android:name=".CafeApplication" android:usesCleartextTraffic="false"
     android:theme="@style/Theme.CafeMinsu" ...>`, `<activity android:name=".MainActivity"
     android:exported="true">` + LAUNCHER intent-filter. **권한 선언은 추가하지 마라**(이후 기능 step에서 최소 권한만).
   - `res/values/strings.xml`(`app_name`="민수"), `res/values/themes.xml`(`Theme.CafeMinsu`,
     Material3 베이스 — 색은 다음 step에서 토큰으로 오버라이드하므로 여기서는 기본 테마면 충분, **hex 리터럴 금지**).
4. **코드** — `app/src/main/java/com/cafeminsu/`
   - `CafeApplication.kt`: `@HiltAndroidApp class CafeApplication : Application()`.
   - `MainActivity.kt`: `@AndroidEntryPoint`, `setContent { }` 안에 임시 플레이스홀더 `Text("민수")` 한 줄.
     (실제 테마/네비게이션 연결은 step 2·3에서 한다.)

## 하지 말 것
- 화면(M-01~M-10), 디자인 토큰 파일, 네비게이션, 도메인/데이터 코드 생성 금지(다음 step 소관).
- hex 색 리터럴 금지. 비밀키/SDK 절대경로를 추적되는 파일에 넣지 말 것(절대경로는 `local.properties`에만).

## Acceptance Criteria
- `./gradlew :app:assembleDebug` 가 **성공**한다(BUILD SUCCESSFUL). 직접 실행해 확인하라.
- `git status` 기준 `local.properties`·build 산출물은 추적되지 않는다(`.gitignore` 확인).
- 통과하면 `phases/0-bootstrap/index.json`의 step 0 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
