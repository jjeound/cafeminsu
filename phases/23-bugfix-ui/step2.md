# Step 2 — 선물하기 "받는 방식" 카드 텍스트 잘림 수정

## 배경
선물하기 `ui/feature/gift/GiftScreen.kt` 의 "받는 방식" 카드(`GiftChannelCard`)에서 텍스트가 잘린다.
디자인 단일 진실은 첨부 PNG `docs/screens/MY - 03 (선물하기).png` (Read 도구로 직접 열어 기준으로 삼을 것).

`GiftChannelCard` 는 `modifier.height(spacing.space14 + spacing.space2)` = **64dp 고정 높이**인데, 내부
`Column.padding(spacing.space4)`(상·하 합 32dp) + 2줄 텍스트(제목 `bodyL` "카카오톡"/"문자 (SMS)" + 부제 `caption`
"친구 선택"/"연락처 입력")가 가용 높이를 초과해 **텍스트가 클립된다.** 디자인 PNG 처럼 제목+부제가 온전히 보여야 한다.

## 작업 범위 (이 step에서만)
1. `GiftChannelCard` 가 2줄 텍스트를 자르지 않게 한다: 고정 `height(...)` 를 `heightIn(min = ...)` 으로 바꾸거나
   높이를 충분히 키우고/내부 패딩을 조정한다. Row 안 두 카드의 높이는 서로 일관되게(가장 큰 콘텐츠 기준) 보이도록 한다.
   디자인 PNG 의 카드 비율·여백 느낌을 따른다.
2. 테스트: `GiftScreenTest`(androidTest) 의 받는 방식 카드 텍스트("카카오톡"/"문자 (SMS)" 및 각 부제 "친구 선택"/"연락처 입력")
   표시 검증을 추가/유지하고 컴파일을 유지한다. 기존 테스트를 깨뜨리지 않는다.

## 금지 / 불변
- **이번 step 은 "받는 방식" 카드 클립만 고친다.** 통화 단위('원'/'₩') 관련 표시는 **건드리지 않는다**(금액 칩·미리보기·
  버튼·커스텀 금액 입력 등 금액 표시 로직 변경 금지).
- 선물 도메인/전송(`GiftViewModel`·`GiftRepository`) 로직을 변경하지 않는다(레이아웃 수정만).
- 디자인 토큰·hex 가드레일·한국어 카피·`GiftUiState` 공개 형태를 유지한다.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 2 를 `completed` + `summary` 로 갱신·커밋.
