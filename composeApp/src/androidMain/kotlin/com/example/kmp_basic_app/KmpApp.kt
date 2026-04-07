package com.example.kmp_basic_app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.example.kmp_basic_app.di.androidModule
import com.example.kmp_basic_app.di.initKoin
import org.koin.android.ext.koin.androidContext

class KmpApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidModule).apply {
            androidContext(this@KmpApp)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }
}
