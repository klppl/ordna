# Google Tasks API – only keep the packages we actually use
-keep class com.google.api.services.tasks.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.http.client.** { *; }
-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**
-dontwarn org.apache.http.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Glance
-keep class androidx.glance.** { *; }
