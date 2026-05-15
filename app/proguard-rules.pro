# keep resources used by NanoHTTPD and MediaPlayer
-keep class fi.iki.elonen.** { *; }
-keepclassmembers class * {
    public void *(android.view.View);
}
