package com.example.kmp_basic_app.di

import com.example.kmp_basic_app.data.local.DatabaseDriverFactory
import com.example.kmp_basic_app.data.remote.createGraphQLZeroApolloClient
import com.example.kmp_basic_app.data.remote.createRickAndMortyApolloClient
import com.example.kmp_basic_app.data.repository.CharacterRepositoryImpl
import com.example.kmp_basic_app.data.repository.FavoriteRepositoryImpl
import com.example.kmp_basic_app.data.repository.PhotoRepositoryImpl
import com.example.kmp_basic_app.data.repository.PostRepositoryImpl
import com.example.kmp_basic_app.db.AppDatabase
import com.example.kmp_basic_app.domain.repository.CharacterRepository
import com.example.kmp_basic_app.domain.repository.FavoriteRepository
import com.example.kmp_basic_app.domain.repository.PhotoRepository
import com.example.kmp_basic_app.domain.repository.PostRepository
import com.example.kmp_basic_app.domain.usecase.CapturePhotoUseCase
import com.example.kmp_basic_app.domain.usecase.CreatePostUseCase
import com.example.kmp_basic_app.domain.usecase.DeletePostUseCase
import com.example.kmp_basic_app.domain.usecase.GetCharacterDetailUseCase
import com.example.kmp_basic_app.domain.usecase.GetCharactersUseCase
import com.example.kmp_basic_app.domain.usecase.GetCurrentLocationUseCase
import com.example.kmp_basic_app.domain.usecase.GetFavoritesUseCase
import com.example.kmp_basic_app.domain.usecase.GetPostsUseCase
import com.example.kmp_basic_app.domain.usecase.ToggleFavoriteUseCase
import com.example.kmp_basic_app.domain.usecase.UpdatePostUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val sharedModule = module {
    single(named("rickandmorty")) { createRickAndMortyApolloClient() }
    single(named("graphqlzero")) { createGraphQLZeroApolloClient() }
    single { get<DatabaseDriverFactory>().create() }
    single { AppDatabase(get()) }
    single<CharacterRepository> { CharacterRepositoryImpl(get(named("rickandmorty"))) }
    single<PostRepository> { PostRepositoryImpl(get(named("graphqlzero"))) }
    single<FavoriteRepository> { FavoriteRepositoryImpl(get()) }
    single<PhotoRepository> { PhotoRepositoryImpl(get()) }
    factory { GetCharactersUseCase(get(), get()) }
    factory { GetCharacterDetailUseCase(get(), get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { GetFavoritesUseCase(get()) }
    factory { GetPostsUseCase(get()) }
    factory { CreatePostUseCase(get()) }
    factory { UpdatePostUseCase(get()) }
    factory { DeletePostUseCase(get()) }
    factory { CapturePhotoUseCase(get(), get(), get()) }
    factory { GetCurrentLocationUseCase(get()) }
}
