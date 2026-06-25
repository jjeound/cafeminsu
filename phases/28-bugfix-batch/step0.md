# Step 0 — 쿠폰(기프티콘) 할인 금액 버그 수정

## 배경 / 증상
결제 화면에서 **2,000원 쿠폰을 선택하면 4,500원이 할인된다**(가장 비싼 한 잔 가격만큼 깎임). 금액권은
잔액(예: 2,000원)만큼만 할인돼야 한다.

원인: 결제 쿠폰은 기프티콘에서 가져오는데(`ui/feature/payment/PaymentViewModel.kt`의
`observeGifticons()` → `toPaymentCouponUiModel`), 도메인 모델 `domain/model/Reward.kt`의 `Gifticon` 에
**금액(잔액) 필드가 없어서** `toPaymentCouponUiModel` 이 `items.maxOfOrNull { it.unitPrice }`(가장 비싼 한 잔)
로 할인액을 계산한다. 실서버 기프티콘(`data/remote/GifticonApi.kt`의 `MyGifticonRes.balance`,
`GifticonDetailRes.balance/amount`)은 금액을 가지고 있고, `data/mapper/RewardMapper.kt`는 그 금액을
`title`("₩2,000") 문자열로만 옮기고 구조화된 금액은 버린다.

## 작업 범위 (이 step에서만)
1. **도메인 모델**: `domain/model/Reward.kt` 의 `Gifticon` 에 잔액 필드를 추가한다 —
   `val amount: Int = 0`(기본값 0 = 금액 정보 없는 교환권). 기본값을 둬 기존 생성 지점은 그대로 컴파일된다.
2. **매퍼**: `data/mapper/RewardMapper.kt` 의 `MyGifticonRes.toGifticon()`, `GifticonDetailRes.toGifticon()`,
   `GifticonUseRes.toGifticon()` 가 `Gifticon.amount = balance`(잔액)로 채우도록 한다.
3. **결제 할인 계산**: `PaymentViewModel.toPaymentCouponUiModel` 을 고친다 —
   `discountAmount = if (amount > 0) amount else items.maxOfOrNull { it.unitPrice } ?: 0`.
   즉 **금액권(amount>0)은 잔액만큼만**, 금액 정보가 없는 교환권(amount==0)만 종전처럼 가장 비싼 한 잔으로 폴백.
   (할인이 결제 금액을 넘지 않게 `coerceAtMost(totalAmount)` 하는 기존 `PaymentUiState` 로직은 그대로 둔다.)
4. **목 데이터 정합**: `data/mock/MockData.kt` 등 금액형 기프티콘(예: "디저트 2천원 할인권")에는 알맞은 `amount`
   를 설정하고, 무료 음료/스탬프 교환권은 `amount = 0`(폴백)으로 둔다.

## 테스트 (먼저 작성)
- `app/src/test/.../payment/PaymentViewModelTest.kt` 에 케이스 추가:
  **amount=2,000 기프티콘 선택 → discountAmount == 2,000, payableAmount == total-2,000** (가장 비싼 한 잔이 아님).
- 기존 무료음료(amount 없음) 테스트(`applyingGifticonReducesPaidAmountAndMarksUsed`,
  가장 비싼 한 잔만큼 할인)는 **그대로 통과**해야 한다.

## 금지 / 불변
- 결제 플로우(prepare/authorize/verify, 멱등키, 낙관 금지)·`PaymentUiState` 형태·기프티콘 사용처리는 불변.
- 디자인 토큰/한국어 카피 가드레일 준수.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 0 을 `completed` + `summary` 로 갱신·커밋.
