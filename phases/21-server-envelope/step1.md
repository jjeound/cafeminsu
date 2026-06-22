# Step 1 — 봉투 제거 (리워드·기프티콘·알림·FCM) + 공통 봉투 코드 삭제

## 배경
step 0 에서 인증/매장/메뉴/주문/결제 도메인의 응답 봉투를 제거했다(계약은 `docs/SERVER_INTEGRATION.md`
"응답 구조" 절). 이 step 은 **나머지 도메인**(스탬프/기프티콘/알림/FCM)의 봉투 의존을 제거하고,
더 이상 어디서도 쓰지 않게 된 **공통 봉투 코드 자체를 삭제**한다. 변환 규칙은 step 0 와 동일하다.

## 작업 범위 (이 파일들만)
**API (반환 타입 `BaseResponse<T>` → `T`, 목록은 `List<T>`)**
- `app/src/main/java/com/cafeminsu/data/remote/StampApi.kt`
- `app/src/main/java/com/cafeminsu/data/remote/GifticonApi.kt`
- `app/src/main/java/com/cafeminsu/data/remote/NotificationApi.kt`
- `app/src/main/java/com/cafeminsu/data/remote/FcmTokenApi.kt`

**Repository (봉투 언랩 제거)**
- `app/src/main/java/com/cafeminsu/data/repository/RealRewardRepository.kt`
- `app/src/main/java/com/cafeminsu/data/repository/RealGiftRepository.kt`
- `app/src/main/java/com/cafeminsu/data/repository/RealNotificationRepository.kt`
- `app/src/main/java/com/cafeminsu/data/repository/RealFcmTokenRepository.kt`

**공통 봉투 코드 삭제** (`app/src/main/java/com/cafeminsu/data/remote/AuthApi.kt`)
- 위 변환을 끝내 **프로젝트 전체에서 `BaseResponse`/`.unwrap` 사용처가 0 이 된 것을 확인한 뒤**,
  AuthApi.kt 하단의 `data class BaseResponse<T>`, `fun <T,R> BaseResponse<T>.unwrap(...)`,
  `private fun Int?.toDomainErrorOrUnknown()` 정의를 **삭제**한다.
  (확인: `grep -rn "BaseResponse\|\.unwrap" app/src/main` 결과가 비어야 한다.)

**테스트 (봉투 픽스처 → bare DTO)**
- `app/src/test/java/com/cafeminsu/data/repository/RealRewardRepositoryTest.kt`
- `app/src/test/java/com/cafeminsu/data/repository/RealGiftRepositoryTest.kt`
- `app/src/test/java/com/cafeminsu/data/repository/RealNotificationRepositoryTest.kt`
- `app/src/test/java/com/cafeminsu/data/repository/RealFcmTokenRepositoryTest.kt`
- `app/src/test/java/com/cafeminsu/data/remote/RemoteResultTest.kt`
  — 봉투 형태(`{"isSuccess":false,...}`)의 본문 문자열이 있으면 bare DTO/빈 본문으로 정리한다
  (이 테스트의 의도는 HTTP 상태→`DomainError` 매핑이므로 `setResponseCode` 로 신호하고 본문은 단순화).

## 변환 규칙 (step 0 와 동일, 요약)
1. API 반환 `): BaseResponse<Foo>` → `): Foo`, 목록은 `): BaseResponse<List<Foo>>` → `): List<Foo>`.
2. Repository: `response.data.unwrap { it.toBar() }` → `response.data.toBar()` (DTO 에 매퍼 직접 적용,
   매퍼는 그대로 `AppResult<…>` 반환). 안 쓰이는 `unwrap` import 제거.
3. 실패는 HTTP 상태로만(`runCatchingToAppResult` 가 `HttpException`→`DomainError`). 본문 `isSuccess` 판정 금지.
4. 테스트 본문: 봉투 벗겨 bare DTO/배열. `"isSuccess": false`@200 → `MockResponse().setResponseCode(<4xx/5xx>)`
   로 같은 `DomainError`(401→Unauthorized, 404→NotFound, 그 외→Unknown) 유발.

## 금지 / 불변
- 도메인 모델, Repository 인터페이스 시그니처, 매퍼 변환 로직, UI, DTO 필드, Mock 리포, DI 키게이트는 무변경.
- 민감값(바코드/QR/PG 토큰) 미로깅·미복사 등 보안 규칙 유지(`SECURITY.md`).

## AC (직접 실행해 BUILD SUCCESSFUL 확인)
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
또한 `grep -rn "BaseResponse\|\.unwrap" app/src/main` 가 **빈 결과**여야 한다.
통과하면 `phases/21-server-envelope/index.json` 의 step 1 status 를 `completed` + `summary` 한 줄로 갱신하고 커밋한다.
