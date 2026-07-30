# ======================
# Keep general Android / Kotlin
# ======================
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# ======================
# Kotlin
# ======================
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# ======================
# Room
# ======================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ======================
# Lottie
# ======================
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ======================
# Biometric
# ======================
-keep class androidx.biometric.** { *; }

# ======================
# Glance (App Widget)
# ======================
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# ======================
# FastExcel
# ======================
-dontwarn org.dhatim.fastexcel.**
-keep class org.dhatim.fastexcel.** { *; }

# ======================
# Keep data classes / models (Room entities etc.)
# ======================
-keep class ir.hamedan.budgetmanagement.data.local.models.** { *; }

# ======================
# Keep Parcelable / Serializable if needed
# ======================
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-dontwarn javax.annotation.Nullable