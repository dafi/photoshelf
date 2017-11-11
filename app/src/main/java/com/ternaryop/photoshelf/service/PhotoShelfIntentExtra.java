package com.ternaryop.photoshelf.service;

/**
 * Created by dave on 28/10/17.
 * Intent extra parameters common to application
 */

interface PhotoShelfIntentExtra {
    // Extra arguments
    String EXTRA_URI = "uri";
    String EXTRA_BLOG_NAME = "blogName";
    String EXTRA_POST_TITLE = "postTitle";
    String EXTRA_POST_TAGS = "postTags";
    String EXTRA_TYPE = "type";
    String EXTRA_ACTION = "action";

    String EXTRA_BIRTHDAY_DATE = "birthDate";
    String EXTRA_LIST1 = "list1";
    String EXTRA_BOOLEAN1 = "boolean1";

    // Action
    String ACTION_PUBLISH_DRAFT = "draft";
    String ACTION_PUBLISH_PUBLISH = "publish";
    String ACTION_FETCH_COUNTER = "fetchCounter";
    String ACTION_BIRTHDAY_LIST_BY_DATE = "birthdayListByDate";
    String ACTION_BIRTHDAY_PUBLISH = "birthdayPublish";
    String ACTION_CHANGE_WALLPAPER = "changeWallpaper";
}
