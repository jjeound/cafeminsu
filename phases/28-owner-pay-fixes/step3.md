# Step 3: gift-share-intent (선물 = 구매 후 카톡 공유시트)

선물하기 흐름을 단순화한다. 현재(phase 26)는 카카오 친구를 골라 **직접 메시지**를 보낸다. 변경 후:
**구매하기 → 서버가 준 `shareLink` 를 카카오톡 공유시트(인텐트)로 공유**. 친구 선택 UI 와 SMS 채널을 제거한다.
공유 인프라는 phase 24(`GiftEvent.LaunchKakaoShare`, v2-share 의존, `GifticonShareRes.shareLink/deepLink`)가 이미 존재 — **재사용**한다.

## 읽어야 할 파일

- `app/src/main/java/com/cafeminsu/ui/feature/gift/GiftScreen.kt` (친구 선택·채널 선택·`sendKakaoMessage`·`launchKakaoShare`/`openWebShare`)
- `app/src/main/java/com/cafeminsu/ui/feature/gift/GiftViewModel.kt` (`sendGift()`, `GiftEvent.SendKakaoMessage` vs `LaunchKakaoShare`)
- `app/src/main/java/com/cafeminsu/ui/feature/gift/GiftUiState.kt` (채널/수신자 상태, 버튼 라벨)
- `app/src/main/java/com/cafeminsu/data/repository/RealGiftRepository.kt` (`sendGift`: purchase→share, 수신자 옵셔널)
- `app/src/main/java/com/cafeminsu/data/mapper/GiftMapper.kt` (`toGiftSendResult` → shareLink/deepLink)
- `app/src/main/java/com/cafeminsu/domain/model/Reward.kt` (`GiftSendRequest`, `GiftChannel`, `GiftSendResult`)
- `app/src/main/java/com/cafeminsu/domain/repository/GiftRepository.kt`
- `app/src/test/java/com/cafeminsu/ui/feature/gift/` (GiftViewModel 테스트)
- `phases/24-kakao-gift-share/step1.md` (LaunchKakaoShare 런처·웹 폴백 패턴)
- `/docs/UI_GUIDE.md` · `/docs/SECURITY.md`(선물 링크/딥링크만, 민감값 미로깅)

## 작업

1. **GiftScreen / GiftUiState**: 카카오 친구 선택 UI + 채널(KakaoTalk/SMS) 선택 + 수신자 입력 + `GiftEvent.SendKakaoMessage`(phase 26) 직접 메시지 경로를 **제거**. 화면은 금액 선택 + (선택) 메시지 + **구매하기** 버튼 중심으로 단순화. 수신자 없이 구매 가능.
2. **GiftViewModel.sendGift()**: 수신자 미선택으로 `giftRepository.sendGift`(KakaoTalk 채널, 수신자 미전송) 호출 → 성공 시 **`GiftEvent.LaunchKakaoShare(shareLink/deepLink)`** 방출. 친구 uuid/직접 메시지 경로 삭제.
3. **공유 런처(GiftScreen)**: phase 24 의 카카오톡 공유시트 런처 재사용 — 카톡 설치 시 카카오 공유, 미설치 시 웹/`Intent.ACTION_SEND` 폴백. **카카오 공유 실패는 선물 실패가 아니다**(구매는 이미 성공).
4. `RealGiftRepository`(이미 수신자 옵셔널)·`GifticonApi`·도메인 모델은 **최소 변경**. `GiftChannel.Sms` 가 다른 곳에서 안 쓰이면 선물 UI 에서만 제거(모델 enum 은 보존 가능).

### 핵심 규칙 (반드시 준수)

- 구매(`sendGift`) 성공 후에만 공유시트 노출(낙관 금지). 공유 실패는 토스트/무시 — 구매 결과를 뒤집지 마라.
- 민감값(claimCode/딥링크) 과다 로깅 금지(`SECURITY §3`).
- 토큰만, 한국어, 안티-AI슬롭.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## 검증 절차

1. **테스트 우선(TDD)**: `GiftViewModelTest` 갱신 — "구매하기 성공 → `LaunchKakaoShare(shareLink)` 이벤트" 검증, 기존 `SendKakaoMessage`(친구 메시지) 테스트는 제거/대체. 수신자 없이도 sendGift 호출되는지 확인.
2. 위 AC 통과. 무회귀(claim/register 등 다른 선물 테스트 보존).
3. 결과에 따라 `phases/28-owner-pay-fixes/index.json` 의 step 3 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "선물하기 친구선택·SMS 채널·직접메시지 제거 → 구매하기 성공 시 shareLink 를 카카오톡 공유시트(LaunchKakaoShare, 미설치 웹/ACTION_SEND 폴백)로 공유"`
   - 3회 실패 → `"status": "error"`, `"error_message": "<에러>"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "<사유>"` 후 중단

## 금지사항

- 구매 전에 공유시트를 띄우지 마라(낙관 금지). 공유 실패로 구매 성공을 실패 처리하지 마라.
- claimCode/딥링크를 로깅하지 마라. 기존 선물 클레임/등록 흐름·테스트를 깨뜨리지 마라.
