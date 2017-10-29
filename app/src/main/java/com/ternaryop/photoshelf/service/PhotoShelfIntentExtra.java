package com.ternaryop.photoshelf.service;

/**
 * Created by dave on 28/10/17.
 * Intent extra parameters common to application
 */

interface PhotoShelfIntentExtra {
    // Extra arguments
    String URL = "url";
    String BLOG_NAME = "blogName";
    String POST_TITLE = "postTitle";
    String POST_TAGS = "postTags";
    String TYPE = "type";

    // Action
    String ACTION = "action";
    String PUBLISH_ACTION_DRAFT = "draft";
    String PUBLISH_ACTION_PUBLISH = "publish";
    String FETCH_COUNTER_ACTION = "fetchCounter";
    String BIRTHDAY_LIST_BY_DATE_ACTION = "birthdayListByDate";
    String BIRTHDAY_PUBLISH_ACTION = "birthdayPublish";

    String BIRTHDAY_DATE = "birthDate";
    String LIST1 = "list1";
    String BOOLEAN1 = "boolean1";
}
