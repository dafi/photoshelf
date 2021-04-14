package com.ternaryop.photoshelf.fragment

import android.app.Activity
import androidx.fragment.app.FragmentFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface FragmentFactoryEntryPoint {
    val fragmentFactory: FragmentFactory
}

val Activity.appFragmentFactory: FragmentFactory
    get() {
        return EntryPointAccessors.fromActivity(
            this,
            FragmentFactoryEntryPoint::class.java
        ).fragmentFactory
    }
