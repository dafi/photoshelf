@file:Suppress("unused")

package com.ternaryop.photoshelf.di

import android.app.Application
import com.ternaryop.photoshelf.BuildConfig
import com.ternaryop.photoshelf.feedly.prefs.FeedlyPrefs
import com.ternaryop.photoshelf.feedly.reader.ApiStreamContentReader
import com.ternaryop.photoshelf.feedly.reader.AssetManagerStreamContentReader
import com.ternaryop.photoshelf.feedly.reader.StreamContentReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StreamContentReaderModule {

    @Singleton
    @Provides
    fun provideStreamContentReader(application: Application): StreamContentReader {
        return if (BuildConfig.DEBUG) {
            AssetManagerStreamContentReader(application.assets, "sample/feedly.json")
        } else {
            ApiStreamContentReader(FeedlyPrefs(application))
        }
    }
}
