package com.ternaryop.photoshelf.di

import android.app.Application
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.TagMatcherDAO
import com.ternaryop.photoshelf.db.TumblrPostCacheDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The current implementation calls DBHelper so the provided methods don't need to be declared as singleton
 */
@Module
@InstallIn(SingletonComponent::class)
object DAOModule {

    @Provides
    fun tumblrPostCacheDAO(application: Application): TumblrPostCacheDAO =
        DBHelper.getInstance(application).tumblrPostCacheDAO

    @Provides
    fun tagMatcherDAO(application: Application): TagMatcherDAO =
        DBHelper.getInstance(application).tagMatcherDAO
}
