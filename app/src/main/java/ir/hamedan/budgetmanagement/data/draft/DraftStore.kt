package ir.hamedan.budgetmanagement.data.draft

import android.content.Context
import android.util.Log
import ir.hamedan.budgetmanagement.data.security.KeystoreSecretBox
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * ذخیره‌گاه پیش‌نویس‌های ثبت‌های نیمه‌تمام (تراکنش، بدهی/طلب، دسته‌بندی، سقف بودجه و قلک).
 *
 * طراحی:
 *  - هر نوع حداکثر یک پیش‌نویس دارد و هر پیش‌نویس یک فایل جداست؛ مبلغ‌های مالی پس از رمزنگاری
 *    AES‑256‑GCM با کلیدِ Android Keystore ([KeystoreSecretBox]) روی دیسک می‌روند، نه متن ساده.
 *  - فایل‌ها در `noBackupFilesDir` هستند؛ پس به پشتیبان‌گیری ابری و انتقال دستگاه نمی‌روند (مثل دیتابیس).
 *  - پیش‌نویس هرگز همگام‌سازی نمی‌شود و جزو دیتابیس نیست؛ فقط یک یادداشتِ محلی روی همین گوشی است.
 *  - هنگام خروج از حساب، تعویض حساب یا ورود به حالت مهمان [clearAll] صدا زده می‌شود
 *    (از `SyncLocalDataSourceImpl.clearAccountData`)، تا پیش‌نویسِ حساب قبلی به حساب بعدی نرسد.
 *  - نوشتن روی دیسک (و Keystore) روی یک رشته‌ی پس‌زمینه و به‌ترتیب انجام می‌شود تا رابط کاربری کند نشود؛
 *    وضعیت درون‌حافظه ([drafts]) همان لحظه به‌روز می‌شود.
 */
object DraftStore {
    private const val TAG = "DraftStore"
    private const val DIR_NAME = "drafts"
    private const val KEY_ALIAS = "cidna_draft_box_v1"
    private const val AAD_PREFIX = "cidna.draft.v1:"
    private const val FIELD_UPDATED_AT = "t"
    private const val FIELD_PAYLOAD = "p"

    private val box by lazy { KeystoreSecretBox(KEY_ALIAS) }
    private val io = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "draft-store").apply { isDaemon = true }
    }

    private val _drafts = MutableStateFlow<Map<DraftType, Draft>>(emptyMap())

    /** پیش‌نویس‌های فعلی؛ برای نمایش فهرست در صفحه‌ی افزودن. */
    val drafts: StateFlow<Map<DraftType, Draft>> = _drafts.asStateFlow()

    private val _ready = MutableStateFlow(false)

    /** پس از خواندن پیش‌نویس‌های ذخیره‌شده از دیسک true می‌شود؛ صفحه‌ها تا آن موقع تصمیمی نمی‌گیرند. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    @Volatile
    private var directory: File? = null

    @Volatile
    private var resumeRequest: DraftType? = null

    /** با هر [clearAll] بالا می‌رود تا نوشتن‌های نیمه‌راه از پیش‌نویسِ پاک‌شده دوباره فایل نسازند. */
    @Volatile
    private var generation = 0

    /** یک‌بار (idempotent) در شروع برنامه و در صورت نیاز از صفحه‌ها صدا زده می‌شود. */
    fun init(context: Context) {
        if (directory != null) return
        synchronized(this) {
            if (directory != null) return
            directory = File(context.applicationContext.noBackupFilesDir, DIR_NAME).apply { mkdirs() }
        }
        io.execute {
            runCatching { loadAll() }.onFailure { Log.w(TAG, "Could not load drafts", it) }
            _ready.value = true
        }
    }

    fun get(type: DraftType): Draft? = _drafts.value[type]

    /** ذخیره (یا جایگزینی) پیش‌نویسِ [type]؛ اگر محتوا عوض نشده باشد کاری نمی‌کند. */
    fun save(type: DraftType, payload: JsonObject) {
        val current = _drafts.value[type]
        if (current != null && current.payload == payload) return
        val draft = Draft(type = type, payload = payload, updatedAt = System.currentTimeMillis())
        _drafts.value = _drafts.value + (type to draft)
        val gen = generation
        io.execute { persist(draft, gen) }
    }

    /** حذف پیش‌نویسِ [type] (اگر باشد). */
    fun discard(type: DraftType) {
        if (_drafts.value[type] == null) return
        _drafts.value = _drafts.value - type
        val gen = generation
        io.execute { if (gen == generation) fileFor(type)?.delete() }
    }

    /** حذف همه‌ی پیش‌نویس‌ها؛ هنگام خروج از حساب/تعویض حساب. */
    fun clearAll() {
        generation++
        resumeRequest = null
        _drafts.value = emptyMap()
        io.execute {
            DraftType.values().forEach { fileFor(it)?.delete() }
            directory?.listFiles()?.forEach { it.delete() }
        }
    }

    /** از صفحه‌ی فهرست پیش‌نویس‌ها: صفحه‌ی ثبت بدون پرسیدن، پیش‌نویس را بازیابی کند. */
    fun requestResume(type: DraftType) {
        resumeRequest = type
    }

    /** یک‌بار مصرف: true اگر برای [type] درخواست بازیابی مستقیم بوده است. */
    fun consumeResumeRequest(type: DraftType): Boolean {
        val requested = resumeRequest
        if (requested == null) return false
        resumeRequest = null
        return requested == type
    }

    // ───────────────────────── دیسک ─────────────────────────

    private fun fileFor(type: DraftType): File? = directory?.let { File(it, "${type.key}.bin") }

    private fun aad(type: DraftType): ByteArray = (AAD_PREFIX + type.key).toByteArray(Charsets.UTF_8)

    private fun persist(draft: Draft, gen: Int) {
        if (gen != generation) return
        val target = fileFor(draft.type) ?: return
        runCatching {
            val envelope = buildJsonObject {
                put(FIELD_UPDATED_AT, draft.updatedAt)
                put(FIELD_PAYLOAD, draft.payload)
            }
            val encrypted = box.encrypt(envelope.toString().toByteArray(Charsets.UTF_8), aad(draft.type))
            val temp = File(target.parentFile, "${draft.type.key}.tmp")
            temp.writeBytes(encrypted)
            if (gen != generation) {
                temp.delete()
                return
            }
            if (!temp.renameTo(target)) {
                target.delete()
                if (!temp.renameTo(target)) temp.delete()
            }
        }.onFailure { Log.w(TAG, "Could not persist draft ${draft.type.key}", it) }
    }

    private fun loadAll() {
        val loaded = HashMap<DraftType, Draft>()
        DraftType.values().forEach { type ->
            val file = fileFor(type)?.takeIf { it.isFile } ?: return@forEach
            try {
                val plain = box.decrypt(file.readBytes(), aad(type)).toString(Charsets.UTF_8)
                val envelope = Json.parseToJsonElement(plain).jsonObject
                val payload = envelope[FIELD_PAYLOAD]?.jsonObject ?: error("Missing payload")
                loaded[type] = Draft(type, payload, envelope.long(FIELD_UPDATED_AT, System.currentTimeMillis()))
            } catch (e: KeystoreSecretBox.PermanentFailure) {
                // کلید یا فایل دیگر هرگز قابل‌خواندن نیست؛ نگه‌داشتنش فایده‌ای ندارد.
                file.delete()
            } catch (e: IllegalArgumentException) {
                file.delete() // JSON خراب (SerializationException زیرمجموعه‌ی آن است)
            } catch (e: IllegalStateException) {
                file.delete()
            } catch (e: Exception) {
                // مشکل موقتِ Keystore (بعد از بوت و …): فایل را نگه می‌داریم، فقط این بار خوانده نمی‌شود.
                Log.w(TAG, "Could not read draft ${type.key}", e)
            }
        }
        // ذخیره‌هایی که در فاصله‌ی خواندن انجام شده‌اند (نسخه‌ی تازه‌تر) روی خوانده‌ها برتری دارند.
        _drafts.value = loaded + _drafts.value
    }
}
