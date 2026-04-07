package com.example.kmp_basic_app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform