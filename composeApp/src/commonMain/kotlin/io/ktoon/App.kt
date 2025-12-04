package io.ktoon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.ktoon.api.ApiClient
import io.ktoon.navigation.Screen
import io.ktoon.screens.HomeScreen
import io.ktoon.screens.KtorDemoScreen
import io.ktoon.theme.DemoAppTheme

@Composable
fun App() {
    val apiClient = remember { ApiClient() }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    
    DemoAppTheme {
        when (currentScreen) {
            Screen.Home -> {
                HomeScreen(
                    onNavigateToKtor = { currentScreen = Screen.KtorDemo }
                )
            }
            Screen.KtorDemo -> {
                KtorDemoScreen(
                    apiClient = apiClient,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}
