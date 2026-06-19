# Step 1 — 쿠폰 / 선물하기 재설계 (COUPON / GIFT, TDD)

> 첨부된 `MY - 02 (쿠폰).png`(COUPON) + `MY - 03 (선물하기).png`(GIFT) + `docs/SCREENS.md`(COUPON, GIFT)를 그대로 따른다.
> **배경**: 기존 스탬프/기프티콘 화면(phase 6)을 디자인의 COUPON·GIFT로 재작업한다. 보호 화면·민감값(바코드/QR/수신자/토큰) 미로깅 규칙 유지.

## 바꿀 것
1. **COUPON** — `ui/feature/coupon/`(기존 stamp 위치 재사용 가능): `CafeTopBar`(‹ 쿠폰).
   **스탬프 다크 카드**(`surface-dark`): "{매장} 스탬프" + "{N} / 10" `display`/`on-dark`, 안내 "스탬프 {남은}개만 더 모으면 무료 음료 쿠폰!" `primary`.
   **스탬프 그리드**(10칸/2행): 채움=`primary` 원+체크, 빈칸=어두운 원+숫자 `muted`.
   "보유 쿠폰 (N)" + 쿠폰 카드(`surface-card`): 코랄 아이콘, 제목 `h3`, "유효기간 …" `caption`/`muted`(곧 만료 `warning`), 우 `›`. 사용/만료는 딤.
2. **GIFT** — `ui/feature/gift/`: `CafeTopBar`(‹ 선물하기).
   기프티콘 미리보기 카드(`primary` 배경): "✱ CAFEMINSO" + 금액 `display`/`on-primary` + "금액형 기프티콘".
   "금액 선택" 4분할 [5,000][10,000(선택=`surface-dark`)][20,000][직접입력]. "받는 방식" 2카드 [카카오톡][문자(SMS)].
   "받는 사람" `CafeTextField` + "선물 메시지(선택)" 멀티라인. 하단 **"구매하고 선물 보내기 · {금액}원"** `CafeButton`(primary).

## 데이터 (기존 재사용 + 계약)
- 스탬프: `RewardRepository`(`StampCard`). 쿠폰: 기존 소스/`CouponRepository`(있으면). 선물 전송: `GiftRepository.sendGift`(`DATA_MODEL.md`) — 실Kakao or **Mock 폴백**(키 부재 시).
- 민감값(바코드/QR/수신자 연락처/토큰)은 화면/로그 노출·복사 최소화(`SECURITY.md §4`). 금전 전송은 확정 후 반영.

## ⚠ TDD — ViewModel 테스트 먼저
- COUPON: 스탬프 N/10·남은 개수·그리드 채움 매핑, 쿠폰 목록/만료 상태(Turbine). GIFT: 금액·받는 방식 선택 상태, 전송 성공/실패, 수신자 미로깅.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만. hex/새 토큰 금지. 카피 한국어. 민감값 로깅 금지. 카카오 키 하드코딩 금지(폴백).
- 주문내역 재작업은 step 2.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 성공. 직접 실행해 확인하라.
- 쿠폰이 `MY - 02`, 선물하기가 `MY - 03` 구조와 일치하고, 스탬프 그리드·금액 선택·선물 전송(폴백)이 동작한다.
- 통과하면 `phases/14-redesign-my/index.json`의 step 1 status를 `completed` + `summary` 기록.
