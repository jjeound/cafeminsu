# Step 2 — Hilt DI 와이어링 (Repository 바인딩 + Dispatcher)

step 1의 Mock 구현을 step 0의 인터페이스에 Hilt로 연결한다. Mock↔Real 교체가 **DI 모듈 한 곳**에서만
일어나도록 둔다(`ARCHITECTURE.md §DI/모듈`).

## 만들 것 — `app/src/main/java/com/cafeminsu/di/`
1. **`RepositoryModule`** (`@Module @InstallIn(SingletonComponent::class)`, abstract class):
   - 6개 인터페이스를 `@Binds` 로 Mock 구현에 연결:
     `MenuRepository←MockMenuRepository`, `CartRepository←MockCartRepository`,
     `OrderRepository←MockOrderRepository`, `PaymentRepository←MockPaymentRepository`,
     `RewardRepository←MockRewardRepository`, `SessionRepository←MockSessionRepository`.
   - 각 Mock 구현은 `@Singleton` + `@Inject constructor(...)`로 주입 가능하게 한다(상태 보존 위해 Singleton).
2. **`DispatcherModule`** (`@Module @InstallIn(SingletonComponent::class)`, object):
   - `@Qualifier annotation class IoDispatcher` 정의 후 `@Provides @IoDispatcher fun provideIo():
     CoroutineDispatcher = Dispatchers.IO`. (Mock이 IO 디스패처를 쓰지 않더라도 이후 Real 구현/테스트
     대체를 위해 둔다.)
3. Hilt 그래프가 실제로 검증되도록 **최소 1개의 주입 지점**을 만든다:
   - `ui/feature/home/HomeViewModel.kt` 를 `@HiltViewModel class HomeViewModel @Inject constructor(
     menuRepository: MenuRepository, sessionRepository: SessionRepository) : ViewModel()` 로 추가하고,
     아직 화면 로직은 넣지 않아도 된다(주입만 — 그래프 검증 목적). `HomeScreen`에서 `hiltViewModel()`로
     획득하되 기존 플레이스홀더 UI는 유지(상태 표시는 다음 feature phase).
   - lifecycle-viewmodel-compose / hilt-navigation-compose 의존성이 없으면 카탈로그/`app`에 추가한다.

## 하지 말 것
- 화면 로직/상태 구현(다음 feature phase 소관). HomeViewModel은 주입만, 동작 추가 금지.
- Real(Retrofit/Room) 바인딩 추가 금지 — 이 phase는 Mock 바인딩만.

## Acceptance Criteria
- `./gradlew :app:assembleDebug` 가 성공한다(Hilt KSP가 그래프를 검증·생성). 직접 실행해 확인하라.
- `./gradlew :app:testDebugUnitTest` 가 여전히 성공한다.
- 통과하면 `phases/1-data/index.json`의 step 2 status를 `completed`로 바꾸고 `summary`에 한 줄 요약을 적어라.
