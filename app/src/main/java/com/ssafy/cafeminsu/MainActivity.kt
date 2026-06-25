package com.ssafy.cafeminsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme
import com.ssafy.cafeminsu.ui.CafeMinsuApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value.authState == MainActivityAuthState.Loading
        }

        setContent {
            CafeMinsuTheme {
                CafeMinsuApp(viewModel = viewModel)
            }
        }
    }
}
