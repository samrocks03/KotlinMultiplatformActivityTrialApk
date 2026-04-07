package com.example.kmp_basic_app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KMP_Basic_App",
    ) {
        App()
    }
}