# Step 0 — 음성 AI 주문 화면 재설계 (VOICE, TDD)

> 첨부된 `주문 - 05 (음성 AI 주문).png` + `docs/SCREENS.md`(VOICE)를 그대로 따른다.
> **배경**: 기존 `ui/feature/voice/VoiceScreen.kt`(phase 8)를 디자인에 맞게 재작업한다. 규칙기반 파서·온디바이스 STT·RECORD_AUDIO 권한 도메인 로직은 재사용.

## 바꿀 것 — `ui/feature/voice/`
`docs/SCREENS.md` VOICE 레이아웃으로 재작업:
- **전체 다크 배경**(`surface-dark`). 상단바: 좌 `‹`, 중앙 "음성으로 주문" `on-dark`, 우 `✕`(닫기).
- 큰 헤드라인 "원하시는 메뉴를 / 말씀해주세요" `display`/`on-dark`.
- 중앙 **코랄 펄스 원**(`primary`→`accent-soft`, 듣는 중 절제된 펄스). 아래 "● 듣는 중" `on-dark`/`muted`.
- **인식된 음성 카드**(어두운 카드): "인식된 음성" `muted` + transcript(확정 `on-dark`, interim `muted`).
- **AI 인식 결과 카드**: "AI 인식 결과" + "신뢰도 N%" 태그(`accent-soft`/`primary`). 파싱 항목 행(메뉴·옵션·수량) + "예상 금액" 강조.
- 하단 2버튼: **"다시 말하기"**(secondary/dark outline) · **"이대로 주문"**(`primary`) → 장바구니 반영 후 `CART`.
- RECORD_AUDIO 권한 없으면 권한 요청/대체 경로. 시각 transcript 항상 병행(접근성).

## 데이터 (기존 재사용)
- 기존 음성 ViewModel/UiState/규칙기반 파서/SpeechRecognizer 추상화 재사용. transcript→파싱→예상 금액/항목 매핑 유지. 신뢰도 표시가 없으면 파서 결과에서 매핑.
- 온디바이스/Mock(키 불필요) 유지 — 클라우드 STT/LLM 미연동.

## ⚠ TDD — 기존 음성 테스트 유지
- transcript→파싱 항목·예상 금액, "이대로 주문"→장바구니 반영, 권한 분기(Turbine). 기존 voice 테스트가 구조 변경으로 깨지면 새 구조로 갱신.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 마이크 권한 rationale 유지, 최소 권한. 네온 글로우 금지(절제된 펄스).

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 음성 화면이 `주문 - 05`.png 구조(다크·펄스 원·인식된 음성·AI 결과·이대로 주문)와 일치한다.
- 통과하면 `phases/15-redesign-voice-states/index.json`의 step 0 status를 `completed` + `summary` 기록.
