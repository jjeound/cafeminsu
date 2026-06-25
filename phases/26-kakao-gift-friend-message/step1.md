# Step 1: friend-pick-and-send (친구 피커 + 메시지 전송 + 공유 폴백 + UI)

카카오톡 친구 선물의 2단계. 카카오톡 채널에서 **친구 피커로 친구를 선택**하고, 구매(수신자 미지정) 후
**선택 친구에게 카카오톡 메시지로 기프티콘 링크를 전송**한다. 메시지 불가(앱 미가입 친구/실패) 시 기존
**공유(ShareClient)로 폴백**한다. Activity/SDK 의존은 UI·플랫폼 레이어에만 둔다.

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md`, `/docs/SECURITY.md`(§4 링크/친구식별자 미로깅), `/docs/UI_GUIDE.md`,
  `/docs/DESIGN_SYSTEM.md`(토큰만, 한국어), `/docs/KAKAO_GIFT_BACKEND.md`(구매 수신자 미지정 + claimCode/shareLink)
- `phases/26-kakao-gift-friend-message/step0.md` 및 step0 산출물(`KakaoTalkScopeProvider`, v2-friend/v2-talk)
- `app/src/main/java/com/cafeminsu/ui/feature/gift/GiftScreen.kt` (기존 `launchKakaoShare`/`openWebShare` 재사용)
- `app/src/main/java/com/cafeminsu/ui/feature/gift/GiftViewModel.kt`, `GiftUiState.kt`(`GiftEvent`, `KakaoShareTarget`)
- `app/src/main/java/com/cafeminsu/data/repository/RealGiftRepository.kt`(`toPurchaseReq`, `validate`)
- `app/src/main/java/com/cafeminsu/data/remote/GifticonApi.kt`(`GifticonPurchaseReq`, `GifticonShareRes`)
- `app/src/main/java/com/cafeminsu/domain/model/Reward.kt`(`GiftSendRequest`, `GiftChannel`, `GiftSendResult`)
- `app/src/main/java/com/cafeminsu/data/mapper/GiftMapper.kt`
- `app/src/test/java/com/cafeminsu/ui/feature/gift/GiftViewModelTest.kt`,
  `app/src/test/java/com/cafeminsu/data/repository/RealGiftRepositoryTest.kt`

step0 산출물과 기존 선물 코드를 읽고 의도를 이해한 뒤 작업하라.

## 작업

1. **구매 수신자 미지정 전환 (data)** — `RealGiftRepository.toPurchaseReq`/`validate`:
   - `GiftChannel.KakaoTalk` 일 때 `GifticonPurchaseReq(amount, message)` 만 전송(`receiverId`/`receiverPhone` 없음).
     백엔드가 토큰으로 발신자 식별 + 수신자 미지정 구매를 지원한다는 전제(`docs/KAKAO_GIFT_BACKEND.md`).
   - 검증: KakaoTalk 은 더 이상 숫자 receiverId 를 요구하지 않는다. **선택된 친구(uuid) 존재**를 전송 전제 조건으로
     본다(빈 선택 차단). SMS 채널은 기존(`receiverPhone`) 유지.
   - `GifticonPurchaseRes`/`GifticonShareRes` → `GiftSendResult` 매핑에 **claimCode**(있으면)를 포함하도록 확장
     (없으면 기존 `shareLink`/`deepLink` 사용). 친구 uuid 는 서버로 보내지 않는다.

2. **친구 피커 + 전송 (ui/플랫폼)**:
   - 카카오톡 채널 UI: "받는 사람" **텍스트 입력 제거** → **"카카오톡 친구 선택"** 버튼.
     `PickerApi.instance.selectFriend(...)`(단일 선택) → 선택 친구 **표시 이름** + uuid 보관(uuid 미로깅).
     선택 전에는 전송 비활성. 친구 선택 직전 step0 `KakaoTalkScopeProvider.ensureFriendMessageScopes` 호출.
   - 전송: 구매 성공 후 `TalkApiClient.instance.sendDefaultMessage(receiverUuids = listOf(uuid), templatable = 링크 템플릿)`.
     링크 = `GiftSendResult.shareLink`(claim 링크). 성공 시 "선물을 보냈어요".
   - **폴백**: 카카오톡 미설치/메시지 권한 없음/전송 실패/앱 미가입 친구면 기존 `launchKakaoShare`(ShareClient,
     미설치 시 웹 공유)로 폴백. 폴백도 불가하면 안내 토스트.
   - `GiftViewModel`/`GiftUiState`: 친구 선택 상태·이벤트(`GiftEvent`)에 친구 선택/메시지 전송/공유 폴백 분기 반영.
   - 카피 한국어, 디자인 토큰만(hex 금지).

### 핵심 규칙 (반드시 준수)

- **메시지 실패 ≠ 선물 실패**: 서버 구매가 성공했다면 카카오 메시지/공유 단계 실패는 선물을 실패로 만들지 않는다.
  메시지 불가 시 공유로 폴백한다. 이유: 이중 차감/혼란 방지, 카카오 메시지는 앱 가입 친구에게만 가능.
- **미로깅**: 친구 uuid/표시이름·shareLink/claimCode 를 로그에 남기지 마라(SECURITY §4).
- **레이어 분리**: `PickerApi`/`TalkApiClient`/Context 의존은 UI·플랫폼에만. `GiftViewModel`/도메인/데이터에 Context
  를 들이지 마라.
- 친구 uuid 를 **서버 구매 요청에 넣지 마라**(서버는 앱 유저 id/전화번호만 인식). uuid 는 메시지 전송 전용.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**: `GiftViewModelTest` 에 "카카오톡 채널 + 친구 선택됨 → 전송 시 메시지 전송 이벤트,
   실패 시 공유 폴백 이벤트" 분기를 Turbine 으로 먼저 작성한 뒤 구현. `RealGiftRepositoryTest` 에 KakaoTalk
   수신자 미지정 구매(req 에 receiver 없음) 케이스 추가. 피커/메시지 실호출은 Activity 의존이라 Fake/계약 위주.
2. AC 통과 확인. 기존 `GiftViewModelTest`/`RealGiftRepositoryTest`/`GiftMapperTest` 무회귀.
3. 체크리스트: 레이어 분리/미로깅/메시지-실패-비치명/디자인 토큰·한국어/uuid 서버 미전송.
4. `phases/26-kakao-gift-friend-message/index.json` step 1 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "카카오톡 채널 친구 피커(PickerApi)+메시지 전송(TalkApiClient), 미가입/실패 시 공유 폴백. 구매 수신자 미지정 전환(백엔드 게이트), uuid 서버 미전송"`
   - 3회 실패 → `"status": "error"` + `"error_message"`
   - 사용자 개입 필요 → `"status": "blocked"` + `"blocked_reason"` 후 중단

## 금지사항

- 친구 uuid 를 서버 구매 요청(`GifticonPurchaseReq`)에 넣지 마라. 이유: 서버 비인식 + 의미 오류.
- 카카오 메시지/공유 실패를 선물 전송 실패로 처리하지 마라.
- 친구 식별자/링크/claimCode 를 로깅하지 마라. hex 색 리터럴 금지(토큰만). 기존 테스트를 깨뜨리지 마라.
- 받는 사람 등록(claim) 화면/딥링크는 만들지 마라. 이유: phase 27 범위.
