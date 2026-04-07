package com.example.kmp_basic_app

import androidx.compose.runtime.Composable
import com.example.kmp_basic_app.ui.navigation.AppNavigation
import com.example.kmp_basic_app.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        AppNavigation()
    }
}
