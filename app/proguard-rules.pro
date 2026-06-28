-keepattributes SourceFile,LineNumberTable
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class com.google.dagger.** { *; }
-keep class androidx.room.** { *; }
-keep class com.amit.browser.data.local.entities.** { *; }
-keepclassmembers class * extends android.webkit.WebChromeClient { public *; }
-keepclassmembers class * extends android.webkit.WebViewClient { public *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
