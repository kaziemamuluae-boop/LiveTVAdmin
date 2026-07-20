# Keep Room database and DAO classes
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.local.** { *; }
-keep class com.example.data.model.** { *; }

# Keep remote API and Moshi models
-keep class com.example.data.remote.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter
-keep @com.squareup.moshi.JsonClass class * { *; }

# Keep Retrofit and OkHttp classes/interfaces
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault

