# Step 0 — 음성 주문 파싱 (도메인 UseCase, 규칙기반, TDD)

음성 STT 결과 텍스트(transcript)를 구조화된 주문 의도로 바꾸는 **순수 Kotlin 도메인 로직**을 만든다.
`ARCHITECTURE.md §음성 AI 주문 파이프라인`의 "주문 파싱(발화 → {메뉴, 옵션, 수량})" 단계다. ADR-004의
**규칙기반(rule-based) 경로**를 택한다(LLM/클라우드 키 불필요 → 이 step은 blocked 아님).

## 만들 것 — `app/src/main/java/com/cafeminsu/domain/voice/` (또는 domain/usecase)
- `ParsedOrder` / `ParsedOrderItem` 도메인 모델:
  - `ParsedOrderItem(menuItemId: String, name: String, quantity: Int, selectedOptions: List<SelectedOption>)`
  - `ParsedOrder(items: List<ParsedOrderItem>, unmatched: List<String>)` — 못 알아들은 토막은 `unmatched`로.
- `ParseVoiceOrderUseCase`(또는 `VoiceOrderParser`): `operator fun invoke(transcript: String, menu: List<MenuItem>): ParsedOrder`.
  순수 함수. Android/`androidx` import 금지.
  - **메뉴 매칭**: 정규화(공백/조사 제거, 소문자) 후 `MenuItem.name` 부분일치로 매칭. 품절(`isSoldOut`)은 매칭하되 표시용 플래그 유지(담기 가부는 상위가 판단).
  - **수량**: 한국어 수사(한/하나=1, 두/둘=2, 세/셋=3, 네/넷=4, 다섯…)와 아라비아 숫자 + 단위(잔/개) 인식. 미지정은 1.
  - **옵션(있으면)**: "따뜻/뜨겁/핫"→온도 Hot, "아이스/차갑/시원"→Iced, "샷 추가/연하게" 등 키워드를 해당 메뉴의
    `MenuOptionGroup`/`MenuOption`과 매칭(없으면 무시). 메뉴에 없는 옵션은 적용하지 않는다.
  - 여러 항목(쉼표/"그리고"/"랑"으로 구분) 처리.

## ⚠ TDD — 테스트 먼저
`ParseVoiceOrderUseCaseTest.kt`(실패 먼저 → 구현). 시드 메뉴(테스트 픽스처)로:
- "아메리카노 두 잔" → 아메리카노 qty=2.
- "라떼 한 잔 따뜻하게" → 라떼 qty=1 + 온도 Hot 옵션(메뉴에 해당 옵션 있을 때).
- "아메리카노랑 라떼" → 두 항목 각 qty=1.
- 메뉴에 없는 발화("피자 하나") → `unmatched`에 포함, items 비거나 매칭분만.
- 수량 미지정 → 1.
- 빈/공백 transcript → 빈 `ParsedOrder`.

## 하지 말 것
- STT/마이크/권한/화면 코드 금지(다음 step). LLM·네트워크 호출 금지(규칙기반). hex/Android import 금지.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest` 성공(파서 테스트 포함). `./gradlew :app:compileDebugKotlin` 성공. 직접 실행해 확인하라.
- `domain/voice`에 android/androidx import 없음.
- 통과하면 `phases/8-voice/index.json`의 step 0 status를 `completed` + `summary` 기록.
