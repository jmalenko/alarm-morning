# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in Android SDK in path .../sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Requred by Jollyday library to prevent warnings (as mentioned on Jollyday website)
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Requred by Jollyday library for APK minify
-keep class de.galgtonold.jollydayandroid.impl.XMLManager.** { *; }
-keep interface de.galgtonold.jollydayandroid.impl.XMLManager.** { *; }

-keep class de.galgtonold.jollydayandroid.impl.** { *; }
-keep interface de.galgtonold.jollydayandroid.impl.** { *; }

-keep class de.galgtonold.jollydayandroid.** { *; }
-keep interface de.galgtonold.jollydayandroid.** { *; }


-keep class de.galgtonold.jollydayandroid.util.XMLUtil.** { *; }
-keep interface de.galgtonold.jollydayandroid.util.XMLUtil.** { *; }

-keep class de.galgtonold.jollydayandroid.util.** { *; }
-keep interface de.galgtonold.jollydayandroid.util.** { *; }


-keep class javax.xml.stream.** { *; }
-keep interface javax.xml.stream.** { *; }

-keep class javax.xml.stream.events.** { *; }
-keep interface javax.xml.stream.events.** { *; }


-keep class org.simpleframework.xml.** { *; }
-keep interface org.simpleframework.xml.** { *; }

-keep class org.simpleframework.xml.core.** { *; }
-keep interface org.simpleframework.xml.core.** { *; }

-keep class org.simpleframework.xml.stream.** { *; }
-keep interface org.simpleframework.xml.stream.** { *; }


-keep class cz.jaro.alarmmorning.** { *; }

# Remove log messages
-assumenosideeffects class android.util.Log {
  public static *** v(...);
  public static *** d(...);
}
#  public static *** i(...);
#  public static *** w(...);
#  public static *** e(...);
