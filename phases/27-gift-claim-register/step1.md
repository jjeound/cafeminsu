# Step 1: claim-screen-and-deeplink (기프티콘 등록 화면 + 딥링크)

받는 사람 수동 등록의 2단계. step0 의 `GiftRepository.claimGift` 를 사용하는 **등록 화면**을 만들고,
`cafeminsu://gift?code=...` **딥링크**로 코드를 받아 자동 진입하게 한다. 수동 코드 입력도 지원.

## 읽어야 할 파일

- `/docs/ARCHITECTURE.md`(MVVM/UDF, `UiState`/`Error`), `/docs/UI_GUIDE.md`, `/docs/DESIGN_SYSTEM.md`(토큰만, 한국어),
  `/docs/SECURITY.md`(§6 딥링크 입력 검증·화이트리스트, 민감값 미로깅), `/docs/KAKAO_GIFT_BACKEND.md`(§4 딥링크)
- `phases/27-gift-claim-register/step0.md` 및 산출물(`GiftRepository.claimGift`, `Gifticon`)
- `app/src/main/java/com/cafeminsu/ui/navigation/Routes.kt`, `ui/navigation/AppNavHost.kt`(라우트/네비 패턴)
- `app/src/main/java/com/cafeminsu/ui/feature/gift/GiftScreen.kt`(기존 선물 화면/컴포넌트 스타일 참고)
- `app/src/main/java/com/cafeminsu/ui/feature/gifticon/`(내 기프티콘 화면 — 등록 후 이동 대상)
- `app/src/main/AndroidManifest.xml`(딥링크 intent-filter 패턴: 카카오 로그인/카카오페이 리다이렉트 참고)
- `app/src/main/java/com/cafeminsu/ui/components/`(CafeButton/CafeTextField/CafeTopBar 등 재사용 컴포넌트)

## 작업

1. **등록 화면 (ui)** — `ui/feature/gift/claim/` (또는 인접):
   - `GiftClaimRoute`/`GiftClaimScreen` + `GiftClaimViewModel`(Hilt) + `GiftClaimUiState`(Loading/Content/Error 등).
   - 코드 입력(`CafeTextField`) + "등록하기"(`CafeButton` primary). 딥링크로 받은 코드는 자동 채움.
   - `claimGift(code)` 호출 → 성공: 안내 + **내 기프티콘(`Routes.GIFTICON`)으로 이동**. 실패: `UiState.Error`/토스트.
   - 처리 중 버튼 비활성/중복 제출 가드. 카피 한국어, 디자인 토큰만(hex 금지).

2. **네비/딥링크**:
   - `Routes` 에 등록 라우트 추가(예: `GIFT_CLAIM = "gift_claim?code={code}"`), `AppNavHost` 에 composable 등록.
   - `AndroidManifest.xml` 에 `cafeminsu://gift` 딥링크 intent-filter 추가(scheme=`cafeminsu`, host=`gift`),
     `code` 쿼리 추출 → 등록 화면으로 라우팅. **스킴/호스트 화이트리스트만 수락**(SECURITY §6).
   - 진입점: 마이/기프티콘 화면 등에서 "선물 등록" 진입 링크(적절한 기존 화면 한 곳)도 추가.

### 핵심 규칙 (반드시 준수)

- 딥링크 입력 검증: 허용된 scheme/host 만, `code` 길이·형식 가드. 이유: 악성 딥링크 방지(SECURITY §6).
- `claimCode`/바코드/QR 미로깅. 결제/금전 아님이나 민감값 취급 동일.
- 레이어 분리: 화면은 ViewModel→`GiftRepository.claimGift` 만 사용. 도메인/데이터에 Context 비주입.
- 백엔드 claim 미구현 시: 화면/네비/딥링크는 동작하되 실 등록은 서버 준비 후(에러 핸들링으로 graceful).

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

## 검증 절차

1. **테스트 우선(TDD)**: `GiftClaimViewModelTest` — 코드 입력→등록 성공/실패 상태 전이(Turbine). 딥링크 파싱
   헬퍼가 있으면 그 단위 테스트(화이트리스트/코드 추출). 먼저 실패 테스트 작성 후 구현.
2. AC 통과 확인. 기존 테스트 무회귀(네비/선물 등).
3. 체크리스트: 딥링크 화이트리스트·입력검증, 미로깅, 레이어 분리, 디자인 토큰·한국어, 처리 중 가드.
4. `phases/27-gift-claim-register/index.json` step 1 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "기프티콘 등록 화면(GiftClaim*)+코드 입력/딥링크 cafeminsu://gift?code 수신→claimGift→내 기프티콘 이동. 딥링크 화이트리스트·입력검증"`
   - 3회 실패 → `"status": "error"` + `"error_message"` / 사용자 개입 필요 → `"status": "blocked"` + `"blocked_reason"`

## 금지사항

- 딥링크 scheme/host 화이트리스트 없이 임의 입력을 신뢰하지 마라. 이유: 보안.
- `claimCode`/민감값 로깅 금지. hex 색 리터럴 금지(토큰만). 기존 테스트를 깨뜨리지 마라.
- step0 의 data 계약(claim API/리포지토리)을 재설계하지 마라. 이미 만든 것을 사용하라.
