package com.ternaryop.photoshelf.event;

import java.util.List;

import android.util.Pair;

import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.tumblr.TumblrPhotoPost;

/**
 * Created by dave on 28/10/17.
 * Event posted in response to a birthday birthdayList request
 */

public class BirthdayEvent {

    private List<Pair<Birthday, TumblrPhotoPost>> birthdayList;

    public BirthdayEvent() {
    }

    public BirthdayEvent(List<Pair<Birthday, TumblrPhotoPost>> birthdayList) {
        this.birthdayList = birthdayList;
    }

    public List<Pair<Birthday, TumblrPhotoPost>> getBirthdayList() {
        return birthdayList;
    }
}
