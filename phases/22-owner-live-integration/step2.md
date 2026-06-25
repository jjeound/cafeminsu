# Step 2 — Opt-in 라이브 스모크 테스트 (실서버 계약 검증)

## 배경
기존 `Real*RepositoryTest` 는 **MockWebServer** 로 HTTP/직렬화 계층을 결정론적으로 검증한다(올바른 단위테스트).
이와 **별도로**, 실 `BASE_URL` 서버를 직접 때려 "스펙과 실제 응답이 맞는지" 확인하는 **opt-in 라이브 스모크
테스트**를 추가한다. 평소 빌드/CI(`./gradlew :app:testDebugUnitTest`)에서는 **반드시 skip** 되어야 하며,
명시적 플래그를 줄 때만 실행된다(네트워크·인증·시드데이터 의존이라 기본 그린 유지). 기존 MockWebServer
테스트는 **삭제·변경하지 않는다**.

## 게이팅 규칙 (엄수)
- 실행 트리거: 시스템 프로퍼티 `liveServer=true` **그리고** 베이스 URL 환경값 존재(예: 시스템 프로퍼티
  `liveServer.baseUrl` 또는 환경변수 `LIVE_SERVER_BASE_URL`). 둘 중 하나라도 없으면 **모든 라이브 테스트는
  `org.junit.Assume.assumeTrue(...)` 로 skip**(실패가 아니라 무시).
- 인증이 필요한 엔드포인트는 토큰 환경값(예: 시스템 프로퍼티 `liveServer.token` / 환경변수 `LIVE_SERVER_TOKEN`)이
  있을 때만 실행, 없으면 `assumeTrue` 로 skip. **테스트 코드/로그에 토큰·PII 를 하드코딩하거나 출력하지 않는다**(`SECURITY.md`).
- 라이브 테스트 OkHttp/Retrofit 은 기존 `createRetrofit`/`createOkHttpClient`/`createMoshi`(NetworkModule) 를
  재사용해 **앱과 동일한 직렬화·HTTPS 설정**으로 호출한다. baseUrl 은 환경값에서 읽는다.

## 작업 범위 (이 step에서만)
1. **테스트 헬퍼**: `app/src/test/java/com/cafeminsu/live/LiveServer.kt`
   - `liveServerEnabled(): Boolean`, `liveBaseUrl(): String?`, `liveToken(): String?` 유틸(시스템 프로퍼티 우선, 없으면 env).
   - `assumeLiveServer()` / `assumeLiveAuth()` 헬퍼(`Assume` 기반).
2. **공개 엔드포인트 스모크**: `app/src/test/java/com/cafeminsu/live/PublicEndpointsLiveTest.kt`
   - 각 테스트 첫 줄에 `assumeLiveServer()`. 검증 대상(2xx + DTO 파싱 성공):
     `GET health`, `GET api/stores`(StoreSearchRes), `GET api/stores/nearby` 또는 `GET api/stores/{id}`(StoreDetailRes),
     `GET api/stores/{id}/menus`(List<MenuListItemRes>), `GET api/menus/{id}`(MenuDetailRes).
     id 가 필요한 호출은 앞 호출 결과의 첫 항목 id 를 사용(없으면 `assumeTrue(false)` 로 skip).
   - 단언은 "역직렬화 성공 + 필수 필드 non-null/형식" 수준(특정 데이터 값 하드코딩 금지 — 서버 데이터는 가변).
3. **인증 엔드포인트 스모크**: `app/src/test/java/com/cafeminsu/live/AuthedEndpointsLiveTest.kt`
   - 각 테스트 첫 줄 `assumeLiveAuth()`. 토큰을 Authorization 헤더로 부착해
     `GET api/user/profile`, `GET api/orders/my`, `GET api/notifications` 등 2xx + 파싱만 확인.
4. **문서**: `docs/SERVER_INTEGRATION.md` 하단에 "라이브 스모크 테스트 실행법" 짧은 절 추가:
   ```
   ./gradlew :app:testDebugUnitTest --tests 'com.cafeminsu.live.*' \
     -DliveServer=true -DliveServer.baseUrl=https://… [-DliveServer.token=…]
   ```
   (기본 `:app:testDebugUnitTest` 는 이들을 skip 한다는 점 명시.)

## 금지 / 불변
- 기존 테스트(특히 `Real*RepositoryTest`, MockWebServer)·main 코드를 변경하지 않는다(테스트/문서 추가만).
- 토큰/PII/결제정보 로깅·하드코딩 금지. 라이브 테스트는 **읽기(GET)·검증 전용** — 서버 상태를 바꾸는
  쓰기(POST/PATCH/DELETE)·결제·주문생성은 하지 않는다(부작용 금지).
- 기본 빌드에서 네트워크 호출이 일어나면 안 된다(게이트 미설정 시 전부 skip 확인).

## AC (직접 실행해 확인)
```
# 1) 게이트 미설정 — 라이브 테스트가 전부 skip 되고 기존 단위테스트는 그대로 통과
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
# 2) 라이브 테스트가 컴파일·로드되는지(게이트 off 라 skip)
./gradlew :app:testDebugUnitTest --tests 'com.cafeminsu.live.*'
```
1)이 BUILD SUCCESSFUL 이고 2)에서 라이브 테스트가 skip(no failures)로 끝나면 step 2 를 `completed` + `summary`
로 갱신·커밋. (실 BASE_URL 로의 통과 여부는 사용자가 `-DliveServer=true …` 로 별도 실행해 검증한다.)
