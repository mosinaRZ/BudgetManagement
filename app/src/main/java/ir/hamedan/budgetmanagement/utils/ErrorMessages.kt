package ir.hamedan.budgetmanagement.utils

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import ir.hamedan.budgetmanagement.data.network.ApiException
import kotlinx.coroutines.CancellationException
import org.json.JSONException
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.GeneralSecurityException
import javax.net.ssl.SSLException

/**
 * Single source of truth for turning ANY failure into a short, localized (fa/en) sentence that is
 * safe to show to the user.
 *
 * Rules every caller gets for free:
 *  - raw `Throwable.message` text is never shown (it is technical, and often in the wrong language);
 *  - coroutine cancellation is never reported as an error (see [rethrowIfCancelled]);
 *  - the same failure produces the same wording on every screen.
 */
object ErrorMessages {

    fun of(error: Throwable?, isPersian: Boolean): String = when (error) {
        null -> generic(isPersian)
        is ApiException -> error.userMessage(isPersian)
        is SocketTimeoutException, is UnknownHostException, is ConnectException, is SSLException ->
            network(isPersian)
        is SQLiteFullException, is SQLiteDiskIOException -> storageFull(isPersian)
        is SQLiteConstraintException -> duplicate(isPersian)
        is SQLiteException -> database(isPersian)
        is JSONException -> badServerResponse(isPersian)
        is SecurityException -> permissionDenied(isPersian)
        is GeneralSecurityException -> cryptoFailure(isPersian)
        is FileNotFoundException -> fileMissing(isPersian)
        is IOException -> if (isPersian) "خواندن یا نوشتن اطلاعات ناموفق بود. دوباره تلاش کنید."
        else "Reading or writing data failed. Please try again."
        else -> generic(isPersian)
    }

    fun generic(isPersian: Boolean): String =
        if (isPersian) "مشکلی پیش آمد. لطفاً دوباره تلاش کنید." else "Something went wrong. Please try again."

    fun network(isPersian: Boolean): String =
        if (isPersian) "اتصال به سرور برقرار نشد. اینترنت و اتصال سرور را بررسی کنید."
        else "Could not connect to the server. Check your internet connection."

    fun storageFull(isPersian: Boolean): String =
        if (isPersian) "حافظه‌ی دستگاه پر است؛ فضا آزاد کنید و دوباره تلاش کنید."
        else "Your device storage is full. Free up some space and try again."

    fun duplicate(isPersian: Boolean): String =
        if (isPersian) "این مورد قبلاً ثبت شده است." else "This item already exists."

    fun database(isPersian: Boolean): String =
        if (isPersian) "ذخیره یا خواندن اطلاعات ناموفق بود. دوباره تلاش کنید."
        else "Saving or loading your data failed. Please try again."

    fun badServerResponse(isPersian: Boolean): String =
        if (isPersian) "پاسخ سرور قابل‌خواندن نبود. کمی بعد دوباره تلاش کنید."
        else "The server sent an unreadable response. Please try again shortly."

    fun permissionDenied(isPersian: Boolean): String =
        if (isPersian) "دسترسی لازم داده نشده است. مجوز را در تنظیمات دستگاه بررسی کنید."
        else "Permission is missing. Check the app permissions in your device settings."

    fun cryptoFailure(isPersian: Boolean): String =
        if (isPersian) "باز کردن اطلاعات امن ناموفق بود. دوباره وارد شوید."
        else "Unlocking your secure data failed. Please sign in again."

    fun fileMissing(isPersian: Boolean): String =
        if (isPersian) "فایل موردنظر پیدا نشد." else "The file could not be found."
}

/** Coroutines use cancellation for control flow: it must keep propagating, never become a message. */
fun Throwable.rethrowIfCancelled() {
    if (this is CancellationException) throw this
}

/** Like [runCatching] but lets coroutine cancellation through. Use it inside suspend code. */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

/**
 * Safety net for ViewModel coroutines. Anything a `launch` block does not handle itself ends up
 * here instead of crashing the app: it is logged and reported to the screen as a localized message.
 * Cancellation never reaches a [CoroutineExceptionHandler].
 *
 * Usage: `private val errorHandler = viewModelErrorHandler(context, "MyVM") { _errorMessage.tryEmit(it) }`
 * and `viewModelScope.launch(errorHandler) { ... }` / `launch(ioDispatcher + errorHandler) { ... }`.
 */
fun viewModelErrorHandler(
    context: android.content.Context,
    tag: String,
    onMessage: (String) -> Unit
): kotlinx.coroutines.CoroutineExceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
    // android.util.Log throws on the plain JVM (unit tests); logging must never be the new crash.
    runCatching { android.util.Log.e(tag, "Unhandled error in ViewModel coroutine", throwable) }
    val isPersian = runCatching {
        ir.hamedan.budgetmanagement.platform.locale.LocaleHelper.getLanguage(context) == "fa"
    }.getOrDefault(true)
    runCatching { onMessage(ErrorMessages.of(throwable, isPersian)) }
}