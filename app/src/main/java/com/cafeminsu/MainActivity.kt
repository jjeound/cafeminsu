package com.cafeminsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
                AppNavHost(
                    sessionRepository = sessionRepository,
                    ownerAuthProvider = ownerAuthProvider,
                )
            }
        }
    }
}
