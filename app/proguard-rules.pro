# Google API Client
-keep class com.google.api.** { *; }
-keep class com.google.http.** { *; }
-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**
-dontwarn org.apache.http.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Glance
-keep class androidx.glance.** { *; }
