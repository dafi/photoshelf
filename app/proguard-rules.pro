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

-dontwarn jakarta.xml.bind.DatatypeConverter
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn javax.xml.bind.DatatypeConverter

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation