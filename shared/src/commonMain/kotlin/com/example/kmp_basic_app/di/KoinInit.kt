package com.example.kmp_basic_app.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(platformModule: Module): KoinApplication {
    return startKoin {
        modules(sharedModule, platformModule)
    }
}
