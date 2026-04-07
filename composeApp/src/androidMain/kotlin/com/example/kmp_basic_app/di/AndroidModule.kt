package com.example.kmp_basic_app.di

import com.example.kmp_basic_app.data.local.DatabaseDriverFactory
import com.example.kmp_basic_app.platform.PlatformLocationProvider
import com.example.kmp_basic_app.viewmodel.CameraViewModel
import com.example.kmp_basic_app.viewmodel.CharacterDetailViewModel
import com.example.kmp_basic_app.viewmodel.CharactersViewModel
import com.example.kmp_basic_app.viewmodel.FavoritesViewModel
import com.example.kmp_basic_app.viewmodel.LocationViewModel
import com.example.kmp_basic_app.viewmodel.PostsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(get()) }
    single { PlatformLocationProvider(get()) }
    viewModel { CharactersViewModel(get(), get()) }
    viewModel { CharacterDetailViewModel(get(), get(), get()) }
    viewModel { PostsViewModel(get(), get(), get(), get()) }
    viewModel { CameraViewModel(get(), get()) }
    viewModel { LocationViewModel(get()) }
    viewModel { FavoritesViewModel(get(), get()) }
}
