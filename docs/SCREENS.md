# 화면 상세 스펙 (CafeMinsu)

> **이 문서 + `docs/screens/*.png` 가 화면 디자인의 단일 진실.** 빌드 step은 해당 PNG가 첨부되고 본 스펙이 주입된다.
> 색·치수·타이포는 **반드시 `DESIGN_SYSTEM.md` 토큰**으로만(hex 금지). 컴포넌트는 `ui/components/*` 재사용.
> 카피는 **한국어, 아래 문구 그대로**. 안티슬롭(`UI_GUIDE.md`) 금지. 상태 4종(+NeedsLogin) 유지.
> 토큰 약칭: 배경 `canvas`, 카드 `surface-card`, 다크카드 `surface-dark`, 강조/CTA `primary`, 본문 `body`,
> 헤드라인 `ink`, 보조 `muted`, 구분선 `hairline`, 성공 `success`, 경고 `warning`, 오류 `error`, 태그배경 `accent-soft`.

공통: 상단 상태바(시간/배터리) 영역 존중, 화면 사이드 패딩 `space-5`(20), 섹션 간격 `space-8`(32). 좌측 정렬 기본.

---

## SPLASH — `Splash.png`
- 전체 배경 **`primary`**(코랄). 중앙에 **"카페민수"** `display`/`on-primary`(흰색), 그 아래 "Warm cream coffee"
  작은 캡션 `on-primary` 70% 느낌(`muted` 대용은 on-primary 저강조). 좌상단 시간만.
- 동작: 앱 시작 시 노출 → AuthState 확인 → 인증 `HOME`, 미인증 `LOGIN`으로 전환(짧은 fade, 1.0~1.5s).

## LOGIN (회원가입) — `회원가입.png`
- 배경 `canvas`. 중앙: 코랄 **애스터리스크(✱) 로고**(`primary`) + **"카페민수"** `display`/`ink`.
- 하단: **카카오 로그인 버튼** — 카카오 옐로우(이 버튼은 브랜드 색 예외로 별도 상수 허용, 말풍선 아이콘 + "카카오 로그인" 검정 텍스트),
  height 52, `radius-lg`. 가로 패딩 `space-5`.
- 동작: 탭 → 카카오 로그인(실연동, 키 부재 시 Mock 인증) → 성공 시 **신규 유저면 `SIGNUP`**, 기존 유저면 `HOME`. 실패는 스낵바 안내.

## SIGNUP (회원가입 - 닉네임 설정) — `회원가입 - 닉네임 설정.png`, 에러: `회원가입 - 닉네임 설정 (에러).png`
- 신규 카카오 유저 최초 1회. 배경 `canvas`. 좌측 정렬, 사이드 패딩 `space-5`(20). 좌상단 `‹` 뒤로(로그인 복귀).
- 헤더: **"닉네임을 설정해주세요"** `h1`/`ink` + "카페민수에서 사용할 이름이에요" `body`/`muted`.
- 입력: 라벨 "닉네임"(`body` Medium/`ink`). `CafeTextField`(`surface-card` 배경, `radius-md`(12), height 52),
  placeholder "닉네임을 입력해주세요" `muted`, 입력 텍스트 `ink`, 우측 클리어(✕) `muted`. 포커스 1.5px `primary` 보더.
- 헬퍼 행: 좌측 규칙 "한글·영문·숫자 2~10자" `caption`/`muted`, 우측 글자수 "n/10" `caption`/`muted`.
- 하단 폭 꽉 찬 **"시작하기"** `CafeButton`(primary, `on-primary` 텍스트) → 닉네임 저장 후 `HOME`.
- 에러 상태: 입력 1.5px `error` 보더 + 헬퍼를 에러 메시지로 교체("이미 사용 중인 닉네임이에요" / 규칙 위반 시
  "한글·영문·숫자 2~10자로 입력해주세요") `error`. 닉네임 유효(2~10자)·미중복 전까지 진행 차단.

## HOME — `홈.png`
- 상단: **"안녕하세요, 민수님"** `h1`/`ink` + 아래 "오늘도 잘 부탁드려요" `body`/`muted`. 우상단 **알림 벨 아이콘**(빨간 점 배지) → `NOTI`.
- **오늘의 추천 메뉴 카드**(`CafeCard` product, `surface-dark`): 상단 라벨 "오늘의 추천 메뉴" `on-dark`,
  우측 "🔥 인기" 태그(`accent-soft` 배경/`primary` 텍스트, pill). 좌측 메뉴 썸네일(원형/라운드), 우측 텍스트:
  메뉴명 "민수 시그니처 라떼" `h3`/`on-dark`, 설명 "고소한 헤이즐넛 시럽 + 따뜻한 우유" `caption`,
  가격 **"5,500원"** `primary` + 취소선 "6,000원" `muted`. 하단 폭 꽉 찬 **"지금 주문하기 ›"** 버튼(`primary`).
- **사용 가능 쿠폰 카드**(`CafeCard` default): 좌측 쿠폰 아이콘(코랄 라운드), "사용 가능 쿠폰 3장" `bodyL`/`ink`,
  "1잔 무료 쿠폰 · 오늘 만료" `caption`/`muted`, 우측 `›`. → `COUPON`.
- **"다시 주문하기"** 섹션 헤더 `h2` + 우측 "전체보기 ›"(`primary`). 2열 카드(`surface-card`):
  각 카드 썸네일 + 메뉴명(아메리카노 ICE / 헤이즐넛 라떼) `h3`, 옵션요약 "샷 추가 · 톨" `caption`/`muted`,
  우상단 "어제"/"3일 전" `meta`, 하단 pill "4,500원 · 재주문"(`hairline` 보더). 탭 → 재주문(장바구니/상세).
- 하단 탭바: **홈 / 주문 / MY** (활성 `primary`, 비활성 `muted`, 상단 1px `hairline`).

## NOTI (알림) — `알림.png`
- `CafeTopBar`: 좌측 `‹` 뒤로, 중앙 "알림". 배경 `canvas`.
- 그룹 헤더 "오늘"/"어제" `caption`/`muted`. 알림 행: 좌측 원형 아이콘(타입별), 제목 `bodyL`/`ink`(예: "주문이 준비됐어요"),
  본문 `body`/`muted`(예: "주문번호 A-2419 — 픽업대에서 수령해주세요"), 우측 시간 `meta`(방금/5분 전/어제 19:42) +
  미읽음 코랄 점. 행 구분 여백. 빈 상태: EmptyView.
- 타입별 아이콘 색: 주문=코랄, 스탬프=`warning`/코랄, 기프티콘=민트(`success` 계열) — 토큰 사용.

## STORE (매장 선택) — `주문 - 01.png`
- 헤더 "매장 선택" `h1`/`ink` + "오늘 어디서 한 잔 하실까요?" `body`/`muted`. 우상단 검색(돋보기) 아이콘.
- **검색 입력**(`CafeTextField`): 좌측 위치핀, placeholder "현재 위치 또는 매장명 검색".
- **지도 영역**(`StoreMap`): 라운드 카드, 좌상단 "내 주변 지도" pill, 중앙 코랄 마커. (실연동 지도 or 플레이스홀더 그리드.)
- "가까운 매장" `h2` + 우측 "전체 보기"(`primary`). 매장 카드 목록: 첫 항목 **선택=다크 카드(`surface-dark`)**,
  나머지 `surface-card`. 각 카드: 좌측 썸네일, 매장명 "카페민수 강남점" `h3`, 거리 pill "120m",
  주소 `caption`/`muted`, 영업상태 점+텍스트("● 영업중" `success` / "● 20:00 마감" `warning`/`muted`). 탭 → `STORE_DETAIL`.
- 하단 탭바(홈/주문/MY, 주문 활성).

## STORE_DETAIL (매장 상세, 바텀시트) — `주문 - 02 (매장 선택).png`
- 뒤 화면 흐리게(스크림) + 하단 **바텀시트**(상단 핸들 바, `canvas`, 큰 라운드 상단).
- 매장명 "카페민수 강남점" `h2`/`ink`, 영업상태 "● 영업중 · 22:00 마감" `success`.
- 정보 행(라벨 `muted` / 값 `ink`): 주소·전화·거리·주차("건물 내 30분 무료"). 라벨 좌측 정렬, 값 우측 또는 들여쓰기.
- **편의시설 칩**(`CafeChip` 비선택 스타일): 콘센트 · Wi-Fi · 드라이브스루 · 테라스.
- 하단 폭 꽉 찬 **"이 매장에서 주문하기"** `CafeButton`(primary) → `MENU`(해당 매장 선택).

## MENU (메뉴 선택) — `주문 - 03 (메뉴 선택).png`
- 헤더: 매장명 "강남점" `h1`/`ink` + "오늘의 추천 메뉴" `caption`/`muted`. 우상단 검색 아이콘.
- **카테고리 칩 row**(가로 스크롤, `CafeChip`): 추천(선택) · 커피 · 논커피 · 디저트 · 티. 선택=`primary`.
- **메뉴 단일 컬럼 리스트**(2열 아님): 행마다 좌측 라운드 썸네일, 메뉴명 `h3`/`ink`(+품절 시 "품절" 태그 `accent-soft`),
  설명 `caption`/`muted`, 가격 `bodyL`/`ink`(또는 `primary`). 행 사이 `hairline` 구분. **품절 행은 비활성/딤 + 담기 불가.**
- **음성 주문 FAB**: 우하단 원형 `primary` + 마이크 아이콘 `on-primary` → `VOICE`.
- 탭 → `MENU_DETAIL`. 하단 탭바.

## MENU_DETAIL (메뉴 상세) — `주문 - 04 (메뉴 상세).png`
- `CafeTopBar`: 좌 `‹`, 중앙 "메뉴 상세", 우측 **찜 하트** 토글(`ink`/선택 `primary`).
- 상단 큰 이미지 영역(`surface-card` 배경 + 원형 메뉴 이미지).
- 메뉴명 "바닐라라떼" `h1`/`ink`, 설명 `body`/`muted`, 가격 "5,500원" `primary`(`h2`급). 아래 `hairline` 구분.
- **옵션 그룹**(라벨 `body` + "*필수" `caption`/`muted` 또는 `primary`):
  - 온도 *필수: [HOT] [ICE] — 2분할 토글, 선택=`surface-dark`/`on-dark`, 비선택=`surface-card`/`ink`.
  - 사이즈 *필수: [Regular] [Large (+500)].
  - 샷 추가: [없음] [+1샷 (+500)] [+2샷 (+1,000)] (3분할).
  - 선택 칩/세그먼트는 height ~52, `radius-md`~`lg`.
- **수량 스텝퍼**: "수량" 라벨 + [− 1 +] (우측 정렬, `surface-card`).
- 하단 고정 **"장바구니 담기 · {합계}원"** `CafeButton`(primary, 폭 꽉 참). 합계=base+옵션, 실시간 갱신. 품절 시 비활성.

## VOICE (음성 AI 주문) — `주문 - 05 (음성 AI 주문).png`
- **전체 다크 배경**(`surface-dark`). 상단바: 좌 `‹`, 중앙 "음성으로 주문" `on-dark`, 우 `✕`(닫기).
- 큰 헤드라인 "원하시는 메뉴를 / 말씀해주세요" `display`/`on-dark`.
- 중앙 **코랄 펄스 원**(`primary`→`accent-soft` 그라데이션, 듣는 중 절제된 펄스 애니메이션). 아래 "● 듣는 중" `on-dark`/`muted`.
- **인식된 음성 카드**(어두운 카드): 라벨 "인식된 음성" `muted`, 따옴표 transcript "아이스 바닐라라떼 한 잔이랑 / 따뜻한 아메리카노 한 잔 주세요" `on-dark`(확정), interim은 `muted`.
- **AI 인식 결과 카드**: 헤더 "AI 인식 결과" + 우측 "신뢰도 97%" 태그(`accent-soft`/`primary`). 항목 행:
  "바닐라라떼 · ICE · Regular ✕ 1", "아메리카노 · HOT · Regular ✕ 1" `on-dark`. 하단 "예상 금액" + **"10,000원"** `on-dark` 강조.
- 하단 버튼 2개: **"다시 말하기"**(secondary/dark outline) · **"이대로 주문"**(`primary`) → 장바구니 반영 후 `CART`.
- 권한(RECORD_AUDIO) 없으면 권한 요청/대체 경로(메뉴로). 시각 transcript 항상 병행(접근성).

## CART (장바구니) — `주문 - 06 (장바구니).png`
- `CafeTopBar`: 좌 `‹`, 중앙 "장바구니". 배경 `canvas`.
- **주문 방식 토글**: "주문 방식" 라벨 + 2분할 [매장에서 먹기] [포장 (픽업)] (선택=`surface-card` 흰톤 강조 within `surface-card` track).
- "주문 항목 (2)" 라벨. 항목 카드(`surface-card`): 좌 썸네일, 메뉴명 `h3` + 옵션 "ICE · Regular" `caption`/`muted`,
  우측 가격 "5,500원" `ink`, 그 아래 수량 스텝퍼 [− 1 +].
- **요청사항**: 라벨 + `CafeTextField` placeholder "예) 얼음 적게 부탁드려요".
- 하단: "총 결제 금액" `body` + **"10,000원"** `display`/`ink` (우측 큰 금액). 폭 꽉 찬 **"결제하기"** `CafeButton`(primary).
- 빈 장바구니: EmptyView("담은 메뉴가 없어요" + "메뉴 보러가기"). 체크아웃 진행 중 버튼 비활성(중복 가드).

## PAY (결제) — `주문 - 07(결제).png`
- `CafeTopBar`: 좌 `‹`, 중앙 "결제".
- **정보 배너**(info, `accent-soft`/`muted` 톤): "ⓘ PG 미연동 — Mock 성공/실패 분기로 대체".
- "결제 수단" 라벨 + 3분할/칩 [신용카드(선택=`surface-dark`/`on-dark`)] [간편결제] [쿠폰].
- "주문 요약" 카드(`surface-card`): 항목 "바닐라라떼 (ICE/Reg) ✕ 1 ... 5,500원", "아메리카노 (HOT/Reg) ✕ 1 ... 4,500원",
  구분선, **"총 결제 금액" ... "10,000원"** `ink` 강조.
- 하단 2버튼(Mock): **"결제 실패"**(secondary) · **"결제 성공"**(`primary`). 성공 → `ORDER_OK`, 실패 → `ORDER_FAIL` 다이얼로그.
- 멱등키 사용, 처리 중 비활성, Unknown은 성공 처리 금지(`ARCHITECTURE`/`SECURITY`).

## ORDER_OK (주문 성공) — `주문 - 08 (주문 성공).png`
- 풀스크린(`canvas`), 우상단 `✕`(홈/이전으로).
- 중앙 큰 **코랄 원 + 체크**(`primary`/`on-primary`). 아래 **"주문이 완료됐어요"** `display`/`ink`,
  "준비가 끝나면 알림을 보내드릴게요" `body`/`muted`.
- **다크 요약 카드**(`surface-dark`): "주문 번호" `muted` + **"A-2543"** `h1`/`on-dark`. 행: 픽업 매장 "카페민수 강남점",
  예상 완성 "약 8분 후", 결제 금액 "8,500원" (라벨 `muted`/값 `on-dark`, 우측 정렬).
- **스탬프 적립 배너**(`surface-card`): ☆ "스탬프 1개가 적립됐어요 (8/10)".
- 하단 버튼: **"주문 상태 보기"**(`primary`) → `HISTORY`(해당 주문) · **"홈으로 이동"**(secondary) → `HOME`.

## ORDER_FAIL (주문 실패) — `주문 - 09 (주문 실패).png`
- 스크림 + 중앙 **다이얼로그 카드**(`canvas`, 큰 라운드). 상단 **빨간 원 + ✕**(`error` 연한 배경/`error` 아이콘).
- "결제에 실패했어요" `h2`/`ink`, "카드 한도 초과 또는 정보 오류로 / 결제가 처리되지 않았어요." `body`/`muted`.
- 에러코드 칩 "ERR_PAY_LIMIT_EX"(`surface-card`/`muted`, mono 느낌).
- 버튼 2개: **"취소"**(secondary) · **"다시 시도"**(`primary`, 같은 멱등키로 재시도).

## MY (마이) — `MY - 01.png`
- 헤더 "MY" `h1`/`ink` + 우상단 설정 기어 아이콘.
- **프로필 다크 카드**(`surface-dark`): 좌측 원형 아바타(이니셜 "민", `primary` 배경/`on-primary`),
  이름 "진지원 님" `h2`/`on-dark` + 등급 배지 "GOLD"(`accent-soft`/`primary` pill). 하단 3분할 stats(구분선 세로):
  "12 / 주문", "7/10 / 스탬프", "3 / 쿠폰" (숫자 `h2`/`on-dark`, 라벨 `caption`/`muted`).
- **빠른 메뉴** `h2`: 4개 아이콘 버튼(`surface-card`, 라운드): 주문내역 / 선물하기 / 쿠폰 / 알림설정 (아이콘 `primary`, 라벨 `caption`).
- **설정 리스트**(행 + `›`, 구분선 `hairline`): 이용 약관 / 자주 묻는 질문 / 고객센터(우측 "1588-1234" `muted`) /
  버전 정보(우측 "v1.0.0" `meta`) / **로그아웃**(`primary` 텍스트) → `ST_LOGOUT` 다이얼로그.
- 하단 탭바(MY 활성).

## COUPON (쿠폰) — `MY - 02 (쿠폰).png`
- `CafeTopBar`: 좌 `‹`, 중앙 "쿠폰".
- **스탬프 다크 카드**(`surface-dark`): "강남점 스탬프" `caption`/`muted` + **"7 / 10"** `display`/`on-dark`,
  우상단 흰 사각(스탬프 아이콘/배지). 안내 "스탬프 3개만 더 모으면 무료 음료 쿠폰!" `caption`/`primary`.
  **스탬프 그리드**(10칸, 2행): 채워진 칸=`primary` 원+체크(`on-primary`), 빈 칸=어두운 원+숫자 8/9/10 `muted`.
- "보유 쿠폰 (3)" 라벨. 쿠폰 카드(`surface-card`): 좌측 쿠폰 아이콘(코랄), 제목 `h3`/`ink`("무료 음료 1잔 쿠폰" / "₩10,000" / "₩8,500"),
  "유효기간 2026.08.31" `caption`/`muted`, 우측 `›`. 곧 만료는 `warning`. 사용됨/만료는 딤+상태.

## GIFT (선물하기) — `MY - 03 (선물하기).png`
- `CafeTopBar`: 좌 `‹`, 중앙 "선물하기".
- **기프티콘 미리보기 카드**(`primary` 배경, 큰 라운드): "✱ CAFEMINSO" `on-primary`, **"₩ 10,000"** `display`/`on-primary`,
  "금액형 기프티콘" `on-primary` 저강조.
- "금액 선택" 라벨 + 4분할 [5,000] [10,000(선택=`surface-dark`/`on-dark`)] [20,000] [직접입력].
- "받는 방식" 라벨 + 2카드 [카카오톡 / "친구 선택"(선택)] [문자 (SMS) / "연락처 입력"].
- "받는 사람" + `CafeTextField` placeholder "카카오 친구 선택". "선물 메시지 (선택)" + 멀티라인 입력 "오늘 하루 수고 많았어 ☕".
- 하단 폭 꽉 찬 **"구매하고 선물 보내기 · 10,000원"** `CafeButton`(primary). 실연동(카카오) or Mock 전송. 수신자/토큰 로깅 최소화.

## HISTORY (주문내역) — `MY - 04 (주문내역).png`, empty: `MY - 05 (주문내역 empty).png`
- `CafeTopBar`: 좌 `‹`, 중앙 "주문내역".
- **진행중 주문 다크 카드**(`surface-dark`): 라벨 "진행중인 주문" `muted` + 우측 라이브 점(`success`).
  주문번호 **"#A-2419"** `h1`/`on-dark`. **단계 스텝퍼**: 접수 → 수락 → 준비중 → 완료 (지난 단계 `primary` 채움+연결선,
  현재 단계 강조, 미도달 `muted`). 하단 "바닐라라떼 외 1개" `body` + 우측 "10,000원" `on-dark`. 탭 → 주문 상세/상태.
- "지난 주문" `caption`/`muted`. 지난 주문 카드(`surface-card`): 매장명 "강남역" `h3` + 우측 금액,
  날짜 "어제 14:32"/"6월 4일" `caption`/`muted`, 품목 요약 "아메리카노 ✕ 2"/"바닐라라떼, 크로플" `body`,
  하단 폭 꽉 찬 **"↻ 재주문"** 버튼(secondary, `hairline`).
- **빈 상태**(`MY-05`): 중앙 영수증 아이콘(`accent-soft` 원 + `primary` 라인), "아직 주문 내역이 없어요" `h2`/`ink`,
  "첫 번째 한 잔을 주문해보세요" `body`/`muted`. (중앙 정렬 예외 허용 — 빈 상태)

## ST_LOADING (로딩) — `M-S2 Loading.png`
- 배경 `canvas`. 중앙 **코랄 아크 스피너**(`primary`, 절제된 회전), 아래 "메뉴 정보를 불러오고 있어요" `h3`/`ink`,
  "잠시만 기다려주세요" `body`/`muted`. 하단에 **스켈레톤 카드 2개**(`surface-card`/`hairline` 블록). 전체 블로킹 스피너 남용 금지.
- `ui/components`의 `LoadingView`를 이 디자인에 맞춰 정렬(스켈레톤 + 아크).

## ST_NETERR (네트워크 오류) — `M-S4 Network Error.png`
- `CafeTopBar`: 좌 `‹`, 중앙 "오류". 중앙 **연한 원(`accent-soft`/`error` 톤) + Wi-Fi off 아이콘(`error`)**.
- "연결에 실패했어요" `h2`/`ink`, "네트워크 상태를 확인하고 / 다시 시도해주세요." `body`/`muted`.
  에러코드 칩 "ERR_NETWORK_408"(`surface-card`/`muted`).
- 하단 폭 꽉 찬 **"다시 시도"** `CafeButton`(primary) + 그 아래 텍스트버튼 "고객센터 문의"(`muted`/ghost).
- `ui/components`의 `ErrorView`를 이 디자인(에러코드·고객센터)에 맞춰 정렬.

## ST_LOGOUT (로그아웃 확인) — `M-S7 Logout Confirm.png`
- 스크림 + 중앙 **다이얼로그**(`canvas`): "로그아웃 하시겠어요?" `h2`/`ink`,
  "로그인 정보를 잊지 않도록 / 계정 정보를 확인해주세요." `body`/`muted`.
  버튼 2개: **"취소"**(secondary) · **"로그아웃"**(`primary`) → `clearSession()`(토큰/민감데이터 와이프) → `LOGIN`.
- 상단 토스트 예시(스낵바 스타일): "✓ 스탬프 1개가 적립됐어요"(`surface-dark`/`on-dark`, 성공 아이콘 `success`).

---

## 점주(매장 관리자) 화면
점주 화면은 고객과 **동일 토큰·컴포넌트**를 재사용한다(점주 전용 색/토큰 신규 금지). 진입은 `LOGIN` 하단 "점주 로그인 →" 링크.
하단 탭바 **대시보드 · 주문 · 메뉴 · 매출 (4개)**: 활성 `primary`(라벨+상단 점), 비활성 `muted`, 상단 1px `hairline`.
주문 상태는 기존 `OrderStatus`(Accepted/Preparing/Ready/Completed) 재사용. 모든 데이터 화면은 4상태(Loading/Empty/Error/Offline) 유지.

## OWNER_LOGIN (점주 로그인) — `점주 - 00 (로그인).png`
- `CafeTopBar`: 좌 `←`, 중앙 "점주 로그인". 배경 `canvas`.
- 코랄 애스터리스크 `*`(`primary`) + "매장 관리자 로그인" `h1`/`ink` + "카페민수 매장 계정으로 로그인하세요." `body`/`muted`.
- **아이디** 라벨 `caption`/`ink` + `CafeTextField`(placeholder "아이디를 입력하세요", `surface-card`).
- **비밀번호** 라벨 + `CafeTextField`(placeholder "비밀번호를 입력하세요", **마스킹**, 화면/로그 노출 금지).
- 하단 폭 꽉 찬 **"로그인"** `CafeButton`(primary) → `OwnerAuthProvider.login(id, pw)` → 성공 시 `OWNER_HOME`, 실패 스낵바.

## OWNER_HOME (점주 대시보드) — `점주 - 01 (대시보드).png`
- 상단: 매장명 **"강남점 ▾"** `h1`/`ink`(매장 선택, MVP는 드롭다운 표시만) + 우측 **"● 영업중" 토글 pill**(`surface-card`, `success` 점, ON/OFF로 `setStoreOpen`).
- "6월 19일 (금)" `caption`/`muted` + **"오늘의 매장 현황"** `h2`/`ink`.
- **3 stat 카드 row**(`surface-card`): 라벨 `caption`/`muted` + 값 `h2`/`ink`. 오늘 매출 "₩482,000" · 주문 "37건" · 신규 대기 "3건"(값 `primary` 강조).
- **"지금 처리할 주문"** `h2` + 우측 "전체 보기 →"(`primary`) → `OWNER_ORDERS`. 주문 카드(`surface-card`):
  주문번호 "#1042" + "· 오후 2:14" `caption`, 우측 상태 점+텍스트("● 신규" `warning` / "● 준비중" `primary`),
  품목 "아메리카노(L) ICE 외 1" `body`/`ink`, 금액 "₩9,300" `primary`, 우측 액션 버튼(신규=**"접수하기"**, 준비중=**"준비완료"**) → `advanceStatus`.
- 하단 탭바(대시보드 활성).

## OWNER_ORDERS (실시간 주문 관리) — `점주 - 02 (주문 관리).png`
- 상단 "주문 관리" `h1`/`ink` + 우측 "● 실시간" `caption`(`success` 점).
- **필터 칩 row**(`CafeChip`): "신규 3"(선택=`primary`) · "준비중 5" · "준비완료 2"(개수는 상태별 카운트).
- 주문 카드(`surface-card`): 주문번호 "#1042" `h3` + "오후 2:14" `caption`/`muted`, 우측 "● 신규" 상태.
  품목 멀티라인 "아메리카노 (L) · ICE · 1 / 바닐라라떼 (R) · HOT · 1" `body`, 요청 "포장 · 요청: 얼음 적게" `caption`/`muted`,
  금액 "₩9,300" `primary` + 우측 액션 버튼(신규="접수하기", 준비중="준비완료", 준비완료="픽업완료") → `advanceStatus`(접수→준비중→준비완료→픽업완료).
- 빈 상태 `EmptyView`("새 주문이 없어요"). 하단 탭바(주문 활성).

## OWNER_MENU (메뉴 관리) — `점주 - 03 (메뉴 관리).png`
- 상단 "메뉴 관리" `h1`/`ink` + 우측 **"+ 메뉴 추가"** `primary` 텍스트버튼 → `OWNER_MENU_ADD`.
- **카테고리 칩 row**(`CafeChip`): "전체"(선택=`primary`) · "커피" · "논커피" · "디저트".
- 메뉴 행(`surface-card`): 메뉴명 "아메리카노" `h3`/`ink`, 가격 "₩4,500" `primary`, 상태 점+텍스트("● 판매중" `success` / "● 품절" `error`),
  우측 **토글 스위치**: ON=`primary`(판매중), OFF=`hairline`(품절) → `setSoldOut`. **품절 행은 셀 디밍(`muted`)** + `error` "품절" 태그.
- 하단 탭바(메뉴 활성).

## OWNER_MENU_ADD (메뉴 추가) — `점주 - 05 (메뉴 추가).png`
`OWNER_MENU` 우측 "+ 메뉴 추가"로 진입하는 신규 메뉴 등록 폼. 점주 화면 공통 토큰/컴포넌트 재사용(전용 색/토큰 신규 금지).
- `CafeTopBar`: 좌 `←`(취소·복귀), 중앙 "메뉴 추가" `h2`/`ink`. 배경 `canvas`.
- **대표 사진 추가**: 점선(`hairline`, dash) 보더 + `surface-card` 업로드 박스(`radius-lg`). 중앙 `＋` + "대표 사진 추가" `caption`/`muted`. 탭 시 갤러리/카메라(권한 rationale, 이미지 1장).
- **카테고리** `*필수`: `CafeChip` row "커피"(기본 선택=`primary`) · "논커피" · "디저트". 단일 선택.
- **메뉴명** `*필수`: `CafeTextField`(placeholder "메뉴 이름을 입력하세요", `surface-card`). 공백·길이 검증.
- **가격** `*필수`: `₩` prefix + `CafeTextField`(숫자 키패드, placeholder "0"). 0 초과 정수만, 금액 입력 검증.
- **설명**(선택): 멀티라인 텍스트영역(`surface-card`, placeholder "메뉴 설명을 입력하세요 (선택)").
- **판매 상태** 카드(`surface-card`): "판매 상태" `h3`/`ink` + "등록 즉시 판매중으로 표시됩니다" `caption`/`muted` + 우측 토글(ON=`primary`/판매중, OFF=`hairline`/품절).
- 하단 고정 바(상단 1px `hairline`): 폭 꽉 찬 **"저장하기"** `CafeButton`(primary) → 필수값 검증 → `addMenu(...)`. 등록 액션은 **낙관적 UI 금지**(확정 후 `OWNER_MENU` 복귀 + 토스트), 중복탭 가드. 실패 시 스낵바.

## OWNER_SALES (매출·정산) — `점주 - 04 (매출·정산).png`
- 상단 "매출 · 정산" `h1`/`ink`.
- **기간 세그먼트**(`surface-card` pill 3분할): 오늘 · **이번 주**(선택=흰 배경/`ink`) · 이번 달 → `SalesPeriod`.
- "이번 주 매출" `caption`/`muted` + 큰 숫자 **"₩2,840,000"** `display`/`primary` + **"▲ 12% 지난주 대비"** `caption`/`success`(증감 부호색).
- **"요일별 매출"** 카드(`surface-card`): 막대 차트(일~토, `dailySales`). 최고/당일 막대 `primary`, 나머지 `accent-soft`. 축 라벨 `meta`/`muted`.
- **"인기 메뉴"** `h2`. 순위 행: 순위 숫자 "1" `primary`, 메뉴명 "아메리카노" `h3` + "142잔" `caption`/`muted`, 우측 금액 "₩639,000" `bodyL`/`ink`.
- **"정산 예정 금액"** 다크 카드(`surface-dark`): 좌측 라벨 `muted` + "6월 24일 입금 예정" `caption`, 우측 "₩2,556,000" `h2`/`on-dark`.
- 하단 탭바(매출 활성).

---

## 공통 규칙 요약
- 모든 데이터 화면은 Loading/Empty/Error/Offline 상태(`DataUiStateContent`) + 보호 화면은 NeedsLogin.
- CTA는 화면당 primary 1개 원칙. 가격·강조 수치 `primary`. 카드 타입별 radius 구분(`lg`/`xl`).
- 금전 액션(결제·주문)은 낙관적 UI 금지 — 확정 후 표시. 멱등키·중복탭 가드.
- 카카오 로그인 버튼의 카카오 옐로우만 브랜드 예외 색으로 허용(그 외 hex 금지, `Color.kt`에 상수로).
