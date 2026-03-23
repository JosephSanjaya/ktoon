package io.ktoon.navigation

sealed class Screen {
    data object Home : Screen()
    data object KtorDemo : Screen()
}
