package com.ternaryop.photoshelf.adapter

/**
 * Created by dave on 11/06/2019.
 * Checkbox item, contains the checked status
 */
data class CheckBoxItem<T>(var checked: Boolean, val item: T)
