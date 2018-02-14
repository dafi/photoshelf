package com.ternaryop.photoshelf.event

import android.util.Pair

import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.tumblr.TumblrPhotoPost

/**
 * Created by dave on 28/10/17.
 * Event posted in response to a birthday birthdayList request
 */

data class BirthdayEvent(val birthdayList: List<Pair<Birthday, TumblrPhotoPost>>)
