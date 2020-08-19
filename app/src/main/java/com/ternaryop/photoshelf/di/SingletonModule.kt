@file:Suppress("unused")
package com.ternaryop.photoshelf.di

import androidx.fragment.app.FragmentFactory
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.impl.ImageViewerActivityStarterImpl
import com.ternaryop.photoshelf.db.DraftCacheImpl
import com.ternaryop.photoshelf.fragment.AppFragmentFactory
import com.ternaryop.photoshelf.misspelled.MisspelledName
import com.ternaryop.photoshelf.misspelled.impl.MisspelledNameImpl
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.dialog.impl.TumblrPostDialogImpl
import com.ternaryop.photoshelf.tumblr.ui.draft.DraftCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
abstract class SingletonModule {
    @Singleton
    @Binds
    abstract fun bindImageViewerActivityStarter(
        imageViewerActivityStarter: ImageViewerActivityStarterImpl
    ): ImageViewerActivityStarter

    @Singleton
    @Binds
    abstract fun bindMisspelledName(
        misspelledName: MisspelledNameImpl
    ): MisspelledName

    @Singleton
    @Binds
    abstract fun bindDraftCache(
        draftCache: DraftCacheImpl
    ): DraftCache

    /**
     * This is a factory object, don't try to use it with @Singleton otherwise the app may crash
     */
    @Binds
    abstract fun bindTumblrPostDialogModule(
        tumblrPostDialog: TumblrPostDialogImpl
    ): TumblrPostDialog

    @Singleton
    @Binds
    abstract fun bindAppFragmentFactory(
        appFragmentFactory: AppFragmentFactory
    ): FragmentFactory
}
