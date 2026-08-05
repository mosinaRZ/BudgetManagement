# ============================================================
# General
# ============================================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# ============================================================
# Kotlin
# ============================================================
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# ============================================================
# Room
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ============================================================
# SQLCipher
# ============================================================
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# ============================================================
# Security Crypto / Google Tink
# ============================================================
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

-dontwarn com.google.crypto.tink.**

# R8 generated missing rules
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.concurrent.GuardedBy

# ============================================================
# WorkManager
# ============================================================
-keep class androidx.work.** { *; }

# ============================================================
# Glance
# ============================================================
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# ============================================================
# Lottie
# ============================================================
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ============================================================
# Biometric
# ============================================================
-keep class androidx.biometric.** { *; }

# ============================================================
# FastExcel
# ============================================================
-keep class org.dhatim.fastexcel.** { *; }
-dontwarn org.dhatim.fastexcel.**

# ============================================================
# Kotlin Serialization
# ============================================================
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ============================================================
# Parcelable
# ============================================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ============================================================
# Serializable
# ============================================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================
# Your Models
# ============================================================
-keep class ir.hamedan.budgetmanagement.data.local.models.** { *; }

# ============================================================
# Navigation Routes
# در بیلد ریلیز، R8 اسم کلاس‌ها رو کوتاه می‌کنه و چون تشخیص
# تب انتخاب‌شده در بات‌بار بر اساس ::class.simpleName انجام می‌شه،
# باید این کلاس‌ها از ماینیفای/آبفسکیت‌شدن مستثنی بشن وگرنه
# isSelected همیشه false برمی‌گرده.
# ============================================================
-keep class ir.hamedan.budgetmanagement.ui.navigation.** { *; }
-keepnames class ir.hamedan.budgetmanagement.ui.navigation.** { *; }

# ============================================================
# Nullable
# ============================================================
-dontwarn javax.annotation.Nullable