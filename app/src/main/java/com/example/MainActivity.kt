package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.local.PreferencesManager
import com.example.data.remote.GoogleSheetApiService
import com.example.ui.components.AnimatedLogoSplash
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.JobExpressTheme

enum class Screen {
    SPLASH,
    HOME,
    ADMIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesManager = PreferencesManager(applicationContext)
        val apiService = GoogleSheetApiService()

        setContent {
            JobExpressTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }

                    when (currentScreen) {
                        Screen.SPLASH -> {
                            AnimatedLogoSplash(
                                onAnimationFinished = {
                                    currentScreen = Screen.HOME
                                }
                            )
                        }
                        Screen.HOME -> {
                            HomeScreen(
                                preferencesManager = preferencesManager,
                                apiService = apiService,
                                onNavigateToAdmin = {
                                    currentScreen = Screen.ADMIN
                                }
                            )
                        }
                        Screen.ADMIN -> {
                            AdminDashboardScreen(
                                preferencesManager = preferencesManager,
                                apiService = apiService,
                                onBack = {
                                    currentScreen = Screen.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
