# Step 0 — 점주 모드 토대 + 점주 로그인 + 역할 분기 네비 셸 (TDD)

> 첨부된 `점주 - 00 (로그인).png` + `docs/SCREENS.md`(OWNER_LOGIN, 점주 화면 섹션)를 그대로 따른다.
> 점주 도메인 계약·모델·역할은 `docs/DATA_MODEL.md`(점주(Owner) 모델 — 역할·인증·운영) / `docs/ADR.md`(ADR-012) / `docs/PRD.md`(점주 화면 인벤토리)가 단일 진실.

고객(카카오)과 분리된 **아이디/비밀번호** 점주 로그인과, 역할(`CUSTOMER`/`OWNER`) 기반 네비게이션 분기 셸을 구축한다.
점주 운영 화면(대시보드/주문/메뉴/매출) 본구현은 다음 step들 — 여기서는 **로그인 + 4탭 셸 + 내부 화면 플레이스홀더**까지.

## 만들 것
1. **역할·모델**(`domain`, `DATA_MODEL.md`): `enum UserRole { Customer, Owner }`, `data class OwnerProfile(id, storeId, storeName, loginId, isStoreOpen)`.
   `AuthState.Authenticated`가 역할을 보유하도록 확장(기본 `Customer`) — **기존 사용처/테스트가 깨지지 않게** 기본값/오버로드로 안전하게 추가.
   비밀번호는 모델/로그/디스크에 두지 않는다(메모리 전용·전송 후 폐기).
2. **`OwnerAuthProvider` 추상화**(`DATA_MODEL.md`): `login(loginId, password): AppResult<OwnerProfile>`, `logout(): AppResult<Unit>`,
   `setStoreOpen(open): AppResult<OwnerProfile>`. `MockOwnerAuthProvider`(data): 아무 입력이나 성공 → 데모 `OwnerProfile("강남점")`.
   실연동(점주 인증 API)은 키 게이트 + Mock 폴백 — 지금은 Mock `@Binds` DI. 비밀번호 로깅·저장 금지(`SECURITY.md`).
3. **라우트**(`ui/navigation/Routes.kt`): `OWNER_LOGIN, OWNER_HOME, OWNER_ORDERS, OWNER_MENU, OWNER_SALES` 추가.
4. **네비 분기**(`ui/navigation/AppNavHost.kt`):
   - 고객 `LOGIN` 화면 하단에 **"점주 로그인 →"** 텍스트버튼 추가 → `OWNER_LOGIN`.
   - `OWNER_LOGIN` 인증 성공 → **점주 메인 Scaffold + 하단 탭 4개**(`docs/SCREENS.md` 점주 탭바): **대시보드(OWNER_HOME) · 주문(OWNER_ORDERS) · 메뉴(OWNER_MENU) · 매출(OWNER_SALES)**.
     활성 `primary`/비활성 `muted`, 상단 1px `hairline`. **OWNER_HOME/ORDERS/MENU/SALES 내부는 제목만 있는 플레이스홀더 화면으로 등록**(본구현 다음 step).
   - 고객 셸(홈/주문/MY 3탭)과 점주 셸(4탭)은 분리. 고객 플로우/3탭은 변경 금지.
5. **OWNER_LOGIN 화면** — `ui/feature/owner/login/`: `OwnerLoginViewModel`(StateFlow<OwnerLoginUiState>) + `OwnerLoginScreen`
   (`docs/SCREENS.md` OWNER_LOGIN): `CafeTopBar`(← 점주 로그인), 코랄 `*`, "매장 관리자 로그인" + 안내문, 아이디/비밀번호 `CafeTextField`(비밀번호 마스킹),
   "로그인" `CafeButton`(primary) → `login()` → 성공 시 `OWNER_HOME`, 실패 스낵바.

## ⚠ TDD — 먼저 작성
- `MockOwnerAuthProvider`: `login()`→`OwnerProfile`, `logout()`→성공, `setStoreOpen(true/false)`→`isStoreOpen` 반영. (MockK/Turbine)
- `OwnerLoginViewModel`: 로그인 성공 시 성공 이벤트/상태, 실패 시 에러 상태. 비밀번호가 상태/로그에 노출되지 않음.

## 규칙 / 하지 말 것
- 토큰/`ui/components`만 사용. hex/새 토큰 금지. 카피 한국어. 점주 비밀번호 로그/화면/디스크 노출 금지.
- 기존 고객 ViewModel/화면/3탭 네비를 깨지 마라(점주 셸은 추가). 대시보드/주문/메뉴/매출 본구현은 다음 step.

## Acceptance Criteria
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` 모두 성공. 직접 실행해 확인하라.
- 고객 LOGIN → "점주 로그인 →" → OWNER_LOGIN → (Mock) 로그인 → 점주 4탭 셸(대시보드/주문/메뉴/매출) 진입.
- 통과하면 `phases/10-owner/index.json`의 step 0 status를 `completed` + `summary` 기록.
