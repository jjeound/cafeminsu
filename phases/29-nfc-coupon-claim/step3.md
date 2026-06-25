# Step 3: nfc-reader-screen-nav (NFC 읽기 + 발급 화면 + 네비 + 기프티콘 새로고침)

손님용 NFC 쿠폰 발급의 3단계(마무리). `NfcAdapter` **reader mode** 로 NDEF 태그를 읽어 raw 문자열을 뽑고,
step2 의 `NfcClaimViewModel.onTagRead` 로 넘겨 발급한다. 발급 화면·라우트·매니페스트(NFC)·진입점·성공 후
**기프티콘 목록 새로고침**까지 연결한다.

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md`(네비/화면), `/docs/UI_GUIDE.md`, `/docs/DESIGN_SYSTEM.md`(토큰만·한국어, anti-slop),
  `/docs/SECURITY.md`(§6 입력 검증·최소 권한·rationale)
- `phases/29-nfc-coupon-claim/step2.md` 산출물(`NfcClaimViewModel`/`NfcClaimUiState`/`NfcClaimEvent`),
  step1(`NfcTagCode.parse`)
- `app/src/main/java/com/cafeminsu/ui/feature/gift/claim/GiftClaimScreen.kt`(Route+Screen+이벤트 토스트→이동 패턴 — 그대로 따른다)
- `app/src/main/java/com/cafeminsu/ui/navigation/Routes.kt`, `ui/navigation/AppNavHost.kt`(라우트/composable 등록·이동)
- `app/src/main/java/com/cafeminsu/ui/feature/gifticon/GifticonScreen.kt`,`GifticonViewModel.kt`
  (발급 후 이동 대상 = 내 기프티콘 목록; `observeGifticons()` 재구독 시 재조회되어 목록이 갱신됨)
- `app/src/main/java/com/cafeminsu/ui/components/`(CafeButton/CafeTopBar/상태뷰 등 재사용 컴포넌트)
- `app/src/main/java/com/cafeminsu/MainActivity.kt`(호스트 Activity — reader mode 는 Activity 필요)
- `app/src/main/AndroidManifest.xml`(권한/feature 추가 위치)

## 작업

1. **NDEF 파서(안드로이드 글루)** — `ui/feature/nfc/NfcNdefParser.kt`(또는 인접):
   - `NdefMessage`/`NdefRecord` → raw 문자열 추출:
     - **URI 레코드**(TNF well-known, RTD_URI): `NdefRecord.toUri()` 사용 → URI 문자열.
     - **Text 레코드**(RTD_TEXT): status 바이트로 언어코드 길이 파싱 후 본문(UTF-8/UTF-16) 디코드.
     - 첫 유효 레코드의 문자열을 반환(없으면 null). 추출된 raw 는 step1 `NfcTagCode.parse` 로 코드화한다.
   - 예외 안전(깨진 태그/지원안되는 레코드 → null, 크래시 금지).

2. **Reader mode 글루** — Compose 에서 호스트 Activity 에 reader mode 를 붙였다 떼는 효과
   (`ui/feature/nfc/NfcReaderEffect.kt` 등):
   - `NfcAdapter.getDefaultAdapter(context)` 로 가용성 판단:
     - **null** → NFC 미지원 기기. 화면은 태깅 안내 대신 "이 기기는 NFC를 지원하지 않아요" 표시(발급 진입 비활성).
     - 어댑터 있으나 `isEnabled == false` → "NFC가 꺼져 있어요" + `Settings.ACTION_NFC_SETTINGS` 로 설정 이동 버튼.
   - 화면 활성 동안 `DisposableEffect` 로 `enableReaderMode(activity, callback, flags, extras)` 호출,
     `onDispose` 에서 `disableReaderMode(activity)`. flags: `FLAG_READER_NFC_A or NFC_B or NFC_F or NFC_V`
     (+ NDEF 처리). callback(`onTagDiscovered`)에서 `Ndef.get(tag)` → `connect()` → `ndefMessage`(또는
     `cachedNdefMessage`) 읽고 **`finally`로 close** → `NfcNdefParser` 로 raw 추출 → **메인 스레드로** 코드를
     `viewModel.onTagRead(raw)` 전달. IO/예외는 안전 처리(크래시·토큰로깅 금지).
   - **따닥 가드**: 콜백에서 직접 호출해도 ViewModel 의 진행중 가드(step2)가 중복을 막는다. 추가로 동일 태그
     연속 인식 디바운스(짧은 시간창)를 둘 수 있다.

3. **발급 화면** — `ui/feature/nfc/NfcClaimScreen.kt` + `NfcClaimRoute`:
   - `CafeTopBar`("NFC 쿠폰 받기") + 본문: 태깅 안내(폰을 매장 NFC 태그에 대주세요), 진행중 표시(`claiming`),
     인라인 에러(`errorMessage`), NFC 미지원/비활성 분기 UI(작업 2).
   - `events` 수집: `NfcClaimEvent.Claimed` → **토스트 + 발급 결과 다이얼로그**(금액/유효기한/메시지) 표시 →
     확인 시 **내 기프티콘 목록(`Routes.GIFTICON`)으로 이동**(현재 화면 pop). 이동으로 목록이 재구독→재조회되어
     발급된 쿠폰이 보인다(= 새로고침). GiftClaim 의 토스트→onClaimed 패턴을 따른다.
   - 카피 한국어, 디자인 토큰만(hex 금지), anti-slop 준수.

4. **네비/매니페스트/진입점**:
   - `Routes` 에 `NFC_CLAIM = "nfc_claim"` 추가, `AppNavHost` 에 `composable(Routes.NFC_CLAIM) { NfcClaimRoute(...) }`
     등록(뒤로가기·발급 성공 시 `Routes.GIFTICON` 이동 콜백 배선).
   - `AndroidManifest.xml`:
     - `<uses-permission android:name="android.permission.NFC" />`
     - `<uses-feature android:name="android.hardware.nfc" android:required="false" />`(미지원 기기 설치 허용)
   - **진입점**: 내 기프티콘 목록 화면(`GifticonScreen`)에 "NFC 쿠폰 받기" 진입(상단 액션/빈 상태 버튼 등 자연스러운
     한 곳)을 추가해 `Routes.NFC_CLAIM` 으로 이동.

### 핵심 규칙 (반드시 준수)

- reader mode 는 화면이 보일 때만 활성, 이탈 시 반드시 해제(`onDispose`). 자원 누수·백그라운드 스캔 금지.
- NFC 미지원/비활성 기기에서 **크래시 없이** graceful 처리(안내 + 설정 유도 또는 진입 비활성). 권한은 NFC 최소.
- 금전성 발급 — 낙관적 UI 금지: 성공 다이얼로그/이동은 ViewModel 성공 이벤트 이후에만.
- `tagCode`/raw/토큰을 로깅하지 마라. 화면은 ViewModel 만 사용(레이어 분리).
- **TDD 가드**: 신규 `src/main` `.kt` 마다 대응 `...Test.kt` 를 먼저 만든다. Compose 화면은 `src/androidTest`
  의 `NfcClaimScreenTest`(`createComposeRule`: 미지원/비활성/진행중/에러/안내 상태 렌더), 파서/리더 글루는
  단위 또는 androidTest 로 커버.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**:
   - `NfcClaimScreenTest`(androidTest, `createComposeRule`): 안내/진행중/에러/NFC미지원·비활성 상태가 올바르게
     렌더되고, 에러 메시지·다이얼로그가 노출되는지.
   - NDEF 파서/리더 글루의 가능한 부분 단위테스트(URI/Text 추출, 어댑터 null/disabled 분기 헬퍼).
2. AC 통과·무회귀(기존 네비/기프티콘/스케줄링 테스트 포함). 실제 NFC 태깅은 기기 필요 — 빌드/테스트는
   하드웨어 없이 통과해야 한다.
3. 체크리스트: reader mode 활성/해제, 미지원·비활성 graceful, 성공→다이얼로그→기프티콘 이동(새로고침),
   딥링크/입력 검증(`NfcTagCode`), 미로깅, 디자인 토큰·한국어, 따닥 가드.
4. `phases/29-nfc-coupon-claim/index.json` step 3 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "NfcAdapter reader mode로 NDEF(URI/Text) 읽어 NfcTagCode 추출→NfcClaimViewModel.onTagRead 발급. NfcClaimScreen/Route+AppNavHost+NFC권한/feature(required=false). NFC미지원·비활성 graceful(설정유도), 성공시 토스트+발급결과 다이얼로그→내 기프티콘 이동(목록 재조회=새로고침), 따닥 가드·미로깅. 진입점=기프티콘 화면."`
   - 3회 실패 → `"status": "error"` + `"error_message"` / 사용자 개입 필요 → `"status": "blocked"` + `"blocked_reason"`

## 금지사항

- 백그라운드/상시 NFC 스캔, reader mode 미해제(누수) 금지. 불필요한 권한 추가 금지(NFC 만).
- step1~2 의 계약(파서/API/리포지토리/ViewModel)을 재설계하지 마라. 이미 만든 것을 사용하라.
- hex 색 리터럴·보라/네온 anti-slop 금지(토큰만). `tagCode`/토큰 로깅 금지. 기존 테스트를 깨뜨리지 마라.
