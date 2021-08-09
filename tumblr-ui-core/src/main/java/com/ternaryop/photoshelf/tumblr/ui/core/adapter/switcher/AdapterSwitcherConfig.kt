package com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher

/**
 * @param prefNamePrefix The prefix used to build the unique key to load/save the last used view
 * @param colorCellByScheduleTimeType Determine if the color used for the background grid cell must
 * be set based on the scheduleTimeType. Valid only for grid views
 */
data class AdapterSwitcherConfig(
    val prefNamePrefix: String,
    val colorCellByScheduleTimeType: Boolean
)
