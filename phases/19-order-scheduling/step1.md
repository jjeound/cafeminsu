# Step 1 — 비콘(BLE) 근접 입력을 스케줄러에 연결

고객이 매장에 가까워지는 것을 **비콘(BLE) 근접 신호**로 감지해 step 0 스케줄러의 실시간 입력으로 공급한다.
근접이 높은(곧 도착) 주문은 우선순위가 올라가고 `ArrivingSoon` 뱃지가 붙어 "도착 임박 → 신선하게 제조"
UX를 만든다. **실물 비콘이 없어도 데모되도록 시뮬레이션 구현을 함께 제공**한다.

step 0의 `OrderScheduler`/`SchedulingSignals.proximity`/`ProximityInput`/`SchedulingBadge.ArrivingSoon`를
재사용한다. `docs/SECURITY.md`(권한 최소화·rationale·민감데이터 비로깅), `docs/ARCHITECTURE.md`를 따른다.

## 현재 코드 (반드시 일관 유지)
- in-memory 공유 상태 패턴: `data/repository/SelectedStoreHolder.kt`(싱글톤 `MutableStateFlow`).
- 런타임 권한 패턴(재사용 대상): `ui/feature/voice/VoiceViewModel.kt`(onPermissionResult)와
  `data/voice/AndroidSpeechRecognizer.kt`(`ContextCompat.checkSelfPermission`).
- Mock/Real 게이트 컨벤션: `di/RepositoryModule.kt`. 디스패처 `@IoDispatcher`. 결과 `core/AppResult.kt`.
- minSdk 26 / targetSdk 35. 위치 권한(ACCESS_COARSE/FINE_LOCATION)은 이미 `AndroidManifest.xml`에 선언됨.
- BLE 외부 라이브러리는 추가하지 않는다 — **안드로이드 내장 `android.bluetooth.le.BluetoothLeScanner`** 사용.

## ⚠ TDD — 테스트를 먼저 작성하라 (실패 테스트 우선)
- `ProximitySignalRepository`(in-memory): 신호 publish→observe 흐름, 최신 신호 보존(Turbine).
- `SimulatedProximityScanner`: start 시 근접 신호를 방출, stop 시 중단.
- `OrderSchedulerTest` 보강: proximity가 채워진 주문이 우선순위 상승 + `ArrivingSoon` 뱃지로 분류되는지,
  proximity=null과의 상대 정렬.
- `OwnerOrdersViewModelTest` 보강: `ProximitySignalRepository`에 신호가 들어오면 해당 주문이 위로
  올라오고 뱃지/ETA가 갱신되는지(가짜 repository 주입).
- **BLE 실제 스캔(`AndroidBeaconScanner`)은 에뮬레이터에서 검증 불가** → 권한 가드/콜백 매핑 로직만 단위로,
  실제 스캔 동작은 실기기 수동 검증(테스트 강제하지 않음).

## 만들 것
### 1) 도메인 — `domain/proximity/`
- `ProximitySignal(orderId: String, rssi: Int, estimatedArrivalSeconds: Int, atMillis: Long)`.
- `interface ProximityScanner { fun observe(): Flow<AppResult<ProximitySignal>>; suspend fun start():
  AppResult<Unit>; suspend fun stop() }`.
- `ProximitySignalRepository`(in-memory, `SelectedStoreHolder` 패턴): 최근 신호들을
  `Map<orderId, ProximitySignal>`로 보관하는 `MutableStateFlow`와 publish/observe API. 스케줄러가
  여기서 읽어 `SchedulingSignals.proximity`(=`ProximityInput`)로 변환한다.
- RSSI/도착초 → `ProximityInput(estimatedArrivalSeconds, rssi)` 매핑 헬퍼(순수 함수, 단위 테스트).

### 2) 데이터 구현 — `data/proximity/`
- `AndroidBeaconScanner`(`ProximityScanner` 구현): `@ApplicationContext` + `@IoDispatcher` 주입.
  `BluetoothLeScanner`로 BLE 스캔, `ScanResult.rssi`로 근접 추정. **권한·블루투스 비활성·하드웨어 없음**은
  `AppResult.Failure`(또는 빈 흐름)로 안전 처리하고 절대 예외 전파/크래시 금지. 근접 RSSI/식별자는 **로깅 금지**.
- `SimulatedProximityScanner`(`ProximityScanner` 구현): 데모/에뮬레이터용. start 시 일정 간격으로
  "도착 임박"에 가까워지는 신호를 방출(데모 토글). 실물·권한 불필요.
- `di/ProximityModule.kt`: 기본은 **시뮬레이터 바인딩**(에뮬레이터/CI 안전), 실제 BLE는 빌드플래그/가용성으로
  교체 가능하게(주석으로 전환 방법 명시). `ProximitySignalRepository`는 `@Singleton`.

### 3) 권한 — `AndroidManifest.xml`
- 추가:
  - `BLUETOOTH`(`android:maxSdkVersion="30"`), `BLUETOOTH_ADMIN`(`android:maxSdkVersion="30"`),
  - `BLUETOOTH_SCAN`(`android:usesPermissionFlags="neverForLocation"`), `BLUETOOTH_CONNECT`.
- 런타임 요청: **음성 권한 패턴 재사용**. SDK 31+는 `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`, 그 이하는
  위치 권한으로 스캔 가능. **거부 시 graceful fallback**(시뮬레이터/근접 없이 FIFO 유사 동작) — 크래시 금지,
  rationale 문구는 한국어.

### 4) 통합 — `OwnerOrdersViewModel`
- `ProximitySignalRepository`를 주입하고 주문 흐름과 `combine`해, 각 주문의 `SchedulingSignals.proximity`를
  최신 근접 신호로 채운다. 근접이 임박이면 step 0 규칙에 따라 우선순위↑ + `ArrivingSoon` 뱃지.
- 화면은 `ArrivingSoon` 뱃지를 "도착 임박" 같은 한국어 라벨로 노출(디자인 토큰만, hex 금지).

### 5) 보안 문서 — `docs/SECURITY.md`
- BLE 근접 기능 절 추가: 권한 최소화·rationale, 근접/식별 데이터 **비로깅**, 비콘에 민감정보 미탑재(참조 ID만),
  거부 시 안전 폴백 원칙.

## 하지 말 것
- 외부 비콘/BLE 라이브러리 추가 금지(내장 API만). AI/LLM 코드 금지(다음 step).
- 근접 RSSI·기기 식별자·주문 식별자 로그 노출 금지(릴리스). 새 결과 타입 금지(예외→`AppResult`).
- step 0의 스케줄러 점수식/모델 시그니처 변경 금지(이 step은 proximity 슬롯을 **채우는** 배선만).
- hex 리터럴·임의 색/토큰·매직넘버 금지. 위치/블루투스 권한을 과도하게 요구하지 말 것(최소 범위).

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공
  (`ProximitySignalRepository`/시뮬레이터/스케줄러 proximity/ViewModel 보강 테스트 + 기존 무파손).
- 근접 신호가 들어오면 해당 주문이 우선순위 상단으로 이동하고 `ArrivingSoon` 뱃지가 표시됨을 테스트로 확인.
- 에뮬레이터/권한 거부 시에도 크래시 없이 폴백 동작함을 확인.
- 통과하면 step 1 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
