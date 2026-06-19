# Step 1 — 코어 결과/에러 타입 (도메인, TDD)

앱 전체가 공유하는 **결과 래퍼**와 **표준 도메인 에러**를 만든다. 이것은 순수 Kotlin 도메인 코드로,
Android 프레임워크에 의존하지 않는다. `ARCHITECTURE.md`의 "에러 처리 & 회복" 절이 단일 진실이다.

## ⚠ TDD — 테스트를 먼저 작성하라
이 step은 **도메인 로직**이므로 CLAUDE.md의 "테스트 우선" 규칙이 엄격히 적용된다.
**구현 파일(`src/main`)보다 먼저 `src/test`에 실패하는 테스트를 작성**하고, 그 다음 구현하라.

## 만들 것 (src/main — 테스트 작성 후)
`app/src/main/java/com/cafeminsu/core/` (또는 `core/result`)에:

```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: DomainError) : AppResult<Nothing>
}

sealed interface DomainError {
    data object Network : DomainError
    data object Timeout : DomainError
    data object Unauthorized : DomainError
    data object NotFound : DomainError
    data class Payment(val reason: String) : DomainError
    data class Validation(val field: String) : DomainError
    data object Unknown : DomainError
}
```

추가로 호출부 편의를 위한 **순수 함수 헬퍼**(인라인 권장)를 둔다. 최소:
- `map(transform: (T) -> R): AppResult<R>` — Success면 변환, Failure면 그대로 통과.
- `fold(onSuccess, onFailure)` — 두 경로를 하나의 값으로 접는다.
- `getOrNull(): T?` 및 `isSuccess`/`isFailure`.

## 테스트 (src/test, JUnit4) — 먼저 작성
`app/src/test/java/com/cafeminsu/core/AppResultTest.kt` 등에서 다음을 검증:
- `Success.map`은 값을 변환하고, `Failure.map`은 같은 `DomainError`를 보존한다.
- `fold`가 Success/Failure 각각의 람다를 호출한다.
- `getOrNull`이 Success에서 값을, Failure에서 `null`을 반환한다.
- `DomainError`의 각 variant가 구분된다(예: `Payment("x") != Payment("y")`, data class 동등성).

## 하지 말 것
- Android import(`android.*`, `androidx.*`) 사용 금지 — 순수 Kotlin이어야 한다.
- 예외를 던지는 API로 만들지 마라(에러는 `Failure(DomainError)`로 표현).
- 데이터/UI/네비게이션 코드 생성 금지(이 step 범위 밖).

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 가 **성공**한다(작성한 테스트가 모두 green). 직접 실행해 확인하라.
- 테스트 파일이 구현 파일과 함께 커밋된다.
- 통과하면 `phases/0-bootstrap/index.json`의 step 1 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
