# Step 1 — STT 추상화 + 마이크 권한 (온디바이스)

음성 인식(STT)을 **추상화 인터페이스**로 두고 Android 온디바이스 구현을 만든다. 테스트/교체를 위해 도메인은
프레임워크에 의존하지 않게 한다. ADR-004의 **온디바이스 `SpeechRecognizer` 경로**(키 불필요 → blocked 아님).
권한은 `SECURITY.md §8`(런타임 요청 + rationale, 사용 시점에만, 거부 시 대체 경로).

## 만들 것
1. **추상화 인터페이스** (`domain` 또는 `core`, 프레임워크 비종속):
   - `interface SpeechRecognizer`(이름 충돌 피하려면 `VoiceRecognizer`):
     `fun start()`, `fun stop()`, 그리고 인식 결과를 흘리는 `Flow`:
     `val events: Flow<VoiceRecognitionEvent>` 또는 콜백. `VoiceRecognitionEvent`: `Partial(text)`,
     `Final(text)`, `Error(reason)`, `EndOfSpeech` 등 sealed. (transcript interim/final 구분 가능해야 함.)
2. **Android 온디바이스 구현** — `data` 또는 `platform` 레이어:
   - `AndroidSpeechRecognizer` : `android.speech.SpeechRecognizer` + `RecognizerIntent`(언어 `ko-KR`,
     partial results on) 래핑 → `VoiceRecognitionEvent`로 변환. 리소스 해제(`destroy`) 처리.
   - 디바이스에 STT 미지원/오류 시 `Error` 이벤트로 (크래시 금지).
3. **권한** — `AndroidManifest.xml`에 `<uses-permission android:name="android.permission.RECORD_AUDIO"/>` 추가.
   런타임 권한 요청 + rationale 헬퍼(Compose `rememberLauncherForActivityResult` 등)는 화면 step에서 쓰되,
   이 step에서 권한 상태 점검 유틸을 제공해도 된다. **다른 권한은 추가하지 마라.**
4. **DI** — `VoiceRecognizer` 바인딩(`@Binds`/`@Provides`)을 Hilt 모듈에 추가(Android 구현 연결). 테스트에서 fake로
   대체 가능하게.

## 검증
- `AndroidSpeechRecognizer`는 계측 환경이 없으면 런타임 검증 불가 → **컴파일 + 인터페이스 단위 테스트**(fake 구현으로
   이벤트 흐름 검증)로 갈음.

## 하지 말 것
- 클라우드 STT/LLM·네트워크 키 사용 금지(온디바이스만). M-04 화면/VM은 다음 step. hex/새 토큰 금지.
- RECORD_AUDIO 외 권한 선언 금지.

## Acceptance Criteria
- `./gradlew :app:assembleDebug` + `:app:testDebugUnitTest` 성공. 직접 실행해 확인하라.
- `AndroidManifest.xml`에 RECORD_AUDIO가 선언되고, 그 외 권한은 추가되지 않았다.
- 통과하면 `phases/8-voice/index.json`의 step 1 status를 `completed` + `summary` 기록.
