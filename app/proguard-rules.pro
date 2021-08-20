# Preserve the value defined inside the service's metadata
-keep public class com.ternaryop.photoshelf.imagepicker.OnPublishAddBirthdate

# serialize/deserialize to/from sqlite
-keep class com.ternaryop.tumblr.TumblrPost

# Prevent InvalidClassException on serialVersionUID when TumblrPost is deserialized
# https://www.guardsquare.com/manual/configuration/examples#serializable
-keepnames class * implements java.io.Serializable

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class com.ternaryop.photoshelf.feedly.prefs.FeedlyPreferenceFragment
