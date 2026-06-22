# Step 0 — 봉투 제거 (인증·매장·메뉴·주문·결제)

## 배경
서버가 공통 응답 봉투 `BaseResponse<T>`(`{ isSuccess, code, message, result }`)를 **폐지**했다.
이제 모든 API가 **DTO(`T`)를 본문에 직접** 반환하고, 목록은 **최상위 JSON 배열**(`[...]`)이며,
실패는 **HTTP 상태 코드로만** 신호한다(본문 `isSuccess:false` 케이스 없음). 자세한 계약은
`docs/SERVER_INTEGRATION.md` "응답 구조" 절을 따른다. 현재 코드는 봉투를 가정하므로 실서버에서 전부 실패한다.

이 step은 **인증/매장/메뉴/주문/결제** 도메인의 봉투 의존을 제거한다. (스탬프/기프티콘/알림/FCM 은 step 1.)

## 작업 범위 (이 파일들만)
**API (반환 타입 `BaseResponse<T>` → `T`, 목록은 `List<T>`)**
- `app/src/main/java/com/cafeminsu/data/remote/AuthApi.kt`
  — `kakaoLogin`/`ownerLogin`/`refresh`/`getMyProfile`/`checkNickname`/`signup` 의 반환 타입에서 `BaseResponse<...>` 래퍼 제거.
  - ⚠ **이 파일 하단의 `data class BaseResponse<T>`, `fun <T,R> BaseResponse<T>.unwrap(...)`,
    `private fun Int?.toDomainErrorOrUnknown()` 정의는 삭제하지 말고 그대로 둔다** — step 1 의 다른 API 가
    아직 사용한다. (step 1 에서 마지막에 제거한다.)
- `app/src/main/java/com/cafeminsu/data/remote/StoreApi.kt` — `searchStores`→`StoreSearchRes`, `getStore`→`StoreDetailRes`.
- `app/src/main/java/com/cafeminsu/data/remote/MenuApi.kt`
- `app/src/main/java/com/cafeminsu/data/remote/OrderApi.kt`
- `app/src/main/java/com/cafeminsu/data/remote/PaymentApi.kt`
  - 목록 반환 메서드는 `BaseResponse<List<X>>` → `List<X>` 로 바꾼다.

**Repository / Provider (봉투 언랩 제거)**
- `app/src/main/java/com/cafeminsu/data/repository/RealSessionRepository.kt`
- `app/src/main/java/com/cafeminsu/data/repository/RealStoreRepository.kt`
- `app/src/main/java/com/cafeminsu/data/repository/RealMenuRepository.kt`
- `app/src/main/java/com/cafeminsu/data/repository/RealOrderRepository.kt`
- `app/src/main/java/com/cafeminsu/data/repository/RealPaymentRepository.kt`
- `app/src/main/java/com/cafeminsu/data/auth/RealOwnerAuthProvider.kt`

**테스트 (봉투 픽스처 → bare DTO)**
- `app/src/test/java/com/cafeminsu/data/remote/AuthApiTest.kt`
- `app/src/test/java/com/cafeminsu/data/auth/RealOwnerAuthProviderTest.kt`
- `app/src/test/java/com/cafeminsu/data/repository/RealSessionRepositoryTest.kt`
- `app/src/test/java/com/cafeminsu/data/repository/RealStoreRepositoryTest.kt`
- `app/src/test/java/com/cafeminsu/data/repository/RealMenuRepositoryTest.kt`
- `app/src/test/java/com/cafeminsu/data/repository/RealOrderRepositoryTest.kt`
- `app/src/test/java/com/cafeminsu/data/repository/RealPaymentRepositoryTest.kt`

## 변환 규칙 (정확히 따를 것)
1. **API 반환 타입**: `): BaseResponse<Foo>` → `): Foo`. 목록은 `): BaseResponse<List<Foo>>` → `): List<Foo>`.
2. **Repository 호출부**: 기존 패턴
   ```kotlin
   when (val response = runCatchingToAppResult { api.foo(...) }) {
       is AppResult.Success -> response.data.unwrap { it.toBar() }
       is AppResult.Failure -> response
   }
   ```
   에서 `response.data` 가 이제 봉투가 아니라 DTO 그 자체다. `.unwrap { it.toBar() }` 를 제거하고
   **람다 본문을 DTO 에 직접 적용**한다:
   ```kotlin
   is AppResult.Success -> response.data.toBar()   // toBar() 는 그대로 AppResult<…> 반환
   ```
   `.unwrap { ... }` 의 람다는 이미 `AppResult<R>` 를 반환하므로 동작/시그니처는 그대로 보존된다.
   `import ...unwrap` 가 더 이상 안 쓰이면 제거한다.
3. **에러 신호**: 비-2xx 는 `runCatchingToAppResult` 가 `HttpException`→`DomainError` 로 잡는다.
   봉투 기반 실패 판정(`isSuccess`/`code` 본문)은 **쓰지 않는다**.
4. **테스트 본문**: MockWebServer 의 `setBody`/`enqueue` 본문에서 봉투를 벗긴다 —
   `{ "isSuccess": true, "code": 200, "message": "...", "result": <DTO> }` → `<DTO>` 만, `"result": [ ... ]` → `[ ... ]` 만.
   `"isSuccess": false` 로 HTTP 200 실패를 흉내내던 케이스는 **`MockResponse().setResponseCode(<4xx/5xx>)`**
   로 바꿔 같은 `DomainError`(테스트가 단언하는 값: 401→Unauthorized, 404→NotFound, 그 외→Unknown)를 유발한다.
5. `AuthApiTest.kt` 에 `response.unwrap { it.toLoginExchange() }` 처럼 테스트가 직접 `unwrap` 을 호출하는 곳은
   `response.toLoginExchange()`(response 가 이제 DTO) 로 바꾼다.

## 금지 / 불변 (반드시)
- 도메인 모델, Repository **인터페이스 시그니처**, 매퍼(`data/mapper/*`)의 변환 로직, UI 는 **변경하지 않는다**.
- DTO 데이터클래스의 필드는 바꾸지 않는다(서버 내부 DTO 는 동일). Mock 리포지토리·DI 키게이트도 무변경.
- 결제 낙관 금지·민감값 미로깅 등 보안 규칙 유지(`SECURITY.md`).
- step 1 가 쓸 `BaseResponse`/`unwrap`/`toDomainErrorOrUnknown` 정의는 이 step 에서 삭제 금지.

## AC (직접 실행해 BUILD SUCCESSFUL 확인)
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과하면 `phases/21-server-envelope/index.json` 의 step 0 status 를 `completed` + `summary` 한 줄로 갱신하고 커밋한다.
