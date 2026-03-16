# Room
-keepclassmembers class it.fonsolo.muzeiroma.AppDatabase {
    it.fonsolo.muzeiroma.ArtworkDao artworkDao();
    it.fonsolo.muzeiroma.LogDao logDao();
}
-keep class it.fonsolo.muzeiroma.ArtworkEntity { *; }
-keep class it.fonsolo.muzeiroma.LogEntity { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker

# Muzei API
-keep class com.google.android.apps.muzei.** { *; }
-keep class it.fonsolo.muzeiroma.RomaArtProvider { *; }
-keep class it.fonsolo.muzeiroma.UpdateWorker { *; }
