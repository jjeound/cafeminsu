# Step 2 — AI 예측 보정 레이어 (선택, Optional Layer)

스케줄러의 입력값(제조시간·혼잡도·도착확률)을 **온디바이스 AI로 더 정확히 추정**해 보정한다.
**AI는 의사결정 주체가 아니다** — step 0 규칙 엔진의 결정 로직은 그대로 두고, AI는 입력 추정치만 제공한다.
**모델이 없거나 추론이 실패하면 step 0의 규칙 추정치로 자동 폴백**(blocked가 아니라 정상 폴백)한다.

음성주문이 쓰는 온디바이스 Gemma 인프라(`domain/voice/VoiceOrderInterpreter.kt`의 `VoiceLlmEngine`,
`data/voice/GemmaLlmEngine.kt`, `data/voice/GemmaVoiceOrderInterpreter.kt`)의 **패턴을 재사용**한다.

## ⚠ TDD — 테스트를 먼저 작성하라 (실패 테스트 우선)
- `GemmaOrderMetricsPredictor`: 가짜 `VoiceLlmEngine` 주입해
  - `isReady()==false` → **규칙 추정치로 폴백**(예외/실패 없이).
  - 정상 JSON 응답 → 파싱된 추정치 반환(prepSeconds 등).
  - 깨진/비JSON 응답 → `AppResult.Failure`로 감싸고 **규칙 폴백**.
- `PrepTimeEstimator`(AI 우선·규칙 폴백 구현): 모델 미가용 시 step 0 `RulePrepTimeEstimator`와 동일 결과.
- 실제 모델 추론 e2e는 이 환경에서 불가 → **엔진을 mock**해 로직만 검증한다.

## 만들 것
### 1) 도메인 인터페이스 — `domain/scheduling/OrderMetricsPredictor.kt`
- `interface OrderMetricsPredictor {`
  - `suspend fun estimatePrepSeconds(order: Order, congestion: CongestionLevel): AppResult<Int>`
  - `suspend fun predictCongestion(recentOrders: List<Order>, nowMillis: Long): AppResult<CongestionLevel>`
  - `suspend fun estimateArrivalProbability(signal: ProximitySignal): AppResult<Double>`  // 0.0..1.0
  `}`
- 모든 함수는 보정용 추정치만 반환. 실패는 `AppResult.Failure`로(호출측이 규칙 폴백).

### 2) 데이터 구현 — `data/scheduling/GemmaOrderMetricsPredictor.kt`
- `VoiceLlmEngine` + `@IoDispatcher`(+ Moshi) 주입. `GemmaVoiceOrderInterpreter` 방식 그대로:
  buildPrompt(주문/혼잡/근접 컨텍스트를 구조화 + JSON 출력 지시) → `engine.generate` → 코드펜스 제거 →
  Moshi 파싱 → 도메인 값으로 검증. `isReady()` false거나 파싱 실패 시 **규칙 폴백 값**.
- 추론 입력/출력에 PII·주문 민감정보 로깅 금지.

### 3) `PrepTimeEstimator`를 AI 우선·규칙 폴백으로 교체
- step 0의 `PrepTimeEstimator` 인터페이스를 구현하는 `AiPrepTimeEstimator`(또는 데코레이터):
  `OrderMetricsPredictor.estimatePrepSeconds`를 우선 시도하고 실패/미가용 시 `RulePrepTimeEstimator`로 폴백.
- `di/SchedulingModule.kt`에서 `PrepTimeEstimator` 바인딩을 AI 우선 구현으로 바꾼다(규칙 구현은 폴백으로 유지).
  **모델이 없는 기본 상태에서도 빌드·테스트·런타임이 규칙 폴백으로 정상 동작**해야 한다.

### 4) (선택) 혼잡도·도착확률 보정 연결
- 가능하면 `CongestionCalculator`/proximity 경로에도 `OrderMetricsPredictor`를 **보정 옵션으로** 연결하되,
  실패/미가용 시 step 0·step 1의 규칙 동작과 동일해야 한다. 무리하게 범위를 넓히지 말 것.

## 하지 말 것
- step 0 규칙 엔진의 **결정 로직(점수식·정렬)** 변경 금지 — AI는 입력 추정치만 보정한다.
- 새 모델 파일 다운로더/네트워크 모델 로딩 추가 금지(음성과 동일하게 `VoiceModelProvider` 전제, 범위 밖).
- 모델 미탑재를 이유로 step을 `blocked` 처리하지 마라 — **규칙 폴백으로 동작**시키고 `completed`로 끝낸다.
- 외부 AI 클라우드 호출·API 키 도입 금지(온디바이스만). PII/주문정보 로깅 금지. 새 결과 타입 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공
  (predictor 폴백/파싱/AI우선-규칙폴백 테스트 + 기존 무파손).
- 모델 미가용(기본) 상태에서 스케줄링이 step 0 규칙값과 동일하게 정상 동작함을 테스트로 확인.
- 통과하면 step 2 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
