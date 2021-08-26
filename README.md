# PhotoShelf

[![Build Status](https://travis-ci.org/dafi/phototumblrshare.png)](https://travis-ci.org/dafi/phototumblrshare)

Share photo on Tumblr


# Browse app files on device

    run-as com.ternaryop.photoshelf.debug
    cd /data/data/com.ternaryop.photoshelf.debug

# Publish

    ./gradlew bundleRelease

Do not increment version code

    ./gradlew bundleRelease -P versionCode.increment=false
