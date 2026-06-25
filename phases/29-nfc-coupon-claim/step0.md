# Step 0: remove-beacon-proximity (비콘 근접 스케줄링 제거)

손님용 NFC 쿠폰 발급으로 대체하기에 앞서, **비콘(BLE) 근접 신호를 주문 스케줄링에 배선했던 코드(phase
19-order-scheduling step1 산출물)를 모두 제거**한다. 비콘 스캐너 소스 레이어·DI·매니페스트 BLE 권한·점주
주문 화면 배선·관련 테스트를 삭제한다. **순수 스케줄링 엔진(`domain/scheduling`)과 AI 보정 레이어는 그대로
둔다** — 비콘이라는 *신호원*만 없애고, 스케줄러는 비콘 입력 없이(근접 null) 정상 동작해야 한다.

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md`(레이어/DI), `/docs/SECURITY.md`(§9 비콘 항목 — 제거 대상)
- `phases/19-order-scheduling/index.json`(step1 `beacon-proximity` 산출물 요약 — 무엇을 만들었는지 확인)
- `app/src/main/java/com/cafeminsu/domain/proximity/ProximityModels.kt`(ProximitySignal·ProximityScanner·toProximityInput)
- `app/src/main/java/com/cafeminsu/domain/proximity/ProximitySignalRepository.kt`
- `app/src/main/java/com/cafeminsu/data/proximity/AndroidBeaconScanner.kt`,
  `app/src/main/java/com/cafeminsu/data/proximity/SimulatedProximityScanner.kt`
- `app/src/main/java/com/cafeminsu/di/ProximityModule.kt`
- `app/src/main/java/com/cafeminsu/ui/feature/owner/orders/OwnerOrdersViewModel.kt`(근접 배선 지점)
- `app/src/main/java/com/cafeminsu/domain/scheduling/OrderScheduler.kt`,
  `app/src/main/java/com/cafeminsu/domain/scheduling/SchedulingModels.kt`(여기 `ProximityInput` 은 **남겨둔다**)
- `app/src/main/AndroidManifest.xml`(BLUETOOTH* 권한)
- 관련 테스트: `app/src/test/java/com/cafeminsu/ui/feature/owner/orders/OwnerOrdersViewModelTest.kt`,
  `app/src/test/java/com/cafeminsu/domain/proximity/*`, `app/src/test/java/com/cafeminsu/data/proximity/*`,
  `app/src/test/java/com/cafeminsu/di/ProximityModuleTest.kt`

## 작업

1. **소스 삭제** — 아래 파일·디렉토리를 삭제한다:
   - `app/src/main/java/com/cafeminsu/domain/proximity/` 전체(`ProximityModels.kt`, `ProximitySignalRepository.kt`)
   - `app/src/main/java/com/cafeminsu/data/proximity/` 전체(`AndroidBeaconScanner.kt`, `SimulatedProximityScanner.kt`)
   - `app/src/main/java/com/cafeminsu/di/ProximityModule.kt`

2. **테스트 삭제** — 위 소스에 대응하는 테스트 삭제:
   - `app/src/test/java/com/cafeminsu/domain/proximity/` 전체
   - `app/src/test/java/com/cafeminsu/data/proximity/` 전체
   - `app/src/test/java/com/cafeminsu/di/ProximityModuleTest.kt`

3. **점주 주문 ViewModel 배선 제거** — `OwnerOrdersViewModel`:
   - 생성자에서 `proximitySignalRepository: ProximitySignalRepository` 주입 제거.
   - `combine(...)` 의 `proximitySignalRepository.observe()` 슬롯과 `proximitySignals` 파라미터 제거(combine 항목 5→4).
   - `mapOwnerOrdersState(...)` 의 `proximitySignals` 파라미터와 그 사용 제거.
   - `order.toSchedulingSignals(...)` 호출에서 `proximity = ...` 인자 제거. `toSchedulingSignals` 헬퍼의
     `proximity: ProximityInput?` 파라미터를 제거하고, `SchedulingSignals(...)` 생성 시 `proximity` 는
     기본값(null)에 맡긴다(인자 생략).
   - `import com.cafeminsu.domain.proximity.*`(ProximitySignal, ProximitySignalRepository, toProximityInput)
     제거. `ProximityInput` 임포트는 더 이상 필요 없으면 제거.

4. **ViewModel 테스트 정리** — `OwnerOrdersViewModelTest`:
   - `arrivingProximitySignalLiftsOrderToTopAndMarksArrivingSoon` 테스트와 `ProximitySignal`/
     `ProximitySignalRepository` 임포트를 삭제.
   - `ownerOrdersViewModel(...)` 헬퍼의 `proximitySignalRepository` 파라미터와 생성자 전달 인자 제거.
   - 나머지 정렬/뱃지 테스트(대기시간 기반 Urgent 등)는 유지·통과시킨다.

5. **매니페스트** — `AndroidManifest.xml` 에서 BLE 권한 4종과 그 주석을 제거:
   `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`.

6. **문서** — `docs/SECURITY.md` 에 비콘(§9 등) 항목이 있으면 제거하거나 "제거됨" 표기. (문서는 TDD 대상 아님.)

### 핵심 규칙 (반드시 준수)

- `domain/scheduling` 의 `OrderScheduler`·`SchedulingModels`(`ProximityInput`, `proximityWeight`,
  `SchedulingBadge.ArrivingSoon`)·`SchedulingWeights`·AI 예측(`*MetricsPredictor`, `AiPrepTimeEstimator`)은
  **수정·삭제하지 않는다.** 비콘 신호원만 사라지고 스케줄러는 근접 입력 없이(항상 null) 그대로 동작한다.
- 스케줄러 코어 테스트(`OrderSchedulerTest`, `SchedulingModelsTest`, `di/SchedulingModuleTest`,
  `OrderMetricsPredictorTest`, `AiPrepTimeEstimatorTest`, `GemmaOrderMetricsPredictorTest`)는 **무회귀**여야 한다.
- 삭제 후 어떤 파일도 `com.cafeminsu.domain.proximity` / `com.cafeminsu.data.proximity` 를 참조하지 않아야 한다.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

- 빌드·테스트 통과. `proximity`/`Beacon` 잔존 참조 0건(`grep -ri proximity app/src/main app/src/test` → 스케줄링
  도메인의 남겨둔 `ProximityInput` 외 비콘 신호원 참조 없음).

## 검증 절차

1. 위 AC 명령 통과. 컴파일 에러(미사용 임포트/끊긴 참조) 없음.
2. `git grep -n "domain.proximity\|data.proximity\|ProximitySignalRepository\|BeaconScanner\|BLUETOOTH"` 결과가
   비어있는지 확인.
3. `phases/29-nfc-coupon-claim/index.json` step 0 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "비콘(BLE) 근접 스캐너 소스·DI·BLE 권한·점주주문 배선·관련 테스트 제거. 순수 OrderScheduler/AI 보정은 유지(근접 입력 없이 동작), 스케줄러 코어 테스트 무회귀."`
   - 3회 실패 → `"status": "error"` + `"error_message"` / 사용자 개입 필요 → `"status": "blocked"` + `"blocked_reason"`

## 금지사항

- 스케줄링 엔진/AI 보정 로직과 그 테스트를 건드리지 마라(`ProximityInput` 타입은 남긴다). 이유: 범위 밖·무회귀.
- NFC 관련 신규 파일을 만들지 마라(step1~3 범위). 이 step 은 **제거만** 한다.
