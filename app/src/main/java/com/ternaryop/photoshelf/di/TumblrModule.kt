@file:Suppress("unused")
package com.ternaryop.photoshelf.di

import android.app.Application
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.db.TumblrPostCacheDAO
import com.ternaryop.photoshelf.tumblr.postaction.impl.PostActionExecutorImpl
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostActionExecutor
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.android.TumblrManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object TumblrModule {

    @Provides
    fun providePostActionExecutor(
        tumblr: Tumblr,
        tumblrPostCacheDAO: TumblrPostCacheDAO
    ): PostActionExecutor =
        PostActionExecutorImpl(
            tumblr,
            tumblrPostCacheDAO,
            ApiManager.postService())

    @Singleton
    @Provides
    fun provideTumblr(
        application: Application
    ): Tumblr = TumblrManager.getInstance(application)
}
