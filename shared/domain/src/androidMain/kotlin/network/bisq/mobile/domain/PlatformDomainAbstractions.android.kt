@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.Properties

actual fun formatDateTime(dateTime: LocalDateTime): String {
    val timeZone = TimeZone.currentSystemDefault()
    val instant = dateTime.toInstant(TimeZone.of(timeZone.id)) // Convert to Instant
    val date = Date(instant.toEpochMilliseconds()) // Convert to Java Date
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(date)
}

actual fun encodeURIParam(param: String): String {
    return URLEncoder.encode(param, StandardCharsets.UTF_8.toString())
}

actual fun getDeviceLanguageCode(): String {
    return Locale.getDefault().language
}

actual fun setupUncaughtExceptionHandler(onCrash: (Throwable) -> Unit) {
    val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        println("Uncaught exception on thread: ${thread.name}")
        throwable.printStackTrace()
        try {
            // Call the error handler immediately on the current thread
            onCrash(throwable)
        } catch (e: Exception) {
            println("Error in exception handler: ${e.message}")
            e.printStackTrace()
        }
        // For non-main thread exceptions or if recovery failed, call original handler
        originalHandler?.uncaughtException(thread, throwable)
    }
}

class AndroidUrlLauncher(private val context: Context) : UrlLauncher {
    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

class AndroidPlatformInfo : PlatformInfo {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val type = PlatformType.ANDROID
}

actual fun getPlatformInfo(): PlatformInfo = AndroidPlatformInfo()


actual fun loadProperties(fileName: String): Map<String, String> {
    val properties = Properties()
    val classLoader = Thread.currentThread().contextClassLoader
    val resource = classLoader?.getResourceAsStream(fileName)
        ?: throw IllegalArgumentException("Resource not found: $fileName")
    // Read .properties using UTF-8 to support non-ASCII characters consistently
    resource.reader(Charsets.UTF_8).use { reader ->
        properties.load(reader)
    }

    return properties.entries.associate { it.key.toString() to it.value.toString() }
}

@Serializable(with = PlatformImageSerializer::class)
actual class PlatformImage(val bitmap: ImageBitmap) {
    actual companion object {
        actual fun deserialize(data: ByteArray): PlatformImage {
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: throw IllegalArgumentException("Failed to decode image data")
            return PlatformImage(bitmap.asImageBitmap())
        }
    }

    actual fun serialize(): ByteArray {
        val androidBitmap = bitmap.asAndroidBitmap()
        val stream = ByteArrayOutputStream()
        androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}

actual fun createEmptyImage(): PlatformImage {
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
    return PlatformImage(bitmap.asImageBitmap())
}

actual val decimalFormatter: DecimalFormatter = object : DecimalFormatter {
    private val formatters: MutableMap<Pair<Int, Locale>, DecimalFormat> = mutableMapOf()
    override fun format(value: Double, precision: Int): String {
        val locale = Locale.getDefault()
        val key = precision to locale
        val formatter = formatters.getOrPut(key) {
            val format = DecimalFormat(generatePattern(precision), DecimalFormatSymbols(locale))
            format.isGroupingUsed = true
            format
        }
        return formatter.format(value)
    }

    private fun generatePattern(precision: Int): String {
        return if (precision > 0) {
            buildString {
                append("#,##0.")
                repeat(precision) { append("0") }
            }
        } else {
            "#,##0"
        }
    }
}

actual fun setDefaultLocale(language: String) {
    // Use Locale.forLanguageTag to support BCP‑47 (e.g., "en-US").
    val locale = runCatching { Locale.forLanguageTag(language) }.getOrElse { Locale(language) }
    Locale.setDefault(locale)
}

actual fun getDecimalSeparator(): Char {
    return DecimalFormatSymbols(Locale.getDefault()).decimalSeparator
}

actual fun getGroupingSeparator(): Char {
    return DecimalFormatSymbols(Locale.getDefault()).groupingSeparator
}

actual fun String.toDoubleOrNullLocaleAware(): Double? {
    return try {
        val javaLocale = Locale.getDefault()
        NumberFormat.getInstance(javaLocale).parse(this)?.toDouble()
    } catch (e: Exception) {
        null
    }
}

actual fun getLocaleCurrencyName(currencyCode: String): String {
    val javaLocale = Locale.getDefault()
    return runCatching {
        Currency.getInstance(currencyCode).getDisplayName(javaLocale)
    }.getOrElse {
        // Fallback gracefully when currency code is not recognized by the platform
        currencyCode
    }
}

actual fun Scope.getStorageDir(): String {
    return androidContext().filesDir.absolutePath
}
