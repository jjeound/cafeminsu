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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.ui.navigation.AppNavHost
import com.cafeminsu.ui.theme.CafeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var ownerAuthProvider: OwnerAuthProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CafeTheme {
                RequestNotificationPermission()
                AppNavHost(
                    sessionRepository = sessionRepository,
                    ownerAuthProvider = ownerAuthProvider,
                )
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
