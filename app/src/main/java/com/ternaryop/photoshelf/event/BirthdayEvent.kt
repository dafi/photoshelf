package com.ternaryop.photoshelf.event

import com.ternaryop.photoshelf.api.birthday.BirthdayManager

/**
 * Created by dave on 28/10/17.
 * Event posted in response to a birthday birthdayList request
 */

data class BirthdayEvent(val birthdayResult: BirthdayManager.BirthdayResult? = null)
