# PhotoShelf

[![Build Status](https://travis-ci.org/dafi/phototumblrshare.png)](https://travis-ci.org/dafi/phototumblrshare)

Share photo on Tumblr


# Browse app files on device

    run-as com.ternaryop.photoshelf.debug
    cd /data/data/com.ternaryop.photoshelf.debug

# adb shell useful commands

### List activities

    adb shell dumpsys activity | grep photoshelf

### Run activity for debug

    adb shell am start -n com.ternaryop.photoshelf.debug/com.ternaryop.photoshelf.imagepicker.activity.ImagePickerActivity                   

# Publish

    ./gradlew bundleRelease

Do not increment version code

    ./gradlew bundleRelease -P versionCode.increment=false

# Common tasks

Compile with all warnings

    ./gradlew build -Dorg.gradle.warning.mode=all
    
Run lint before release

    ./gradlew clean check bundleRelease

# Generate dependencies graph

    gradlegraphviz all_modules all.png

where `gradlegraphviz` is the shell function

    gradlegraphviz() {
    if [ $# -lt 1 ]
    then echo "Syntax <graphviz path> <png path>\nExample: gradlegraphviz all_modules all.png"
    else ./gradlew generateModulesGraphvizText -Pmodules.graph.output.gv="$1" && dot -Tpng "$1" -O && open "$1.png"
    fi
    }

# Run detekt

    ./gradlew detekt

only on specific module

    ./gradlew :home:detekt

# Run dependency checker

    ./gradlew dependencyUpdates
