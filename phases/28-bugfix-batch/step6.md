# Step 6 — 점주 메뉴 추가 시 이미지 추가 동작 검증/수정

## 배경 / 요구
점주 메뉴 추가(`ui/feature/owner/menu/OwnerMenuAddScreen.kt` + `OwnerMenuAddViewModel.kt`)는 이미지를 고를 수
있고(`onImagePicked(uri)` → `imageUri`, `AsyncImage` 미리보기), 저장 시
`NewMenuDraft(imageUrl = state.imageUri)` → `OwnerMenuRepository.addMenu(draft)` 로 보낸다.

문제: `imageUri` 는 **로컬 `content://` URI** 인데 이게 그대로 JSON `MenuCreateReq.imageUrl` 로 전송된다
(`RealOwnerMenuRepository.addMenu` → `ownerMenuApi.createMenu`). 실제 **이미지 업로드(멀티파트)** 가 없어
서버엔 의미 없는 값이 가고, 로컬 스냅샷은 `imageUrl = null`(`RealOwnerMenuRepository` 약 140행)로 떨군다.
요구: **메뉴 추가 시 이미지 추가가 실제로 동작**하도록 검증하고 고친다.

## 작업 범위 (이 step에서만)
1. **현황 조사**: 메뉴 추가 이미지 경로(picker → `imageUri` → `NewMenuDraft` → `createMenu`/로컬 스냅샷)를
   조사한다. 백엔드 스웨거(`https://cafeminsu.duckdns.org/`)/`OwnerMenuApi` 에 **이미지 업로드/멀티파트
   엔드포인트가 있는지** 확인한다.
2. **업로드 배선(가능하면)**: 업로드 엔드포인트가 있으면, 선택 이미지를 업로드해 받은 URL 을
   `MenuCreateReq.imageUrl` 로 보내고 로컬 스냅샷에도 반영한다. 그래야 메뉴 목록/추가 후에 이미지가 보인다.
3. **업로드 엔드포인트가 없거나 키/서버 설정이 필요하면**: 잘못된 `content://` 를 imageUrl 로 보내지 않도록
   정리(전송에서 제외하거나 빈 값 처리)하고, 이 step 을 **`blocked`**(blocked_reason: 서버 이미지 업로드
   엔드포인트/설정 필요)로 표시한다. 즉시 중단한다(가드레일: 사용자 개입 필요 시 blocked).
4. **테스트**: `OwnerMenuAddViewModelTest` 에 이미지 선택→저장 경로 검증(이미지 있을 때/없을 때) 추가.
   기존 점주 메뉴 추가 테스트/페이크 패턴을 따른다.

## 금지 / 불변
- HTTPS 강제·cleartext 금지. 업로드 페이로드/토큰 로깅 금지. 비밀키 하드코딩 금지.
- 디자인 토큰/한국어 카피 가드레일 준수. 점주 메뉴 추가의 기존 검증/네비 동작 유지.

## AC
```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
통과 시 step 6 을 `completed` + `summary` 로, 서버 개입 필요 시 `blocked` + `blocked_reason` 으로 갱신·커밋.
