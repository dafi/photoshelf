package com.ternaryop.photoshelf.di

import androidx.fragment.app.FragmentFactory
import com.ternaryop.photoshelf.BuildConfig
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.MainActivityViewModel
import com.ternaryop.photoshelf.activity.impl.ImageViewerActivityStarterImpl
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.birthday.browser.fragment.BirthdayBrowserViewModel
import com.ternaryop.photoshelf.birthday.publisher.fragment.BirthdayPublisherViewModel
import com.ternaryop.photoshelf.birthday.repository.BirthdayRepository
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.DraftCacheImpl
import com.ternaryop.photoshelf.feedly.fragment.FeedlyViewModel
import com.ternaryop.photoshelf.feedly.prefs.FeedlyPrefs
import com.ternaryop.photoshelf.feedly.reader.ApiStreamContentReader
import com.ternaryop.photoshelf.feedly.reader.AssetManagerStreamContentReader
import com.ternaryop.photoshelf.fragment.AppFragmentFactory
import com.ternaryop.photoshelf.home.fragment.HomeViewModel
import com.ternaryop.photoshelf.imagepicker.fragment.ImagePickerViewModel
import com.ternaryop.photoshelf.imagepicker.repository.ImageGalleryRepository
import com.ternaryop.photoshelf.misspelled.MisspelledName
import com.ternaryop.photoshelf.misspelled.impl.MisspelledNameImpl
import com.ternaryop.photoshelf.repository.tumblr.TumblrRepository
import com.ternaryop.photoshelf.tagphotobrowser.fragment.TagPhotoBrowserViewModel
import com.ternaryop.photoshelf.tumblr.dialog.PostViewModel
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.dialog.impl.TumblrPostDialogImpl
import com.ternaryop.photoshelf.tumblr.postaction.impl.PostActionExecutorImpl
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostActionExecutor
import com.ternaryop.photoshelf.tumblr.ui.draft.DraftCache
import com.ternaryop.photoshelf.tumblr.ui.draft.fragment.DraftListViewModel
import com.ternaryop.photoshelf.tumblr.ui.publish.fragment.PublishedPostsListViewModel
import com.ternaryop.photoshelf.tumblr.ui.schedule.fragment.ScheduledListViewModel
import com.ternaryop.tumblr.android.TumblrManager
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val repositoryModule = module {
    single { BirthdayRepository() }
    single { TumblrRepository(get()) }
    single { ImageGalleryRepository(get()) }
}

val uiModule = module {
    viewModel { MainActivityViewModel(get(), get(), get()) }
    viewModel { DraftListViewModel(get(), get(), get()) }
    viewModel { ScheduledListViewModel(get(), get()) }
    viewModel { BirthdayPublisherViewModel(get()) }
    viewModel { PostViewModel(get(), get()) }
    viewModel { FeedlyViewModel(get(), get()) }
    viewModel { ImagePickerViewModel(get(), get()) }
    viewModel { PublishedPostsListViewModel(get()) }
    viewModel { BirthdayBrowserViewModel(get()) }
    viewModel { TagPhotoBrowserViewModel(get()) }
    viewModel { HomeViewModel(get()) }
}

val factoryModule = module {
    single<TumblrPostDialog> { TumblrPostDialogImpl() }
    single<FragmentFactory> { AppFragmentFactory(get(), get()) }
    single<ImageViewerActivityStarter> { ImageViewerActivityStarterImpl(get()) }
    single<MisspelledName> { MisspelledNameImpl(DBHelper.getInstance(get()).tagMatcherDAO) }
    factory<PostActionExecutor> {
        PostActionExecutorImpl(TumblrManager.getInstance(get()),
            DBHelper.getInstance(get()).tumblrPostCacheDAO, ApiManager.postService())
    }
    single {
        if (BuildConfig.DEBUG) {
            AssetManagerStreamContentReader(androidApplication().assets, "sample/feedly.json")
        } else {
            ApiStreamContentReader(FeedlyPrefs(get()))
        }
    }
    single<DraftCache> {
        DraftCacheImpl(DBHelper.getInstance(get()).tumblrPostCacheDAO)
    }
}
