# Preserve the value defined inside the service's metadata
-keep public class com.ternaryop.photoshelf.imagepicker.OnPublishAddBirthdate

# serialize/deserialize to/from sqlite
-keep class com.ternaryop.tumblr.TumblrPost

# Used inside FragmentContainerView
# https://issuetracker.google.com/issues/142601969
-keepnames class com.ternaryop.photoshelf.birthday.publisher.fragment.BirthdayPublisherFragment

# https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md
# Keep data classes created by Retrofit otherwise they will return null

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
  }

-keep,allowobfuscation @interface com.google.gson.annotations.SerializedName

-keepclassmembers class com.ternaryop.photoshelf.api.** {
  !transient <fields>;
}

-keepclassmembers class com.ternaryop.photoshelf.domselector.* {
  !transient <fields>;
}

-keepclassmembers class com.ternaryop.feedly.* {
  !transient <fields>;
}

###
# Glide
# http://bumptech.github.io/glide/doc/download-setup.html#proguard
###

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}