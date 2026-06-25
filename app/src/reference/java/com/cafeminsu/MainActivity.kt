package com.cafeminsu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.cafeminsu.data.local.prefs.UserPreferencesDataStore
import com.cafeminsu.di.ApplicationScope
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.ui.navigation.AppNavHost
import com.cafeminsu.ui.navigation.resolveInitialStartDestination
import com.cafeminsu.ui.theme.CafeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var ownerAuthProvider: OwnerAuthProvider

    @Inject
    lateinit var userPreferences: UserPreferencesDataStore

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    // 시작 화면(로그인/홈/점주홈). 인증 해석 전엔 null → 시스템 스플래시 유지.
    private var startDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 초기 인증이 해석될 때까지 Android 12+ 시스템 스플래시를 유지(인앱 SplashGate 대체).
        splashScreen.setKeepOnScreenCondition { startDestination == null }

        lifecycleScope.launch {
            startDestination = resolveInitialStartDestination(sessionRepository)
        }

        setContent {
            val destination = startDestination
            if (destination != null) {
                CafeTheme {
                    RequestNotificationPermission()
                    AppNavHost(
                        sessionRepository = sessionRepository,
                        ownerAuthProvider = ownerAuthProvider,
                        startDestination = destination,
                        onCustomerTabSelected = { route ->
                            appScope.launch { userPreferences.setLastCustomerTab(route) }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Android 13+ 의 알림 게시 권한(POST_NOTIFICATIONS)을 최초 1회 요청한다.
 * 거부해도 앱은 정상 동작하며 푸시 알림만 표시되지 않는다(크래시 금지).
 */
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return
    }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 결과와 무관하게 진행 */ }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
