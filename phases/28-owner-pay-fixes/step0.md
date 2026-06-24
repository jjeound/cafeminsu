# Step 0: menu-image-upload (점주 메뉴 이미지 업로드 API 연동)

점주가 메뉴를 등록할 때 선택한 이미지가 **업로드되지 않고 로컬 `content://` Uri 문자열이 그대로
`imageUrl` 로 서버에 전송**되는 버그를 고친다. 라이브 스웨거(`/v3/api-docs`)에 **전용 이미지 업로드
엔드포인트**가 있다: `POST /api/images/menu` (multipart/form-data, 파트명 `file`, 점주 Bearer) → `{ imageUrl }`.
업로드로 받은 https `imageUrl` 을 메뉴 생성(`createMenu`)에 사용한다. 이 step 은 **remote + platform + data 레이어**.

> **주의(레이어)**: `ContentResolver` 로 Uri 바이트를 읽는 Android `Context` 의존은 **플랫폼 레이어**
> (`data/platform/`, 예: `RealKakaoPayRedirectBridge` 패턴)에만 둔다. 도메인/Repository 는 Context 비종속 유지.

## 읽어야 할 파일

- `/docs/SERVER_INTEGRATION.md` (응답 구조: DTO 직접 반환, `runCatchingToAppResult`/`AppResult` 규칙)
- `/docs/SECURITY.md` (HTTPS 강제, PII/토큰 미로깅)
- `/docs/ARCHITECTURE.md` (레이어 의존 방향, 예외 비전파)
- `app/src/main/java/com/cafeminsu/data/remote/MenuApi.kt` · `OwnerMenuApi.kt` (Retrofit 계약·DTO 패턴)
- `app/src/main/java/com/cafeminsu/data/remote/NetworkModule.kt` (Bearer `Retrofit` 의 api provide 패턴)
- `app/src/main/java/com/cafeminsu/data/repository/RealOwnerMenuRepository.kt` (`addMenu()` → `toMenuCreateReq()` → `createMenu`)
- `app/src/main/java/com/cafeminsu/data/mapper/MenuMapper.kt` (`NewMenuDraft.toMenuCreateReq()`)
- `app/src/main/java/com/cafeminsu/domain/model/Menu.kt` (`NewMenuDraft.imageUrl`)
- `app/src/main/java/com/cafeminsu/ui/feature/owner/menu/OwnerMenuAddViewModel.kt` (`imageUri` → `NewMenuDraft.imageUrl`)
- `app/src/main/java/com/cafeminsu/data/platform/RealKakaoPayRedirectBridge.kt` (`@ApplicationContext` 플랫폼 의존 패턴)
- `app/src/main/java/com/cafeminsu/core/AppResult.kt`, `data/remote/runCatchingToAppResult` 위치
- `app/src/test/java/com/cafeminsu/data/repository/RealOwnerMenuRepositoryTest.kt`
- `app/src/test/java/com/cafeminsu/data/remote/OwnerMenuApiTest.kt` (MockWebServer 테스트 패턴)

## 작업

1. **ImageApi (Retrofit, multipart)** — `data/remote/ImageApi.kt`:
   ```kotlin
   @Multipart
   @POST("api/images/menu")
   suspend fun uploadMenuImage(@Part file: MultipartBody.Part): ImageUploadRes
   ```
   - `ImageUploadRes(imageUrl: String?)` DTO. Bearer 필요 → 기본(`@Unauthenticated` 아닌) `Retrofit` 사용.
   - `NetworkModule` 에 `provideImageApi(retrofit)` 추가.

2. **이미지 업로더 (플랫폼)** — `data/platform/MenuImageUploader.kt`:
   - 인터페이스: `suspend fun upload(localUri: String): AppResult<String>` (반환=https imageUrl).
   - 구현 `RealMenuImageUploader @Inject constructor(@ApplicationContext context, imageApi)`:
     `content://` Uri → `contentResolver.openInputStream` 로 바이트 읽기 → `MultipartBody.Part.createFormData("file", fileName, bytes.toRequestBody(mime))` → `imageApi.uploadMenuImage(part)` → `imageUrl` 반환.
   - 모든 호출 `runCatchingToAppResult`/`AppResult` 래핑. URI 읽기 실패/빈 응답 → `DomainError.Unknown`/`Validation`.
   - DI: 인터페이스↔구현 `@Binds`(또는 `RepositoryModule`/신규 모듈). 도메인에 노출하지 말고 data 레이어 내부에서만 사용.

3. **RealOwnerMenuRepository.addMenu() 연동**:
   - `MenuImageUploader` 주입. `draft.imageUrl` 이 `content://`(로컬)면 **먼저 업로드** → 받은 https URL 로 교체한 draft 로 `createMenu` 호출.
   - 이미 `http(s)://` 이거나 `null` 이면 업로드 건너뛰고 그대로(폴백은 step5 표시 레이어 담당).
   - 업로드 실패 시 메뉴 생성으로 진행하지 말고 `AppResult.Failure` 반환(낙관 금지).

### 핵심 규칙 (반드시 준수)

- **레이어**: `ContentResolver`/`Context` 의존은 `data/platform/` 에만. Repository/도메인은 Context 비종속.
- **AppResult 래핑**: 업로드/네트워크 호출에서 예외를 던지지 마라(`UiState.Error` 변환 가능하게).
- **미로깅**: 업로드 바이트/URL 에 PII 가정 — 토큰/응답 본문 과다 로깅 금지(`SECURITY §3` 인터셉터 redact 유지).
- **무회귀**: 이미지 없이(또는 http URL) 등록하던 기존 경로·테스트가 그대로 통과해야 한다.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## 검증 절차

1. **테스트 우선(TDD)**: 구현 전 실패 테스트 작성.
   - `ImageApi` MockWebServer 테스트(`data/remote/`): multipart `POST api/images/menu` → `{imageUrl}` 파싱.
   - `RealOwnerMenuRepository` 테스트: Fake `MenuImageUploader`(고정 https URL 반환)로 `content://` draft → `createMenu` 가 **https URL** 로 호출되는지, 업로드 실패 시 생성 미진행 검증.
2. 위 AC 통과. **기존** `RealOwnerMenuRepositoryTest`·`OwnerMenuApiTest` **무회귀**.
3. 결과에 따라 `phases/28-owner-pay-fixes/index.json` 의 step 0 업데이트:
   - 성공 → `"status": "completed"`, `"summary": "ImageApi(multipart POST api/images/menu) + RealMenuImageUploader(플랫폼, ContentResolver→multipart) + RealOwnerMenuRepository.addMenu 가 content:// 이미지를 업로드 후 https imageUrl 로 메뉴 생성(낙관금지)"`
   - 3회 실패 → `"status": "error"`, `"error_message": "<에러>"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "<사유>"` 후 중단

## 금지사항

- Repository/도메인에 Android `Context`/Activity 를 주입하지 마라(레이어 비종속).
- 업로드 실패를 무시하고 로컬 Uri 로 메뉴를 만들지 마라(낙관 금지).
- UI(OwnerMenuAddScreen) 의 이미지 피커/미리보기는 건드리지 마라(이미 동작). 기존 테스트를 깨뜨리지 마라.
