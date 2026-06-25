# Step 2: home-coupon-count (홈 보유 쿠폰 수 일치)

홈 화면은 "사용 가능 쿠폰 N장" 을 **기프티콘 수**(`HomeViewModel`, `GifticonStatus.Available` → 0장)로 세고,
쿠폰 탭(`CouponScreen`)은 **쿠폰 수**(`couponRepository.observeCoupons()` → 3장)로 센다. 서로 다른 도메인
소스라 홈 0장 vs 쿠폰탭 3장으로 어긋난다. 홈을 **쿠폰 탭과 동일한 소스**(쿠폰)로 맞춘다.

## 읽어야 할 파일

- `app/src/main/java/com/cafeminsu/ui/feature/home/HomeViewModel.kt` (`availableCouponCount = gifticons.count {...}`)
- `app/src/main/java/com/cafeminsu/ui/feature/home/HomeUiState.kt`
- `app/src/main/java/com/cafeminsu/ui/feature/home/HomeScreen.kt` (`CouponSummaryCard`, "사용 가능 쿠폰 N장")
- `app/src/main/java/com/cafeminsu/ui/feature/coupon/CouponScreen.kt` (`보유 쿠폰 (${coupons.size})`)
- `app/src/main/java/com/cafeminsu/ui/feature/coupon/CouponViewModel.kt` (`couponRepository.observeCoupons()`)
- `app/src/main/java/com/cafeminsu/domain/repository/CouponRepository.kt`
- `app/src/main/java/com/cafeminsu/domain/model/Reward.kt` (`Coupon`, `CouponStatus`)
- `app/src/test/java/com/cafeminsu/ui/feature/home/HomeViewModelTest.kt`

## 작업

1. **HomeViewModel**:
   - `CouponRepository` 주입. 홈 상태 combine 에 `couponRepository.observeCoupons()` 추가.
   - "사용 가능 쿠폰 N장" 카운트를 **`CouponStatus.Available` 인 쿠폰 수**로 변경(쿠폰 탭과 동일 소스/필터).
   - 쿠폰 흐름 실패 시 결제처럼 비치명 처리(0장 폴백, 홈 전체는 계속 렌더). 기존 기프티콘 로직이 다른 용도로 쓰이면 그대로 두고 **쿠폰 카운트만** 교체.
2. `HomeUiState`/`HomeScreen` 의 라벨("사용 가능 쿠폰 N장")·디자인은 유지, 값만 쿠폰 기준.
3. `CouponScreen`/`CouponViewModel` 은 변경하지 않는다(이미 올바른 소스).

### 핵심 규칙 (반드시 준수)

- 홈 카운트 == 쿠폰 탭 카운트(동일 `CouponRepository`·동일 `Available` 필터)여야 한다.
- AppResult/Flow 실패 비전파(`catch`), 홈은 부분 실패에도 렌더.
- 토큰만, 한국어 카피 유지.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## 검증 절차

1. **테스트 우선(TDD)**: `HomeViewModelTest` 에 "쿠폰 3장(Available)일 때 홈 availableCouponCount == 3" 검증 추가(쿠폰 탭과 동일 Mock 데이터). 먼저 실패시킨 뒤 구현.
2. 위 AC 통과. 기존 `HomeViewModelTest` **무회귀**.
3. 결과에 따라 `phases/28-owner-pay-fixes/index.json` 의 step 2 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "홈 사용가능 쿠폰 수를 기프티콘 대신 CouponRepository(Available) 기준으로 변경해 쿠폰 탭과 일치"`
   - 3회 실패 → `"status": "error"`, `"error_message": "<에러>"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "<사유>"` 후 중단

## 금지사항

- `CouponScreen`/`CouponViewModel` 을 바꾸지 마라(이미 정답 소스).
- 라벨/디자인 토큰을 임의 변경하지 마라. 기존 테스트를 깨뜨리지 마라.
