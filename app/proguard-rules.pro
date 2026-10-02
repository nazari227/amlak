# IranAmlak Android production shrinker rules.
# Retrofit/OkHttp/Room/Moshi ship consumer rules; keep API DTO names stable because
# Moshi generated adapter discovery derives adapter names from model class names.
-keep @com.squareup.moshi.JsonClass class com.example.data.model.** { *; }
-keepclasseswithmembers,includedescriptorclasses class * {
    @retrofit2.http.* <methods>;
}

# Preserve useful line numbers for crash reports while hiding source filenames.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
